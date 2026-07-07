package server.sassedo.listing.search.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import server.sassedo.listing.common.ListingFilter;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.search.data.dto.ApartmentSearch;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the public browse query for apartment searches (tenant "looking for" ads) from an optional
 * {@link ListingFilter}. Always restricts to ACTIVE searches. Price is expressed as a budget range,
 * so a min/max filter is treated as an overlap against the search's budgetMin/budgetMax. Apartment
 * searches have no bedrooms/bathrooms/amenities, so those filters are ignored.
 */
public final class ApartmentSearchSpecifications {

    private ApartmentSearchSpecifications() {
    }

    public static Specification<ApartmentSearch> browse(ListingFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ListingStatus.ACTIVE));

            if (f == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (f.getCityId() != null) {
                predicates.add(cb.equal(root.get("city").get("id"), f.getCityId()));
            }
            if (f.getPropertyType() != null) {
                predicates.add(cb.equal(root.get("propertyType"), f.getPropertyType()));
            }
            if (f.getNeighborhood() != null && !f.getNeighborhood().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("neighborhood")),
                        "%" + f.getNeighborhood().toLowerCase() + "%"));
            }
            // Budget overlap: the search accepts prices in [budgetMin, budgetMax]; a filtered
            // [minPrice, maxPrice] window matches when the ranges intersect.
            if (f.getMinPrice() != null) {
                predicates.add(cb.or(
                        cb.isNull(root.get("budgetMax")),
                        cb.greaterThanOrEqualTo(root.get("budgetMax"), f.getMinPrice())));
            }
            if (f.getMaxPrice() != null) {
                predicates.add(cb.or(
                        cb.isNull(root.get("budgetMin")),
                        cb.lessThanOrEqualTo(root.get("budgetMin"), f.getMaxPrice())));
            }
            if (f.getAvailableBy() != null) {
                Predicate asap = cb.isTrue(root.get("availableAsap"));
                Predicate byDate = cb.and(
                        cb.isNotNull(root.get("availableFrom")),
                        cb.lessThanOrEqualTo(root.get("availableFrom"), f.getAvailableBy()));
                predicates.add(cb.or(asap, byDate));
            }
            if (f.getLeaseTerms() != null && !f.getLeaseTerms().isEmpty()) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<ApartmentSearch> subRoot = sub.from(ApartmentSearch.class);
                Join<Object, Object> terms = subRoot.join("leaseTerms");
                sub.select(subRoot.get("id"));
                sub.where(cb.and(cb.equal(subRoot.get("id"), root.get("id")), terms.in(f.getLeaseTerms())));
                predicates.add(cb.exists(sub));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

package server.sassedo.listing.rental.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import server.sassedo.listing.common.ListingFilter;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.PropertyAmenity;
import server.sassedo.listing.rental.data.dto.RentalListing;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the public browse query for rental listings from an optional {@link ListingFilter}. Always
 * restricts to ACTIVE listings; collection filters use correlated EXISTS subqueries so pagination
 * stays correct and the entity's EAGER element collections are not multiplied.
 */
public final class RentalListingSpecifications {

    private RentalListingSpecifications() {
    }

    public static Specification<RentalListing> browse(ListingFilter f) {
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
            if (f.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rent"), f.getMinPrice()));
            }
            if (f.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rent"), f.getMaxPrice()));
            }
            if (f.getAvailableBy() != null) {
                Predicate asap = cb.isTrue(root.get("availableAsap"));
                Predicate byDate = cb.and(
                        cb.isNotNull(root.get("availableFrom")),
                        cb.lessThanOrEqualTo(root.get("availableFrom"), f.getAvailableBy()));
                predicates.add(cb.or(asap, byDate));
            }
            if (f.getMinBedrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), f.getMinBedrooms()));
            }
            if (f.getMinBathrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bathrooms"), f.getMinBathrooms()));
            }
            if (f.getLeaseTerms() != null && !f.getLeaseTerms().isEmpty()) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<RentalListing> subRoot = sub.from(RentalListing.class);
                Join<Object, Object> terms = subRoot.join("leaseTerms");
                sub.select(subRoot.get("id"));
                sub.where(cb.and(cb.equal(subRoot.get("id"), root.get("id")), terms.in(f.getLeaseTerms())));
                predicates.add(cb.exists(sub));
            }
            if (f.getAmenities() != null && !f.getAmenities().isEmpty()) {
                for (String raw : f.getAmenities()) {
                    PropertyAmenity amenity = parse(raw);
                    if (amenity == null) {
                        continue;
                    }
                    Subquery<Long> sub = query.subquery(Long.class);
                    Root<RentalListing> subRoot = sub.from(RentalListing.class);
                    Join<Object, Object> am = subRoot.join("propertyAmenities");
                    sub.select(subRoot.get("id"));
                    sub.where(cb.and(cb.equal(subRoot.get("id"), root.get("id")), cb.equal(am, amenity)));
                    predicates.add(cb.exists(sub));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static PropertyAmenity parse(String raw) {
        try {
            return PropertyAmenity.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}

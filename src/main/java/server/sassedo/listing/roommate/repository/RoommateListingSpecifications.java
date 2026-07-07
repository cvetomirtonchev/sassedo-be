package server.sassedo.listing.roommate.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import server.sassedo.listing.common.ListingFilter;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.RoomAmenity;
import server.sassedo.listing.roommate.data.dto.RoommateListing;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the public browse query for roommate listings from an optional {@link ListingFilter}. Always
 * restricts to ACTIVE listings. Roommate listings have no lease-term field, so that filter is ignored;
 * amenities map to the room-amenity element collection.
 */
public final class RoommateListingSpecifications {

    private RoommateListingSpecifications() {
    }

    public static Specification<RoommateListing> browse(ListingFilter f) {
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
            if (f.getAmenities() != null && !f.getAmenities().isEmpty()) {
                for (String raw : f.getAmenities()) {
                    RoomAmenity amenity = parse(raw);
                    if (amenity == null) {
                        continue;
                    }
                    Subquery<Long> sub = query.subquery(Long.class);
                    Root<RoommateListing> subRoot = sub.from(RoommateListing.class);
                    Join<Object, Object> am = subRoot.join("roomAmenities");
                    sub.select(subRoot.get("id"));
                    sub.where(cb.and(cb.equal(subRoot.get("id"), root.get("id")), cb.equal(am, amenity)));
                    predicates.add(cb.exists(sub));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static RoomAmenity parse(String raw) {
        try {
            return RoomAmenity.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}

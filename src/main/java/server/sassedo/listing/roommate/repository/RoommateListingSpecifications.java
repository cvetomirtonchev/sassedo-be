package server.sassedo.listing.roommate.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import server.sassedo.listing.common.ListingFilter;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.PetPolicy;
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
            // Filter by has-property mode. A null column value predates this feature and is treated
            // as "has property" (true).
            boolean withoutProperty = Boolean.FALSE.equals(f.getHasProperty());
            if (f.getHasProperty() != null) {
                if (withoutProperty) {
                    predicates.add(cb.equal(root.get("hasProperty"), Boolean.FALSE));
                } else {
                    predicates.add(cb.or(
                            cb.equal(root.get("hasProperty"), Boolean.TRUE),
                            cb.isNull(root.get("hasProperty"))));
                }
            }
            if (f.getPropertyType() != null) {
                predicates.add(cb.equal(root.get("propertyType"), f.getPropertyType()));
            }
            if (f.getNeighborhood() != null && !f.getNeighborhood().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("neighborhood")),
                        "%" + f.getNeighborhood().toLowerCase() + "%"));
            }
            // For listers without a property the price filter targets their budget; otherwise rent.
            String priceField = withoutProperty ? "budget" : "rent";
            if (f.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(priceField), f.getMinPrice()));
            }
            if (f.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(priceField), f.getMaxPrice()));
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
            // Age-range overlap against the listing's accepted age window. A missing bound on the
            // listing side means "open", so it never excludes a listing.
            if (f.getAgeMin() != null) {
                predicates.add(cb.or(
                        cb.isNull(root.get("ageMax")),
                        cb.greaterThanOrEqualTo(root.get("ageMax"), f.getAgeMin())));
            }
            if (f.getAgeMax() != null) {
                predicates.add(cb.or(
                        cb.isNull(root.get("ageMin")),
                        cb.lessThanOrEqualTo(root.get("ageMin"), f.getAgeMax())));
            }
            // A listing with no gender preference (null) accepts anyone, so it always matches.
            if (f.getPreferredSex() != null) {
                predicates.add(cb.or(
                        cb.isNull(root.get("preferredSex")),
                        cb.equal(root.get("preferredSex"), f.getPreferredSex())));
            }
            // Pets: with-property listings expose petsAllowed; without-property listers use petPolicy.
            if (f.getPetsAllowed() != null) {
                if (withoutProperty) {
                    if (Boolean.TRUE.equals(f.getPetsAllowed())) {
                        predicates.add(cb.and(
                                cb.isNotNull(root.get("petPolicy")),
                                cb.notEqual(root.get("petPolicy"), PetPolicy.NOT_ALLOWED)));
                    } else {
                        predicates.add(cb.equal(root.get("petPolicy"), PetPolicy.NOT_ALLOWED));
                    }
                } else {
                    predicates.add(cb.equal(root.get("petsAllowed"), f.getPetsAllowed()));
                }
            }
            if (f.getSmokingPreference() != null) {
                predicates.add(cb.equal(root.get("smokingPreference"), f.getSmokingPreference()));
            }
            if (f.getEmploymentStatus() != null) {
                predicates.add(cb.equal(root.get("employmentStatus"), f.getEmploymentStatus()));
            }
            if (f.getSpokenLanguages() != null && !f.getSpokenLanguages().isEmpty()) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<RoommateListing> subRoot = sub.from(RoommateListing.class);
                Join<Object, Object> langs = subRoot.join("spokenLanguages");
                sub.select(subRoot.get("id"));
                sub.where(cb.and(cb.equal(subRoot.get("id"), root.get("id")), langs.in(f.getSpokenLanguages())));
                predicates.add(cb.exists(sub));
            }
            if (f.getRoomArrangement() != null) {
                predicates.add(cb.equal(root.get("roomArrangement"), f.getRoomArrangement()));
            }
            if (f.getHasChildren() != null) {
                predicates.add(cb.equal(root.get("hasChildren"), f.getHasChildren()));
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

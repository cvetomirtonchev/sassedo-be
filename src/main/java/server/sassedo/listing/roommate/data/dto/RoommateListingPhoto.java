package server.sassedo.listing.roommate.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "roommate_listing_photos")
public class RoommateListingPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB", nullable = false)
    private byte[] data;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "is_main", nullable = false)
    private boolean main = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private RoommateListing listing;
}

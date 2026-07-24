package server.sassedo.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.sassedo.common.data.dto.Testimonial;

import java.util.List;

public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {

    List<Testimonial> findAllByOrderByIdDesc();

    @Query(value = "SELECT * FROM testimonials WHERE enabled = true ORDER BY RAND() LIMIT 3", nativeQuery = true)
    List<Testimonial> findRandomEnabled();
}

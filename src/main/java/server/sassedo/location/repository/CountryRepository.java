package server.sassedo.location.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.sassedo.location.data.dto.Country;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    boolean existsByNameEnIgnoreCase(String nameEn);
}

package server.sassedo.promotion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.sassedo.promotion.data.dto.Purchase;

import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    Page<Purchase> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

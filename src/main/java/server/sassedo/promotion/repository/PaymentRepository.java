package server.sassedo.promotion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.sassedo.promotion.data.dto.Payment;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPurchaseIdOrderByCreatedAtDesc(Long purchaseId);

    List<Payment> findByPurchaseIdInOrderByCreatedAtDesc(List<Long> purchaseIds);

    Payment findFirstByProviderRefOrderByCreatedAtDesc(String providerRef);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

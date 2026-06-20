package com.smartbasket.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubstitutionRepository extends JpaRepository<Substitution, String> {

    /** Fallback chain for a preferred product, best (lowest priority number) first. */
    List<Substitution> findByUserIdAndPreferredProductOrderByPriorityAsc(String userId, String preferredProduct);

    boolean existsByUserIdAndPreferredProductAndFallbackProduct(
            String userId, String preferredProduct, String fallbackProduct);
}

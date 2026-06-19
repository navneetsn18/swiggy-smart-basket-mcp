package com.smartbasket.substitution;

import com.smartbasket.domain.Substitution;
import com.smartbasket.domain.SubstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class SubstitutionService {

    private final SubstitutionRepository repo;

    public SubstitutionService(SubstitutionRepository repo) {
        this.repo = repo;
    }

    /**
     * Record that {@code fallback} may stand in for {@code preferred}. Appended at the
     * end of the chain (lowest preference). Idempotent on (user, preferred, fallback).
     */
    public void learn(String userId, String preferred, String fallback) {
        requireText(userId, "userId");
        requireText(preferred, "preferred");
        requireText(fallback, "fallback");
        if (preferred.equalsIgnoreCase(fallback)) {
            throw new IllegalArgumentException("preferred and fallback must differ");
        }
        if (repo.existsByUserIdAndPreferredProductAndFallbackProduct(userId, preferred, fallback)) {
            return; // already known — never duplicate (idea.md: learn, don't invent)
        }
        int nextPriority = chain(userId, preferred).size() + 1;
        repo.save(new Substitution(userId, preferred, fallback, nextPriority));
    }

    /** Ordered fallback product names for a preferred product (best first). */
    @Transactional(readOnly = true)
    public List<String> chain(String userId, String preferred) {
        return repo.findByUserIdAndPreferredProductOrderByPriorityAsc(userId, preferred)
                .stream().map(Substitution::getFallbackProduct).toList();
    }

    private static void requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}

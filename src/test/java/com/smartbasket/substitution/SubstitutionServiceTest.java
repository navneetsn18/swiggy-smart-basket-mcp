package com.smartbasket.substitution;

import com.smartbasket.domain.Substitution;
import com.smartbasket.domain.SubstitutionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SubstitutionServiceTest {

    private final SubstitutionRepository repo = mock(SubstitutionRepository.class);
    private final SubstitutionService service = new SubstitutionService(repo);

    @Test
    void learn_appends_at_next_priority() {
        when(repo.findByUserIdAndPreferredProductOrderByPriorityAsc("u1", "Milk 1L"))
                .thenReturn(List.of(new Substitution("u1", "Milk 1L", "Milk 500ml", 1)));

        service.learn("u1", "Milk 1L", "Mother Dairy 1L");

        ArgumentCaptor<Substitution> saved = ArgumentCaptor.forClass(Substitution.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getPriority()).isEqualTo(2);
        assertThat(saved.getValue().getFallbackProduct()).isEqualTo("Mother Dairy 1L");
    }

    @Test
    void learn_is_idempotent() {
        when(repo.existsByUserIdAndPreferredProductAndFallbackProduct("u1", "Milk 1L", "Milk 500ml"))
                .thenReturn(true);
        service.learn("u1", "Milk 1L", "Milk 500ml");
        verify(repo, never()).save(any());
    }

    @Test
    void learn_rejects_self_substitution() {
        assertThatThrownBy(() -> service.learn("u1", "Milk", "Milk"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

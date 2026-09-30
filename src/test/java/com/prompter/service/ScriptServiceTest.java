package com.prompter.service;

import com.prompter.dto.ScriptRequest;
import com.prompter.dto.ScriptResponse;
import com.prompter.model.Script;
import com.prompter.repository.ScriptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScriptServiceTest {

    @Mock
    ScriptRepository repository;

    @InjectMocks
    ScriptService service;

    private Script script(long id, String title, String content) {
        Script s = new Script(title, content);
        // Simulate the persisted identity without going through JPA.
        try {
            var f = Script.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(s, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return s;
    }

    @Test
    void list_returnsAllMappedToResponses() {
        when(repository.findAllByOrderByIdDesc())
                .thenReturn(List.of(script(2, "B", "bbb"), script(1, "A", "aaa")));

        List<ScriptResponse> result = service.list();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(2);
        assertThat(result.get(1).title()).isEqualTo("A");
    }

    @Test
    void get_unknownId_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_trimsTitleAndPersists() {
        when(repository.save(any(Script.class))).thenAnswer(inv -> inv.getArgument(0));

        ScriptResponse created = service.create(new ScriptRequest("  My Script  ", "hello"));

        assertThat(created.title()).isEqualTo("My Script");
        assertThat(created.content()).isEqualTo("hello");
        verify(repository).save(any(Script.class));
    }

    @Test
    void create_blankTitle_stillPersistsValidationIsUpstream() {
        // Validation lives in the controller (@Valid); service passes through.
        when(repository.save(any(Script.class))).thenAnswer(inv -> inv.getArgument(0));

        ScriptResponse created = service.create(new ScriptRequest("x", ""));

        assertThat(created.content()).isEmpty();
    }

    @Test
    void update_unknownId_throws() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(42L, new ScriptRequest("t", "c")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void update_existing_replacesTitleAndContent() {
        Script existing = script(7L, "old", "old text");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        ScriptResponse updated = service.update(7L, new ScriptRequest("new", "new text"));

        assertThat(updated.title()).isEqualTo("new");
        assertThat(updated.content()).isEqualTo("new text");
    }

    @Test
    void delete_unknownId_throws() {
        when(repository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void delete_existing_removes() {
        when(repository.existsById(5L)).thenReturn(true);

        service.delete(5L);

        verify(repository).deleteById(5L);
    }
}

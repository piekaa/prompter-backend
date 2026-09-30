package com.prompter.service;

import com.prompter.dto.ScriptRequest;
import com.prompter.dto.ScriptResponse;
import com.prompter.model.Script;
import com.prompter.repository.ScriptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ScriptService {

    private final ScriptRepository repository;

    public ScriptService(ScriptRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ScriptResponse> list() {
        return repository.findAllByOrderByIdDesc().stream()
                .map(ScriptResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScriptResponse get(Long id) {
        return ScriptResponse.from(find(id));
    }

    @Transactional
    public ScriptResponse create(ScriptRequest request) {
        Script script = new Script(request.title().trim(), request.content());
        return ScriptResponse.from(repository.save(script));
    }

    @Transactional
    public ScriptResponse update(Long id, ScriptRequest request) {
        Script script = find(id);
        script.setTitle(request.title().trim());
        script.setContent(request.content());
        return ScriptResponse.from(repository.save(script));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Script not found: " + id);
        }
        repository.deleteById(id);
    }

    private Script find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Script not found: " + id));
    }
}

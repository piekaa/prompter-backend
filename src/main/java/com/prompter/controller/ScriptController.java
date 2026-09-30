package com.prompter.controller;

import com.prompter.dto.ScriptRequest;
import com.prompter.dto.ScriptResponse;
import com.prompter.service.ScriptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public scripts API — consumed by the Android app and the web editor.
 * Sits under /api/public/** so it requires no session.
 */
@RestController
@RequestMapping("/api/public/scripts")
public class ScriptController {

    private final ScriptService service;

    public ScriptController(ScriptService service) {
        this.service = service;
    }

    @GetMapping
    public List<ScriptResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ScriptResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<ScriptResponse> create(@Valid @RequestBody ScriptRequest request) {
        ScriptResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ScriptResponse update(@PathVariable Long id, @Valid @RequestBody ScriptRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.prompter.repository;

import com.prompter.model.Script;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScriptRepository extends JpaRepository<Script, Long> {

    List<Script> findAllByOrderByIdDesc();
}

package com.rag.kb.demo.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KbCreateDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void validName_shouldPass() {
        KbCreateDTO dto = new KbCreateDTO();
        dto.setName("产品手册");
        Set<ConstraintViolation<KbCreateDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankName_shouldFail() {
        KbCreateDTO dto = new KbCreateDTO();
        dto.setName("");
        Set<ConstraintViolation<KbCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("不能为空")));
    }

    @Test
    void nullName_shouldFail() {
        KbCreateDTO dto = new KbCreateDTO();
        Set<ConstraintViolation<KbCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void nameExceedsMaxLength_shouldFail() {
        KbCreateDTO dto = new KbCreateDTO();
        dto.setName("a".repeat(101));
        Set<ConstraintViolation<KbCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("100")));
    }

    @Test
    void chatSendDTO_shouldHaveDefaultTopK() {
        ChatSendDTO dto = new ChatSendDTO();
        assertEquals(5, dto.getTopK());
    }
}

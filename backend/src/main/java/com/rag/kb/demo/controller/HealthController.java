package com.rag.kb.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rag.kb.demo.vo.HealthVO;
import com.rag.kb.demo.vo.Result;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<HealthVO> health() {
        return Result.success(new HealthVO("ok"));
    }
}

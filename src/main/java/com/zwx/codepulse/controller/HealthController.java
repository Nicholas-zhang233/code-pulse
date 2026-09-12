package com.zwx.codepulse.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: 张伟旭
 * @Create: 2026-09-12 15:32
 * @description:
 **/
@RestController("/health")
public class HealthController {
    @RequestMapping("/")
    public String health() {
        return "ok";
    }
}

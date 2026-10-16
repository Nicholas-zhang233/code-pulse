package com.zwx.codepulse.controller;

import cn.hutool.json.JSONUtil;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.exception.ThrowUtils;
import com.zwx.codepulse.langgraph4j.CodeGenWorkflow;
import com.zwx.codepulse.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * 工作流 SSE 控制器
 * 演示 LangGraph4j 工作流的流式输出功能
 */
@RestController
@RequestMapping("/workflow")
@Slf4j
public class WorkflowSseController {

    /**
     * 同步执行工作流
     */
    @PostMapping("/execute")
    public WorkflowContext executeWorkflow(@RequestParam String prompt) {
        log.info("收到同步工作流执行请求: {}", prompt);
        return new CodeGenWorkflow().executeWorkflow(prompt);
    }

    /**
     * Flux 流式执行工作流（结构化事件：step / message）
     * <p>
     * 这是专家模式实际使用的输出形态，前端按事件名区分阶段进度与代码正文
     */
    @GetMapping(value = "/execute-events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> executeWorkflowEvents(@RequestParam String prompt, @RequestParam Long appId) {
        log.info("收到结构化事件工作流执行请求: {}", prompt);
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR);
        return new CodeGenWorkflow().expertWorkflowStream(prompt, appId)
                .map(event -> ServerSentEvent.<String>builder()
                        .event(event.event())
                        .data(JSONUtil.toJsonStr(event.data()))
                        .build());
    }

    /**
     * Flux 流式执行工作流（旧版文本帧，供调试使用）
     */
    @Deprecated
    @GetMapping(value = "/execute-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeWorkflowWithFlux(@RequestParam String prompt,@RequestParam Long appId) {
        log.info("收到 Flux 工作流执行请求: {}", prompt);
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR);
        return new CodeGenWorkflow().executeWorkflowWithFlux(prompt,appId);
    }


    /**
     * SSE 流式执行工作流
     */
    @GetMapping(value = "/execute-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeWorkflowWithSse(@RequestParam String prompt,@RequestParam Long appId) {
        log.info("收到 SSE 工作流执行请求: {}", prompt);
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR);
        return new CodeGenWorkflow().executeWorkflowWithSse(prompt,appId);
    }
}

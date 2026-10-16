package com.zwx.codepulse.langgraph4j;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zwx.codepulse.exception.BusinessException;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.langgraph4j.model.QualityResult;
import com.zwx.codepulse.langgraph4j.model.WorkflowEvent;
import com.zwx.codepulse.langgraph4j.node.*;
import com.zwx.codepulse.langgraph4j.state.WorkflowContext;
import com.zwx.codepulse.model.enums.CodeGenTypeEnum;
import com.zwx.codepulse.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

@Slf4j
public class CodeGenWorkflow {

    /**
     * 创建完整的工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 添加节点 - 使用完整实现的节点
                    .addNode("image_collector", ImageCollectorNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())

                    // 添加边
                    .addEdge(START, "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")
                    // 新增质检条件边：根据质检结果决定下一步
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    "build", "project_builder",   // 质检通过且需要构建
                                    "skip_build", END,            // 质检通过但跳过构建
                                    "fail", "code_generator"      // 质检失败，重新生成
                            ))
                    .addEdge("project_builder", END)

                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流创建失败");
        }
    }

    /**
     * 执行工作流（Flux 流式输出版本，输出结构化事件）
     * <p>
     * 事件类型：
     * - step：阶段进度，data 为 {stepNumber, currentStep, status}
     * - message：文本内容，data 为字符串（当前为代码生成节点产出的完整代码正文）
     */
    public Flux<WorkflowEvent> expertWorkflowStream(String originalPrompt, Long appId) {
        return expertWorkflowStream(originalPrompt, appId, null);
    }

    /**
     * 执行工作流（Flux 流式输出版本），并在工作流结束后回调最终上下文
     * <p>
     * 各节点共享同一个 WorkflowContext 实例，因此可以在流结束后安全读取最终状态，
     * 用于把 RouterNode 实际选中的代码生成类型回写到应用上
     *
     * @param originalPrompt 用户原始提示词
     * @param appId          应用 ID
     * @param onComplete     工作流全部节点执行成功后的回调，允许为 null
     */
    public Flux<WorkflowEvent> expertWorkflowStream(String originalPrompt, Long appId,
                                                    Consumer<WorkflowContext> onComplete) {
        return Flux.create(sink -> {
            // 使用专用线程池(workflowAsyncExecutor)执行工作流,避免每次调用新建线程,
            Executor executor = SpringContextUtil.getBean("workflowAsyncExecutor", Executor.class);
            executor.execute(() -> {
                // 工作流各节点共享同一个 WorkflowContext 实例，这里持有最终状态用于结束后回调
                AtomicReference<WorkflowContext> finalContextRef = new AtomicReference<>();
                try {
                    CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                    WorkflowContext initialContext = WorkflowContext.builder()
                            .appId(appId)
                            .originalPrompt(originalPrompt)
                            .currentStep("初始化")
                            .build();
                    finalContextRef.set(initialContext);
                    sink.next(WorkflowEvent.step(Map.of(
                            "stepNumber", 0,
                            "currentStep", "初始化",
                            "status", "completed"
                    )));
                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("工作流图:\n{}", graph.content());

                    int stepCounter = 1;
                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                        log.info("--- 第 {} 步完成 ---", stepCounter);
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            finalContextRef.set(currentContext);
                            sink.next(WorkflowEvent.step(Map.of(
                                    "stepNumber", stepCounter,
                                    "currentStep", StrUtil.blankToDefault(currentContext.getCurrentStep(), "处理中"),
                                    "status", "completed"
                            )));
                            log.info("当前步骤上下文: {}", currentContext);
                        }
                        stepCounter++;
                    }
                    // 工作流全部节点执行完成后，把代码正文作为文本内容推给前端
                    WorkflowContext finalContext = finalContextRef.get();
                    if (finalContext != null && StrUtil.isNotBlank(finalContext.getGeneratedCodeOutput())) {
                        sink.next(WorkflowEvent.message(finalContext.getGeneratedCodeOutput()));
                    }
                    log.info("代码生成工作流执行完成！");
                    // 回调放在 complete 之前，保证前端收到 done 事件时生成类型已经落库
                    if (onComplete != null && finalContext != null) {
                        try {
                            onComplete.accept(finalContext);
                        } catch (Exception callbackError) {
                            log.error("工作流完成回调执行失败: {}", callbackError.getMessage(), callbackError);
                        }
                    }
                    sink.complete();
                } catch (Exception e) {
                    // 错误信号统一交给下游的异常处理器转成 business-error 事件，
                    // 这里不再额外 sink.next：FluxCreate 的 sink 在 error 之后会忽略后续信号
                    log.error("工作流执行失败: {}", e.getMessage(), e);
                    sink.error(e);
                }
            });
        });
    }

    /**
     * 执行工作流（Flux 流式输出版本，输出旧版 SSE 文本帧）
     *
     * @deprecated 业务链路请使用 {@link #expertWorkflowStream(String, Long)}，本方法仅供调试端点使用
     */
    @Deprecated
    public Flux<String> executeWorkflowWithFlux(String originalPrompt, Long appId) {
        return expertWorkflowStream(originalPrompt, appId)
                .map(event -> formatSseEvent(event.event(),
                        event.event().equals(WorkflowEvent.EVENT_STEP) ? event.data() : Map.of("d", event.data())));
    }

    /**
     * 格式化 SSE 事件的辅助方法
     */
    private String formatSseEvent(String eventType, Object data) {
        try {
            String jsonData = JSONUtil.toJsonStr(data);
            return "event: " + eventType + "\ndata: " + jsonData + "\n\n";
        } catch (Exception e) {
            log.error("格式化 SSE 事件失败: {}", e.getMessage(), e);
            return "event: error\ndata: {\"error\":\"格式化失败\"}\n\n";
        }
    }

    /**
     * 执行工作流（SSE 流式输出版本）
     */
    public SseEmitter executeWorkflowWithSse(String originalPrompt,Long appId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        Executor executor = SpringContextUtil.getBean("workflowAsyncExecutor", Executor.class);
        executor.execute(() -> {
            try {
                CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                WorkflowContext initialContext = WorkflowContext.builder()
                        .appId(appId)
                        .originalPrompt(originalPrompt)
                        .currentStep("初始化")
                        .build();
                sendSseEvent(emitter, "workflow_start", Map.of(
                        "message", "开始执行代码生成工作流",
                        "originalPrompt", originalPrompt
                ));
                GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                log.info("工作流图:\n{}", graph.content());

                int stepCounter = 1;
                for (NodeOutput<MessagesState<String>> step : workflow.stream(
                        Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                    log.info("--- 第 {} 步完成 ---", stepCounter);
                    WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                    if (currentContext != null) {
                        sendSseEvent(emitter, "step_completed", Map.of(
                                "stepNumber", stepCounter,
                                "currentStep", currentContext.getCurrentStep()
                        ));
                        log.info("当前步骤上下文: {}", currentContext);
                    }
                    stepCounter++;
                }
                sendSseEvent(emitter, "workflow_completed", Map.of(
                        "message", "代码生成工作流执行完成！"
                ));
                log.info("代码生成工作流执行完成！");
                emitter.complete();
            } catch (Exception e) {
                log.error("工作流执行失败: {}", e.getMessage(), e);
                sendSseEvent(emitter, "workflow_error", Map.of(
                        "error", e.getMessage(),
                        "message", "工作流执行失败"
                ));
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /**
     * 发送 SSE 事件的辅助方法
     */
    private void sendSseEvent(SseEmitter emitter, String eventType, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(data));
        } catch (IOException e) {
            log.error("发送 SSE 事件失败: {}", e.getMessage(), e);
        }
    }



    /**
     * 执行工作流
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();

        // 初始化 WorkflowContext
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("初始化")
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", graph.content());
        log.info("开始执行代码生成工作流");

        WorkflowContext finalContext = null;
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            // 显示当前状态
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("当前步骤上下文: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("代码生成工作流执行完成！");
        return finalContext;
    }

    /**
     * 根据质检结果决定下一步
     *
     * @param state
     * @return
     */
    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();
        // 如果质检失败，重新生成代码
        if (qualityResult == null || !qualityResult.getIsValid()) {
            // 重试次数到达上限后不再重新生成，避免 AI 代码生成 + 质检长时间死循环
            if (context.getRetryCount() >= WorkflowContext.MAX_RETRY_COUNT) {
                log.error("代码质检连续 {} 次未通过，结束工作流并保留已生成的代码",
                        context.getRetryCount());
                context.setErrorMessage("代码质检连续未通过，已保留最后一次生成的代码");
                context.setCurrentStep("质检未通过");
                return "skip_build";
            }
            log.error("代码质检失败，需要重新生成代码（当前重试次数 {}）", context.getRetryCount());
            return "fail";
        }
        // 质检通过，使用原有的构建路由逻辑
        log.info("代码质检通过，继续后续流程");
        return routeBuildOrSkip(state);
    }

    /**
     * 根据代码生成类型决定是否需要构建
     *
     * @param state
     * @return
     */
    private String routeBuildOrSkip(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();
        // HTML 和 MULTI_FILE 类型不需要构建，直接结束
        if (generationType == CodeGenTypeEnum.HTML || generationType == CodeGenTypeEnum.MULTI_FILE) {
            return "skip_build";
        }
        // VUE_PROJECT 需要构建
        return "build";
    }

}

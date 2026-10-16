package com.zwx.codepulse.langgraph4j.model;

/**
 * 工作流对外输出的事件
 * <p>
 * 使用结构化载体而不是直接拼接 SSE 字符串，避免 Spring 返回 Flux 时
 * 把已经拼好的 SSE 帧再包一层 data 字段导致双重编码
 *
 * @param event 事件名（message / step）
 * @param data  事件数据，由调用方序列化为 JSON
 */
public record WorkflowEvent(String event, Object data) {

    /**
     * 文本内容事件，前端按 {"d": "..."} 协议拼接展示
     */
    public static final String EVENT_MESSAGE = "message";

    /**
     * 工作流阶段进度事件
     */
    public static final String EVENT_STEP = "step";

    public static WorkflowEvent message(Object data) {
        return new WorkflowEvent(EVENT_MESSAGE, data);
    }

    public static WorkflowEvent step(Object data) {
        return new WorkflowEvent(EVENT_STEP, data);
    }
}

package dev.langchain4j.model.openai.internal.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 扩展 OpenAI 兼容流式响应的 delta 结构，用于接收 DeepSeek 返回的 reasoning_content。
 */
@JsonDeserialize(builder = Delta.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class Delta {

    @JsonProperty
    private final String role;
    @JsonProperty
    private final String content;
    @JsonProperty
    private final String reasoningContent;
    @JsonProperty
    private final List<ToolCall> toolCalls;
    @Deprecated
    @JsonProperty
    private final FunctionCall functionCall;

    public Delta(Builder builder) {
        this.role = builder.role;
        this.content = builder.content;
        this.reasoningContent = builder.reasoningContent;
        this.toolCalls = builder.toolCalls;
        this.functionCall = builder.functionCall;
    }

    public String role() {
        return role;
    }

    public String content() {
        return content;
    }

    public String reasoningContent() {
        return reasoningContent;
    }

    public List<ToolCall> toolCalls() {
        return toolCalls;
    }

    @Deprecated
    public FunctionCall functionCall() {
        return functionCall;
    }

    @Override
    public boolean equals(Object another) {
        return this == another || another instanceof Delta other && equalTo(other);
    }

    private boolean equalTo(Delta another) {
        return Objects.equals(role, another.role)
                && Objects.equals(content, another.content)
                && Objects.equals(reasoningContent, another.reasoningContent)
                && Objects.equals(toolCalls, another.toolCalls)
                && Objects.equals(functionCall, another.functionCall);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content, reasoningContent, toolCalls, functionCall);
    }

    @Override
    public String toString() {
        return "Delta{role=" + role
                + ", content=" + content
                + ", reasoningContent=" + reasoningContent
                + ", toolCalls=" + toolCalls
                + ", functionCall=" + functionCall
                + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class Builder {

        private String role;
        private String content;
        private String reasoningContent;
        private List<ToolCall> toolCalls;
        @Deprecated
        private FunctionCall functionCall;

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder reasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
            return this;
        }

        public Builder toolCalls(List<ToolCall> toolCalls) {
            if (toolCalls != null) {
                this.toolCalls = Collections.unmodifiableList(toolCalls);
            }
            return this;
        }

        @Deprecated
        public Builder functionCall(FunctionCall functionCall) {
            this.functionCall = functionCall;
            return this;
        }

        public Delta build() {
            return new Delta(this);
        }
    }
}

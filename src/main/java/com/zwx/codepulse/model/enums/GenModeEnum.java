package com.zwx.codepulse.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 应用生成模式枚举
 * <p>
 * 一般模式：走传统的 AiCodeGeneratorFacade 流式生成链路
 * 专家模式：走 LangGraph4j 工作流（素材收集 + 提示词增强 + 智能路由 + 质检 + 构建）
 */
@Getter
public enum GenModeEnum {

    NORMAL("一般模式", "normal"),
    EXPERT("专家模式", "expert");

    private final String text;
    private final String value;

    GenModeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值，不存在时返回 null
     */
    public static GenModeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (GenModeEnum anEnum : GenModeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 宽松解析生成模式：空值或非法值统一兜底为一般模式
     * <p>
     * 用于历史数据（genMode 为 NULL）以及前端传入异常值的场景，避免生成入口直接报错
     *
     * @param value 枚举值的value
     * @return 一定非空的枚举值
     */
    public static GenModeEnum of(String value) {
        GenModeEnum genModeEnum = getEnumByValue(value);
        return genModeEnum == null ? NORMAL : genModeEnum;
    }
}

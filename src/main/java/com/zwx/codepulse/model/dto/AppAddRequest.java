package com.zwx.codepulse.model.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    @NotBlank
    private String initPrompt;

    /**
     * 生成模式（normal：一般模式，expert：专家模式），为空时按一般模式处理
     */
    private String genMode;

    private static final long serialVersionUID = 1L;
} 
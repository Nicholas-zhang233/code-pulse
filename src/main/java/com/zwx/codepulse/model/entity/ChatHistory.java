package com.zwx.codepulse.model.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话历史 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_history")
public class ChatHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 消息
     */
    private String message;

    /**
     * user/ai
     */
    @TableField("messageType")
    private String messageType;

    /**
     * 应用id
     */
    @TableField("appId")
    private Long appId;

    /**
     * 创建用户id
     */
    @TableField("userId")
    private Long userId;

    /**
     * 创建时间
     */
    @TableField("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableField(value = "isDelete")
    private Integer isDelete;

}
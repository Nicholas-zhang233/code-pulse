package com.zwx.codepulse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwx.codepulse.model.entity.ChatHistory;
import com.zwx.codepulse.model.vo.ChatHistoryQueryRequest;

/**
 * @author: 张伟旭
 * @Create: 2026-09-18 21:25
 * @description:
 **/

public interface ChatHistoryService {
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);
    boolean deleteByAppId(Long appId);
    QueryWrapper<ChatHistory> getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}

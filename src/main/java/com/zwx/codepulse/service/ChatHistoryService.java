package com.zwx.codepulse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zwx.codepulse.model.entity.ChatHistory;
import com.zwx.codepulse.model.entity.User;
import com.zwx.codepulse.model.vo.ChatHistoryQueryRequest;
import com.zwx.codepulse.model.vo.LoginUserVO;
import com.zwx.codepulse.model.vo.UserVO;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * @author: 张伟旭
 * @Create: 2026-09-18 21:25
 * @description:
 **/

public interface ChatHistoryService {
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    boolean deleteByAppId(Long appId);

    QueryWrapper<ChatHistory> getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               LoginUserVO loginUser);

    Page<ChatHistory> listAllChatHistoryByPageForAdmin(ChatHistoryQueryRequest chatHistoryQueryRequest);
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}

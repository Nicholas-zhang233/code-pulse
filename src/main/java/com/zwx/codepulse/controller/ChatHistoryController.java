package com.zwx.codepulse.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zwx.codepulse.common.BaseResponse;
import com.zwx.codepulse.common.ResultUtils;
import com.zwx.codepulse.constant.UserConstant;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.exception.ThrowUtils;
import com.zwx.codepulse.model.entity.ChatHistory;
import com.zwx.codepulse.model.vo.ChatHistoryQueryRequest;
import com.zwx.codepulse.model.vo.LoginUserVO;
import com.zwx.codepulse.service.ChatHistoryService;
import com.zwx.codepulse.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * @author: 张伟旭
 * @Create: 2026-09-18 21:51
 * @description:
 **/
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime) {
        LoginUserVO loginUserVO = userService.getLoginUserVO(StpUtil.getLoginIdAsLong());
        ThrowUtils.throwIf(loginUserVO == null, ErrorCode.NOT_LOGIN_ERROR);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUserVO);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<ChatHistory> result = chatHistoryService.listAllChatHistoryByPageForAdmin(chatHistoryQueryRequest);
        return ResultUtils.success(result);
    }


}

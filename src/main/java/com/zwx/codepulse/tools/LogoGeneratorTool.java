package com.zwx.codepulse.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.zwx.codepulse.exception.BusinessException;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.langgraph4j.model.ImageResource;
import com.zwx.codepulse.langgraph4j.model.enums.ImageCategoryEnum;
import com.zwx.codepulse.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
@Slf4j
@Component
public class LogoGeneratorTool {

    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${dashscope.image-model:qwen-image-3.0}")
    private String imageModel;

    @Resource
    private CosManager cosManager;

    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String description) {
        List<ImageResource> logoList = new ArrayList<>();
        try {
            // 构建 Logo 设计提示词
            String logoPrompt = String.format("生成 Logo，Logo 中禁止包含任何文字！Logo 介绍：%s", description);
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(List.of(
                            Collections.singletonMap("text", logoPrompt)
                    ))
                    .build();
            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("prompt_extend", true);
            parameters.put("n", 1); // 生成 1 张足够，因为 AI 不知道哪张最好
            parameters.put("size", "512*512");
            parameters.put("negative_prompt", "图片中禁止包含任何文字");
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(dashScopeApiKey)
                    .model(imageModel)
                    .messages(Arrays.asList(userMessage))
                    .parameters(parameters)
                    .build();
            MultiModalConversationResult result = conv.call(param);
            if (result != null && result.getOutput() != null && result.getOutput().getChoices() != null) {
                List<MultiModalConversationOutput.Choice> results = result.getOutput().getChoices();
                for (MultiModalConversationOutput.Choice imageResult : results) {
                    if (imageResult.getMessage() == null) {
                        continue;
                    }
                    List<Map<String, Object>> contentList = imageResult.getMessage().getContent();
                    if (contentList == null || contentList.isEmpty()) {
                        continue;
                    }
                    contentList.forEach(item -> {
                        if (item == null || item.isEmpty()) {
                            return;
                        }
                        Object imageUrl = item.get("image");
                        if (imageUrl == null) {
                            return;
                        }
                        String imageUrlString = imageUrl.toString();
                        if (StrUtil.isNotBlank(imageUrlString)) {
                            // 模型返回的临时 URL 不可长期使用，下载后上传到 COS 换取稳定访问地址
                            String cosUrl = downloadAndUploadToCos(imageUrlString);
                            logoList.add(ImageResource.builder()
                                    .category(ImageCategoryEnum.LOGO)
                                    .description(description)
                                    .url(cosUrl)
                                    .build());
                        }
                    });

                }
            }
        } catch (BusinessException e) {
            // 下载或上传 COS 失败时终止本次工具调用，不返回半成品结果
            throw e;
        } catch (Exception e) {
            log.error("生成 Logo 失败: {}", e.getMessage(), e);
        }
        return logoList;
    }

    /**
     * 下载图片到本地临时文件并上传到 COS，返回 COS 上的稳定访问 URL
     *
     * @param imageUrl 模型返回的图片临时 URL
     * @return 上传 COS 成功后的访问 URL
     */
    public String downloadAndUploadToCos(String imageUrl) {
        File tempFile = null;
        try {
            // 从 URL 路径推断图片后缀，无法识别时默认使用 .png
            String suffix = getImageSuffix(imageUrl);
            tempFile = FileUtil.createTempFile("logo_", suffix, true);
            HttpUtil.downloadFile(imageUrl, tempFile);
            // 上传到 COS，目录风格与 Mermaid 图片保持一致
            String keyName = String.format("/logo/%s/%s",
                    RandomUtil.randomString(5), tempFile.getName());
            String cosUrl = cosManager.uploadFile(keyName, tempFile);
            if (StrUtil.isBlank(cosUrl)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Logo 图片下载或上传 COS 失败");
            }
            return cosUrl;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Logo 图片下载或上传 COS 失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Logo 图片下载或上传 COS 失败");
        } finally {
            // 无论成功失败都清理临时文件
            if (tempFile != null && tempFile.exists()) {
                FileUtil.del(tempFile);
            }
        }
    }

    /**
     * 从图片 URL 中推断文件后缀
     */
    private String getImageSuffix(String imageUrl) {
        String path = imageUrl;
        int queryIndex = path.indexOf('?');
        if (queryIndex > 0) {
            path = path.substring(0, queryIndex);
        }
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < path.length() - 1) {
            String ext = path.substring(dotIndex);
            if (ext.matches("\\.[a-zA-Z0-9]+")) {
                return ext;
            }
        }
        return ".png";
    }
}

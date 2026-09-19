package com.zwx.codepulse.ai.core;

import com.zwx.codepulse.ai.AiCodeGeneratorServiceFactory;
import com.zwx.codepulse.ai.core.parser.MultiFileCodeParser;
import com.zwx.codepulse.ai.core.saver.CodeFileSaverExecutor;
import com.zwx.codepulse.ai.model.MultiFileCodeResult;
import com.zwx.codepulse.constant.AppConstant;
import com.zwx.codepulse.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 多文件流式与普通输出诊断测试。
 *
 * <p>该测试只验证 MULTI_FILE 模式，不测试单文件 HTML 模式。</p>
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MultiFileStreamingVsOrdinaryDiagnosticTest {

    private static final String USER_PROMPT = "做一个简洁的留言板，包含提交留言、删除留言、点赞和本地保存功能，代码保持完整但不要过度复杂";

    private static final Duration MODEL_TIMEOUT = Duration.ofMinutes(3);

    private static final Pattern HTML_BLOCK_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private static final Pattern CSS_BLOCK_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private static final Pattern JS_BLOCK_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private static final long STREAM_APP_ID = System.currentTimeMillis();

    private static final long ORDINARY_APP_ID = STREAM_APP_ID + 1;

    private static List<String> streamChunks;

    private static String streamRawResponse;

    private static MultiFileCodeResult streamParsedResult;

    private static MultiFileCodeResult ordinaryResult;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Test
    @Order(1)
    @DisplayName("流式生成应返回非空 chunk 序列")
    void streamGenerationShouldReturnNonBlankChunks() {
        printTestHeader(1, "验证当前多文件流式入口能不能返回 chunk，以及每个 chunk 的顺序和长度");

        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                USER_PROMPT,
                CodeGenTypeEnum.MULTI_FILE,
                STREAM_APP_ID
        );
        streamChunks = codeStream.collectList().block(MODEL_TIMEOUT);

        assertNotNull(streamChunks, "流式生成未返回 chunk 列表");
        assertFalse(streamChunks.isEmpty(), "流式生成返回了空 chunk 列表");

        streamRawResponse = String.join("", streamChunks);
        assertFalse(streamRawResponse.isBlank(), "流式生成完整响应为空");

        System.out.printf("流式 appId：%d%n", STREAM_APP_ID);
        System.out.printf("chunk 数量：%d，拼接后字符数：%d%n", streamChunks.size(), streamRawResponse.length());
        for (int i = 0; i < streamChunks.size(); i++) {
            String chunk = streamChunks.get(i);
            System.out.printf(
                    "chunk[%d] 长度=%d，内容片段=%s%n",
                    i,
                    chunk == null ? 0 : chunk.length(),
                    preview(chunk, 180)
            );
        }
        System.out.println("测试1结论：流式入口返回了可拼接的数据");
    }

    @Test
    @Order(2)
    @DisplayName("流式原始响应应包含 HTML、CSS、JavaScript 代码块")
    void streamRawResponseShouldContainHtmlCssJsCodeBlocks() {
        printTestHeader(2, "打印流式拼接后的完整原始响应，并统计 html、css、js/javascript 代码块数量");

        requireStreamRawResponse();

        System.out.println("========== 流式完整原始响应开始 ==========");
        System.out.println(streamRawResponse);
        System.out.println("========== 流式完整原始响应结束 ==========");

        int htmlBlockCount = countMatches(streamRawResponse, HTML_BLOCK_PATTERN);
        int cssBlockCount = countMatches(streamRawResponse, CSS_BLOCK_PATTERN);
        int jsBlockCount = countMatches(streamRawResponse, JS_BLOCK_PATTERN);

        System.out.printf("代码块数量：html=%d，css=%d，js/javascript=%d%n", htmlBlockCount, cssBlockCount, jsBlockCount);

        assertAll("流式原始响应应包含 HTML、CSS、JavaScript 三段代码块",
                () -> assertTrue(htmlBlockCount > 0, "流式原始响应缺少 HTML 代码块"),
                () -> assertTrue(cssBlockCount > 0, "流式原始响应缺少 CSS 代码块"),
                () -> assertTrue(jsBlockCount > 0, "流式原始响应缺少 JavaScript 代码块")
        );

        System.out.println("测试2结论：流式原始响应包含 HTML、CSS、JavaScript 代码块");
    }

    @Test
    @Order(3)
    @DisplayName("流式原始响应应能被多文件解析器解析出三段代码")
    void streamRawResponseShouldBeParsedIntoThreeParts() {
        printTestHeader(3, "使用当前 MultiFileCodeParser 解析流式完整原始响应，判断是不是解析阶段丢代码");

        requireStreamRawResponse();

        streamParsedResult = new MultiFileCodeParser().parseCode(streamRawResponse);
        assertNotNull(streamParsedResult, "流式原始响应解析结果为空");

        int htmlLength = lengthOf(streamParsedResult.getHtmlCode());
        int cssLength = lengthOf(streamParsedResult.getCssCode());
        int jsLength = lengthOf(streamParsedResult.getJsCode());

        System.out.printf("解析结果长度：htmlCode=%d，cssCode=%d，jsCode=%d%n", htmlLength, cssLength, jsLength);

        assertAll("流式原始响应解析后应得到 HTML、CSS、JavaScript 三段代码",
                () -> assertFalse(isBlank(streamParsedResult.getHtmlCode()), "流式原始响应解析后缺少 HTML 代码"),
                () -> assertFalse(isBlank(streamParsedResult.getCssCode()), "流式原始响应解析后缺少 CSS 代码"),
                () -> assertFalse(isBlank(streamParsedResult.getJsCode()), "流式原始响应解析后缺少 JavaScript 代码")
        );

        System.out.println("测试3结论：流式原始响应可以被解析为三段代码");
    }

    @Test
    @Order(4)
    @DisplayName("流式生成应保存 index.html、style.css、script.js")
    void streamGenerationShouldSaveHtmlCssJsFiles() {
        printTestHeader(4, "检查流式生成完成后是否在 appId 目录下保存了 HTML、CSS、JavaScript 三个文件");

        File outputDir = outputDirFor(STREAM_APP_ID);
        System.out.printf("流式输出目录：%s%n", outputDir.getAbsolutePath());

        assertAll("流式生成应保存 HTML、CSS、JavaScript 三个文件",
                () -> assertTrue(outputDir.isDirectory(), "流式生成未创建输出目录：" + outputDir.getAbsolutePath()),
                () -> assertNonBlankFile(new File(outputDir, "index.html"), "流式生成 HTML"),
                () -> assertNonBlankFile(new File(outputDir, "style.css"), "流式生成 CSS"),
                () -> assertNonBlankFile(new File(outputDir, "script.js"), "流式生成 JavaScript")
        );

        System.out.println("测试4结论：流式生成保存了 HTML、CSS、JavaScript 三个文件");
    }

    @Test
    @Order(5)
    @DisplayName("普通输出应返回完整 htmlCode、cssCode、jsCode")
    void ordinaryOutputShouldContainHtmlCssJs() {
        printTestHeader(5, "真实调用普通多文件 AI Service，打印完整三段代码，用来对比流式输出是否漏了 CSS/JS");

        ordinaryResult = aiCodeGeneratorServiceFactory
                .getAiCodeGeneratorService(ORDINARY_APP_ID)
                .generateMultiFileCode(USER_PROMPT);

        System.out.printf("普通 appId：%d%n", ORDINARY_APP_ID);
        assertNotNull(ordinaryResult, "普通多文件输出结果为空");

        System.out.println("========== 普通输出 HTML 开始 ==========");
        System.out.println(ordinaryResult.getHtmlCode());
        System.out.println("========== 普通输出 HTML 结束 ==========");
        System.out.println("========== 普通输出 CSS 开始 ==========");
        System.out.println(ordinaryResult.getCssCode());
        System.out.println("========== 普通输出 CSS 结束 ==========");
        System.out.println("========== 普通输出 JavaScript 开始 ==========");
        System.out.println(ordinaryResult.getJsCode());
        System.out.println("========== 普通输出 JavaScript 结束 ==========");

        int htmlLength = lengthOf(ordinaryResult.getHtmlCode());
        int cssLength = lengthOf(ordinaryResult.getCssCode());
        int jsLength = lengthOf(ordinaryResult.getJsCode());
        System.out.printf("普通输出长度：htmlCode=%d，cssCode=%d，jsCode=%d%n", htmlLength, cssLength, jsLength);

        assertAll("普通输出应包含 HTML、CSS、JavaScript 三段代码",
                () -> assertFalse(isBlank(ordinaryResult.getHtmlCode()), "普通输出缺少 HTML 代码"),
                () -> assertFalse(isBlank(ordinaryResult.getCssCode()), "普通输出缺少 CSS 代码"),
                () -> assertFalse(isBlank(ordinaryResult.getJsCode()), "普通输出缺少 JavaScript 代码")
        );

        System.out.println("测试5结论：普通输出包含 HTML、CSS、JavaScript 三段代码");
    }

    @Test
    @Order(6)
    @DisplayName("普通输出应保存 index.html、style.css、script.js")
    void ordinaryOutputShouldSaveHtmlCssJsFiles() {
        printTestHeader(6, "把普通输出的 htmlCode/cssCode/jsCode 通过当前保存执行器落盘，验证普通生成保存链路");

        assertNotNull(ordinaryResult, "前置步骤失败：普通输出结果未生成，请先查看 @Order(5) 的控制台输出");

        File outputDir = CodeFileSaverExecutor.executeSaver(
                ordinaryResult,
                CodeGenTypeEnum.MULTI_FILE,
                ORDINARY_APP_ID
        );
        System.out.printf("普通输出目录：%s%n", outputDir.getAbsolutePath());

        assertAll("普通输出应保存 HTML、CSS、JavaScript 三个文件",
                () -> assertTrue(outputDir.isDirectory(), "普通输出未创建输出目录：" + outputDir.getAbsolutePath()),
                () -> assertNonBlankFile(new File(outputDir, "index.html"), "普通输出 HTML"),
                () -> assertNonBlankFile(new File(outputDir, "style.css"), "普通输出 CSS"),
                () -> assertNonBlankFile(new File(outputDir, "script.js"), "普通输出 JavaScript")
        );

        System.out.println("测试6结论：普通输出保存了 HTML、CSS、JavaScript 三个文件");
    }

    private static void printTestHeader(int order, String feature) {
        System.out.printf("%n========== 【测试%d】%s ==========%n", order, feature);
    }

    private static void requireStreamRawResponse() {
        assertNotNull(streamRawResponse, "前置步骤失败：流式原始响应未生成，请先查看 @Order(1) 的控制台输出");
        assertFalse(streamRawResponse.isBlank(), "前置步骤失败：流式原始响应为空，请先查看 @Order(1) 的控制台输出");
    }

    private static File outputDirFor(long appId) {
        return new File(AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator
                + CodeGenTypeEnum.MULTI_FILE.getValue() + "_" + appId);
    }

    private static int countMatches(String content, Pattern pattern) {
        if (content == null) {
            return 0;
        }
        int count = 0;
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static void assertNonBlankFile(File file, String stage) {
        System.out.printf("%s文件：%s，存在=%s，大小=%d bytes%n",
                stage, file.getAbsolutePath(), file.isFile(), file.length());
        assertTrue(file.isFile(), stage + "文件不存在：" + file.getAbsolutePath());
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            assertFalse(content.isBlank(), stage + "文件内容为空：" + file.getAbsolutePath());
        } catch (IOException e) {
            fail(stage + "文件读取失败：" + file.getAbsolutePath(), e);
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private static int lengthOf(String text) {
        return text == null ? 0 : text.length();
    }

    private static String preview(String text, int maxLength) {
        if (text == null) {
            return "<null>";
        }
        String singleLine = text.replace("\r", "\\r").replace("\n", "\\n");
        if (singleLine.length() <= maxLength) {
            return singleLine;
        }
        return singleLine.substring(0, maxLength) + "...";
    }
}

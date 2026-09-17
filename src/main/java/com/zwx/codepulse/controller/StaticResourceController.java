package com.zwx.codepulse.controller;

import com.zwx.codepulse.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 静态资源访问（公开访问，deployKey 即访问凭据）
 */
@RestController
@RequestMapping("/static")
public class StaticResourceController {

    // 应用生成根目录（用于浏览）
    private static final String PREVIEW_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    // deployKey 合法字符：字母、数字、下划线、中划线
    private static final Pattern DEPLOY_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    /**
     * 提供静态资源访问，支持目录重定向
     * 访问格式：http://localhost:8123/api/static/{deployKey}[/{fileName}]
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        try {
            // 获取资源路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            resourcePath = resourcePath.substring(("/static/" + deployKey).length());
            // 如果是目录访问（不带斜杠），重定向到带斜杠的URL
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }
            // 解析文件路径（同时校验 deployKey 与路径穿越）
            File file = resolvePreviewFile(deployKey, resourcePath);
            // 检查文件是否存在（目录不作为静态资源返回）
            if (file == null || !file.exists() || file.isDirectory()) {
                return ResponseEntity.notFound().build();
            }
            // 返回文件资源
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, getContentType(file.getPath()))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 解析预览文件，并保证最终路径位于预览根目录内，防止路径穿越
     *
     * @param deployKey    应用部署标识
     * @param resourcePath 相对资源路径（以 / 开头）
     * @return 真实文件路径；输入非法或越界时返回 null
     */
    File resolvePreviewFile(String deployKey, String resourcePath) {
        if (deployKey == null || !DEPLOY_KEY_PATTERN.matcher(deployKey).matches()) {
            return null;
        }
        if (resourcePath == null || !resourcePath.startsWith("/")) {
            return null;
        }
        // 反斜杠在 Windows 下同样是路径分隔符，直接拒绝
        if (resourcePath.indexOf('\\') >= 0) {
            return null;
        }
        // 拒绝包含 .. 路径段的越界尝试
        for (String segment : resourcePath.split("/")) {
            if ("..".equals(segment)) {
                return null;
            }
        }
        try {
            File root = new File(PREVIEW_ROOT_DIR).getCanonicalFile();
            File file = new File(root, deployKey + resourcePath).getCanonicalFile();
            // 规范化后必须仍位于预览根目录内
            if (!file.getPath().startsWith(root.getPath() + File.separator)) {
                return null;
            }
            return file;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 返回文件的 Content-Type：优先按常见扩展名返回带字符编码的类型，
     * 未命中时交给 Spring 的 MediaTypeFactory 推断，最后兜底为二进制流
     */
    private String getContentType(String filePath) {
        String lowerPath = filePath.toLowerCase(Locale.ROOT);
        if (lowerPath.endsWith(".html") || lowerPath.endsWith(".htm")) {
            return "text/html; charset=UTF-8";
        }
        if (lowerPath.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (lowerPath.endsWith(".js") || lowerPath.endsWith(".mjs")) {
            return "application/javascript; charset=UTF-8";
        }
        if (lowerPath.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        }
        if (lowerPath.endsWith(".svg")) {
            return "image/svg+xml; charset=UTF-8";
        }
        if (lowerPath.endsWith(".txt")) {
            return "text/plain; charset=UTF-8";
        }
        if (lowerPath.endsWith(".png")) {
            return "image/png";
        }
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerPath.endsWith(".gif")) {
            return "image/gif";
        }
        if (lowerPath.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerPath.endsWith(".ico")) {
            return "image/x-icon";
        }
        return MediaTypeFactory.getMediaType(filePath)
                .map(MediaType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }
}

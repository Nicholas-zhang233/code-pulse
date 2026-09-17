package com.zwx.codepulse.controller;

import com.zwx.codepulse.constant.AppConstant;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 静态资源路径解析测试
 */
class StaticResourceControllerTest {

    private final StaticResourceController controller = new StaticResourceController();

    private final String previewRoot = new File(AppConstant.CODE_OUTPUT_ROOT_DIR).getPath();

    @Test
    void resolvePreviewFileReturnsIndexHtml() {
        File file = controller.resolvePreviewFile("multi_file_1", "/index.html");
        assertNotNull(file);
        assertEquals(previewRoot + File.separator + "multi_file_1" + File.separator + "index.html", file.getPath());
    }

    @Test
    void resolvePreviewFileReturnsNestedFile() {
        File file = controller.resolvePreviewFile("multi_file_1", "/assets/app.js");
        assertNotNull(file);
        assertEquals(
                previewRoot + File.separator + "multi_file_1" + File.separator + "assets" + File.separator + "app.js",
                file.getPath());
    }

    @Test
    void resolvePreviewFileRejectsPathTraversal() {
        assertNull(controller.resolvePreviewFile("multi_file_1", "/../../pom.xml"));
        assertNull(controller.resolvePreviewFile("multi_file_1", "/../secret.txt"));
        assertNull(controller.resolvePreviewFile("multi_file_1", "/assets/../../pom.xml"));
        // Windows 下反斜杠同样是路径分隔符，必须一并拦截
        assertNull(controller.resolvePreviewFile("multi_file_1", "/..\\..\\pom.xml"));
    }

    @Test
    void resolvePreviewFileRejectsIllegalInput() {
        // 非法 deployKey
        assertNull(controller.resolvePreviewFile(null, "/index.html"));
        assertNull(controller.resolvePreviewFile("", "/index.html"));
        assertNull(controller.resolvePreviewFile("..", "/index.html"));
        assertNull(controller.resolvePreviewFile("a/b", "/index.html"));
        assertNull(controller.resolvePreviewFile("a b", "/index.html"));
        assertNull(controller.resolvePreviewFile("multi_file_1 ", "/index.html"));
        // 非法资源路径
        assertNull(controller.resolvePreviewFile("multi_file_1", null));
        assertNull(controller.resolvePreviewFile("multi_file_1", "index.html"));
    }

    @Test
    void resolvePreviewFileAlwaysStaysInPreviewRoot() {
        String[] resourcePaths = {"/index.html", "/assets/app.js", "/a/b/c.css", "/./index.html"};
        for (String resourcePath : resourcePaths) {
            File file = controller.resolvePreviewFile("multi_file_1", resourcePath);
            assertNotNull(file);
            assertTrue(file.getPath().startsWith(previewRoot + File.separator), "越界路径：" + file.getPath());
        }
    }
}

package com.zwx.codepulse.ai.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zwx.codepulse.exception.BusinessException;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 抽象代码文件保存器 - 模板方法模式
 *
 */
public abstract class CodeFileSaverTemplate<T> {

    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    public final File saveCode(T result) {
        // 验证输入
        validateInput(result);
        // 构建目录
        String baseDirPath = buildUniqueDir();
        // 保存文件
        saveFiles(result, baseDirPath);
        // 返回文件目录对象
        return new File(baseDirPath);
    }

    public final void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isNotBlank(content)) {
            String filePath = dirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }

    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果对象不能为空");
        }
    }

    protected String buildUniqueDir() {
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    protected abstract void saveFiles(T result, String baseDirPath);

    protected abstract CodeGenTypeEnum getCodeType();
}
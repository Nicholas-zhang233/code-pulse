package com.zwx.codepulse.ai.core.saver;

import cn.hutool.core.util.StrUtil;
import com.zwx.codepulse.ai.model.MultiFileCodeResult;
import com.zwx.codepulse.exception.BusinessException;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.model.enums.CodeGenTypeEnum;

/**
 * 多文件代码保存器
 *
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        //   HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        //  CSS 文件
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        //  JavaScript 文件
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}
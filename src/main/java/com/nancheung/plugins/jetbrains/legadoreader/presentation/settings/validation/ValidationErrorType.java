package com.nancheung.plugins.jetbrains.legadoreader.presentation.settings.validation;

/**
 * 验证错误类型枚举
 */
public enum ValidationErrorType {
    /**
     * 参数名为空
     */
    EMPTY_PARAM_NAME,

    /**
     * 参数名重复
     */
    DUPLICATE_PARAM_NAME,

    /**
     * 字体大小无效
     */
    INVALID_FONT_SIZE
}

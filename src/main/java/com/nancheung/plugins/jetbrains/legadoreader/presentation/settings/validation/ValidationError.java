package com.nancheung.plugins.jetbrains.legadoreader.presentation.settings.validation;

/**
 * 验证错误记录
 *
 * @param type     错误类型
 * @param rowIndex 行号（-1 表示非行相关）
 * @param message  错误消息
 */
public record ValidationError(
    ValidationErrorType type,
    int rowIndex,
    String message
) {
}

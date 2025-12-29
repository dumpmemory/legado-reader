package com.nancheung.plugins.jetbrains.legadoreader.presentation.settings.validation;

import java.util.List;

/**
 * 验证结果记录
 *
 * @param isValid 是否验证通过
 * @param errors  错误列表
 */
public record ValidationResult(boolean isValid, List<ValidationError> errors) {

    /**
     * 创建有效的验证结果
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, List.of());
    }

    /**
     * 创建包含单个错误的验证结果
     */
    public static ValidationResult of(ValidationError error) {
        return new ValidationResult(false, List.of(error));
    }

    /**
     * 检查指定行是否有错误
     */
    public boolean hasErrorAt(int rowIndex) {
        return errors.stream().anyMatch(e -> e.rowIndex() == rowIndex);
    }

    /**
     * 获取指定行的所有错误
     */
    public List<ValidationError> errorsAt(int rowIndex) {
        return errors.stream()
            .filter(e -> e.rowIndex() == rowIndex)
            .toList();
    }
}

package com.nancheung.plugins.jetbrains.legadoreader.presentation.settings.validation;

import com.nancheung.plugins.jetbrains.legadoreader.presentation.settings.SettingsViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 设置验证器
 * 集中所有验证逻辑，消除重复代码
 */
public final class SettingsValidator {

    /**
     * 验证所有设置
     */
    public ValidationResult validateAll(List<SettingsViewModel.CustomParamEntry> params, int fontSize) {
        List<ValidationError> errors = new ArrayList<>();

        errors.addAll(validateCustomParams(params).errors());
        errors.addAll(validateFontSize(fontSize).errors());

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 验证自定义参数
     */
    public ValidationResult validateCustomParams(List<SettingsViewModel.CustomParamEntry> params) {
        List<ValidationError> errors = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        for (int i = 0; i < params.size(); i++) {
            SettingsViewModel.CustomParamEntry param = params.get(i);
            String name = param.name().trim();

            // 空名称检查
            if (name.isEmpty()) {
                errors.add(new ValidationError(
                    ValidationErrorType.EMPTY_PARAM_NAME,
                    i,
                    "参数名不能为空"
                ));
                continue;
            }

            // 重复名称检查
            if (!seenNames.add(name)) {
                errors.add(new ValidationError(
                    ValidationErrorType.DUPLICATE_PARAM_NAME,
                    i,
                    "参数名重复: " + name
                ));
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 验证字体大小
     */
    public ValidationResult validateFontSize(int size) {
        if (size < 0 || size > 100) {
            return ValidationResult.of(new ValidationError(
                ValidationErrorType.INVALID_FONT_SIZE,
                -1,
                "字体大小必须在 0-100 之间（0 表示使用编辑器默认）"
            ));
        }
        return ValidationResult.valid();
    }
}

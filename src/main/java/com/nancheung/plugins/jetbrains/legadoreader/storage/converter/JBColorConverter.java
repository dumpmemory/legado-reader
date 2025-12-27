package com.nancheung.plugins.jetbrains.legadoreader.storage.converter;

import com.intellij.ui.JBColor;
import com.intellij.util.xmlb.Converter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JBColor 对象的 XML 序列化转换器
 * 格式：RGB 整数值（例如："65280" 代表绿色）
 *
 * @author NanCheung
 */
@Slf4j
public class JBColorConverter extends Converter<JBColor> {

    @Override
    public @Nullable JBColor fromString(@NotNull String value) {
        if (value.isEmpty()) {
            return null;
        }

        try {
            int rgb = Integer.parseInt(value);
            return new JBColor(rgb, rgb);
        } catch (NumberFormatException e) {
            log.error("rgb值[{}]转换为 JBColor失败，", value, e);
            return null;
        }
    }

    @Override
    public @NotNull String toString(@NotNull JBColor value) {
        return String.valueOf(value.getRGB());
    }
}

package com.nancheung.plugins.jetbrains.legadoreader.storage.converter;

import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Font 对象的 XML 序列化转换器
 * 格式：fontName-style-size（例如："Arial-0-14"）
 *
 * @author NanCheung
 */
public class FontConverter extends Converter<Font> {

    @Override
    public @Nullable Font fromString(@NotNull String value) {
        if (value.isEmpty()) {
            return null;
        }

        try {
            String[] parts = value.split("-");
            if (parts.length != 3) {
                return null;
            }

            String name = parts[0];
            int style = Integer.parseInt(parts[1]);
            int size = Integer.parseInt(parts[2]);

            return new Font(name, style, size);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @NotNull String toString(@NotNull Font value) {
        return value.getFontName() + "-" + value.getStyle() + "-" + value.getSize();
    }
}

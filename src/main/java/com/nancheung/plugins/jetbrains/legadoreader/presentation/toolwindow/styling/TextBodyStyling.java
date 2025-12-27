package com.nancheung.plugins.jetbrains.legadoreader.presentation.toolwindow.styling;

import com.intellij.ui.JBColor;
import com.nancheung.plugins.jetbrains.legadoreader.storage.PluginSettingsStorage;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

/**
 * 正文样式管理器
 * 负责封装 JTextPane 的样式应用逻辑
 *
 * @author NanCheung
 */
public class TextBodyStyling {

    /**
     * 应用样式到 JTextPane
     *
     * @param textPane    目标文本组件
     * @param fontColor   字体颜色
     * @param font        字体
     * @param lineHeight  行高
     */
    public void apply(JTextPane textPane, JBColor fontColor, Font font, double lineHeight) {
        textPane.setForeground(fontColor);
        textPane.setFont(font);
        applyLineHeight(textPane, lineHeight);
    }

    /**
     * 从设置中读取并应用默认样式
     *
     * @param textPane 目标文本组件
     */
    public void applyFromSettings(JTextPane textPane) {
        PluginSettingsStorage.State state = PluginSettingsStorage.getInstance().getState();
        apply(
                textPane,
                state.textBodyFontColor,
                state.textBodyFont,
                state.textBodyLineHeight
        );
    }

    /**
     * 应用行高到 JTextPane
     *
     * @param textPane   目标文本组件
     * @param lineHeight 行高（如 1.5 表示 1.5 倍行距）
     */
    private void applyLineHeight(JTextPane textPane, double lineHeight) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        float lineSpacing = (float) (lineHeight - 1.0);
        StyleConstants.setLineSpacing(attrs, lineSpacing);

        StyledDocument doc = textPane.getStyledDocument();
        doc.setParagraphAttributes(0, doc.getLength(), attrs, false);
    }
}

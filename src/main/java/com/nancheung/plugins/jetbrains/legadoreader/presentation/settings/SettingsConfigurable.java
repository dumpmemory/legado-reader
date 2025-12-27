package com.nancheung.plugins.jetbrains.legadoreader.presentation.settings;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.JBColor;
import com.nancheung.plugins.jetbrains.legadoreader.common.Constant;
import com.nancheung.plugins.jetbrains.legadoreader.event.EventPublisher;
import com.nancheung.plugins.jetbrains.legadoreader.event.SettingsChangedEvent;
import com.nancheung.plugins.jetbrains.legadoreader.storage.PluginSettingsStorage;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class SettingsConfigurable implements SearchableConfigurable {

    private final static String DISPLAY_NAME = "Legado Reader";

    /**
     * SettingsPanel 实例（懒加载，避免在类加载时访问服务）
     */
    private static SettingsPanel settingsPanel;

    @Override
    public @NotNull String getId() {
        return Constant.PLUGIN_SETTING_ID;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return SettingsConfigurable.DISPLAY_NAME;
    }

    @Override
    public @Nullable JComponent createComponent() {
        SettingsPanel ui = instance();
        // 每次打开设置页时，从存储中重新读取设置到 UI（解决取消后再次打开显示未保存值的问题）
        ui.readSettings(PluginSettingsStorage.getInstance().getState());
        return ui.getComponent();
    }

    @Override
    public boolean isModified() {
        if (settingsPanel == null) {
            return false;
        }

        PluginSettingsStorage.State state = PluginSettingsStorage.getInstance().getState();
        if (state == null) {
            return false;
        }

        SettingsPanel ui = instance();

        boolean fontColorModified = !Objects.equals(state.textBodyFontColor, ui.getFontPreviewPane().getForeground());

        Font uiFont = ui.getFontPreviewPane().getFont();
        boolean fontModified = !Objects.equals(state.textBodyFont, uiFont);

        String currentCustomParam = ui.getApiCustomParamTextArea().getText();
        String storedCustomParam = state.apiCustomParam == null ? "" : state.apiCustomParam;
        boolean customParamModified = !currentCustomParam.equals(storedCustomParam);

        boolean errorLogModified = Boolean.TRUE.equals(state.enableErrorLog) != ui.getEnableErrorLogCheckBox().isSelected();

        boolean inLineModelModified = Boolean.TRUE.equals(state.enableShowBodyInLine) != ui.getEnableInLineModelCheckBox().isSelected();

        // 行高比较（注意精度问题）
        double currentLineHeight = (double) ui.getLineHeightSpinner().getValue();
        double storedLineHeight = state.textBodyLineHeight != null ? state.textBodyLineHeight : 1.5;
        boolean lineHeightModified = Math.abs(currentLineHeight - storedLineHeight) > 0.001;

        return fontColorModified || fontModified || customParamModified ||
                errorLogModified || inLineModelModified || lineHeightModified;
    }

    @Override
    public void reset() {
        // 重置为上次保存的值
        if (settingsPanel != null) {
            settingsPanel.readSettings(PluginSettingsStorage.getInstance().getState());
        }
    }

    @Override
    public void apply() {
        // 保存设置
        saveSettings();

        // 发布设置变更事件，通知 UI 更新
        PluginSettingsStorage storage = PluginSettingsStorage.getInstance();
        JBColor fontColor = storage.getState().textBodyFontColor;
        Font font = storage.getState().textBodyFont;
        double lineHeight = storage.getState().textBodyLineHeight;

        EventPublisher.getInstance().publish(SettingsChangedEvent.fontSettings(fontColor, font, lineHeight));
    }

    /**
     * 获取 SettingsPanel 实例（懒加载）
     * 只在用户打开设置页面时才创建实例，避免在类加载时访问服务
     *
     * @return SettingsPanel 实例
     */
    public static SettingsPanel instance() {
        if (settingsPanel == null) {
            settingsPanel = new SettingsPanel();

            settingsPanel.readSettings(PluginSettingsStorage.getInstance().getState());
        }
        return settingsPanel;
    }

    public void saveSettings() {
        SettingsPanel ui = instance();
        // 直接修改 State 字段，框架会自动持久化
        PluginSettingsStorage.State state = PluginSettingsStorage.getInstance().getState();
        if (state == null) {
            return;
        }
        state.apiCustomParam = ui.getApiCustomParamTextArea().getText();

        Color foreground = ui.getFontPreviewPane().getForeground();
        state.textBodyFontColor = new JBColor(foreground,foreground);
        state.textBodyFont = ui.getFontPreviewPane().getFont();
        state.textBodyLineHeight = (double) ui.getLineHeightSpinner().getValue();

        state.enableErrorLog = ui.getEnableErrorLogCheckBox().isSelected();
        state.enableShowBodyInLine = ui.getEnableInLineModelCheckBox().isSelected();
    }
}

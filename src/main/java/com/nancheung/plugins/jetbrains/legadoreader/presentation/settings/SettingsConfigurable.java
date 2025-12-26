package com.nancheung.plugins.jetbrains.legadoreader.presentation.settings;

import com.intellij.openapi.options.SearchableConfigurable;
import com.nancheung.plugins.jetbrains.legadoreader.common.Constant;
import com.nancheung.plugins.jetbrains.legadoreader.event.EventPublisher;
import com.nancheung.plugins.jetbrains.legadoreader.event.SettingsChangedEvent;
import com.nancheung.plugins.jetbrains.legadoreader.storage.PluginSettingsStorage;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

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

        // 比较 UI 当前值与存储值是否不同
        boolean fontColorModified = state.textBodyFontColorRgb == null ||
                state.textBodyFontColorRgb != ui.getFontColorLabel().getBackground().getRGB();

        boolean fontSizeModified = state.textBodyFontSize == null ||
                !state.textBodyFontSize.equals(ui.getFontSizeSpinner().getValue());

        String currentCustomParam = ui.getApiCustomParamTextArea().getText();
        String storedCustomParam = state.apiCustomParam == null ? "" : state.apiCustomParam;
        boolean customParamModified = !currentCustomParam.equals(storedCustomParam);

        boolean errorLogModified = Boolean.TRUE.equals(state.enableErrorLog) != ui.getEnableErrorLogCheckBox().isSelected();

        boolean inLineModelModified = Boolean.TRUE.equals(state.enableShowBodyInLine) != ui.getEnableInLineModelCheckBox().isSelected();

        // 新增：字体名称比较
        String currentFontFamily = (String) ui.getFontFamilyComboBox().getSelectedItem();
        String storedFontFamily = state.textBodyFontFamily;
        if (storedFontFamily == null || storedFontFamily.isEmpty()) {
            try {
                storedFontFamily = com.intellij.openapi.editor.colors.EditorColorsManager.getInstance()
                        .getGlobalScheme()
                        .getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
                        .getFamily();
            } catch (Exception e) {
                storedFontFamily = new JLabel().getFont().getFamily();
            }
        }
        boolean fontFamilyModified = !currentFontFamily.equals(storedFontFamily);

        // 新增：行高比较（注意精度问题）
        double currentLineHeight = (double) ui.getLineHeightSpinner().getValue();
        double storedLineHeight = state.textBodyLineHeight != null ? state.textBodyLineHeight : 1.5;
        boolean lineHeightModified = Math.abs(currentLineHeight - storedLineHeight) > 0.001;

        return fontColorModified || fontSizeModified || customParamModified ||
                errorLogModified || inLineModelModified || fontFamilyModified || lineHeightModified;
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
        Color fontColor = storage.getTextBodyFontColor();
        Font font = storage.getTextBodyFont();
        double lineHeight = storage.getTextBodyLineHeight();

        EventPublisher.getInstance().publish(
                SettingsChangedEvent.fontSettings(fontColor, font, lineHeight)
        );
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

            // 读取已有配置
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

        state.textBodyFontColorRgb = ui.getFontColorLabel().getBackground().getRGB();
        state.textBodyFontSize = (int) ui.getFontSizeSpinner().getValue();
        state.apiCustomParam = ui.getApiCustomParamTextArea().getText();
        state.enableErrorLog = ui.getEnableErrorLogCheckBox().isSelected();
        state.enableShowBodyInLine = ui.getEnableInLineModelCheckBox().isSelected();

        // 新增：保存字体名称和行高
        state.textBodyFontFamily = (String) ui.getFontFamilyComboBox().getSelectedItem();
        state.textBodyLineHeight = (double) ui.getLineHeightSpinner().getValue();
    }
}

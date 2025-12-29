package com.nancheung.plugins.jetbrains.legadoreader.presentation.settings;

import com.intellij.openapi.options.ConfigurationException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

        List<PluginSettingsStorage.CustomParam> currentParams = ui.getCustomParams();
        boolean customParamModified = !currentParams.equals(state.apiCustomParams);

        boolean errorLogModified = Boolean.TRUE.equals(state.enableErrorLog) != ui.getEnableErrorLogCheckBox().isSelected();

        boolean inLineModelModified = Boolean.TRUE.equals(state.enableShowBodyInLine) != ui.getEnableInLineModelCheckBox().isSelected();

        double currentLineHeight = (double) ui.getLineHeightSpinner().getValue();
        boolean lineHeightModified = Math.abs(currentLineHeight - state.textBodyLineHeight) > 0.001;

        return fontColorModified || fontModified || customParamModified ||
                errorLogModified || inLineModelModified || lineHeightModified;
    }

    @Override
    public void reset() {
        if (settingsPanel != null) {
            settingsPanel.readSettings(PluginSettingsStorage.getInstance().getState());
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        SettingsPanel ui = instance();

        // 1. UI层验证检查（快速失败）
        if (ui.getCustomParamPanel().hasValidationErrors()) {
            throw new ConfigurationException(
                "请修正自定义API请求头中的错误后再保存",
                "自定义API请求头配置错误"
            );
        }

        // 2. 最终验证（保险机制）
        List<PluginSettingsStorage.CustomParam> params = ui.getCustomParams();
        Set<String> paramNames = new HashSet<>();
        for (PluginSettingsStorage.CustomParam param : params) {
            String name = param.name.trim();

            // 验证1：空检查
            if (name.isEmpty()) {
                throw new ConfigurationException(
                    "参数名不能为空",
                    "自定义API请求头配置错误"
                );
            }

            // 验证2：重复检查
            if (!paramNames.add(name)) {
                throw new ConfigurationException(
                    "自定义参数名重复: " + name,
                    "自定义API请求头配置错误"
                );
            }
        }

        // 3. 保存设置
        saveSettings();

        // 4. 发布设置变更事件，通知 UI 更新
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
        PluginSettingsStorage.State state = PluginSettingsStorage.getInstance().getState();
        if (state == null) {
            return;
        }

        state.apiCustomParams = ui.getCustomParams();

        Color foreground = ui.getFontPreviewPane().getForeground();
        state.textBodyFontColor = new JBColor(foreground,foreground);
        state.textBodyFont = ui.getFontPreviewPane().getFont();
        state.textBodyLineHeight = (double) ui.getLineHeightSpinner().getValue();

        state.enableErrorLog = ui.getEnableErrorLogCheckBox().isSelected();
        state.enableShowBodyInLine = ui.getEnableInLineModelCheckBox().isSelected();
    }
}

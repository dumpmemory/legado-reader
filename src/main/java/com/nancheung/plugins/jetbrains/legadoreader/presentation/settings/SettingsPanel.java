package com.nancheung.plugins.jetbrains.legadoreader.presentation.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColorPicker;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.nancheung.plugins.jetbrains.legadoreader.storage.PluginSettingsStorage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
public class SettingsPanel {

    // ==================== 常量 ====================
    private static final String FONT_PREVIEW_TEXT = """
            字体预览 Font Preview
            
            Designed to provide inspiration and improve productivity during the coding process.
            
            Legado Reader是 开源阅读APP 的Jetbrains IDE插件版，旨在随时随地在IDE中进行阅读，为编码过程带来灵感和效率的提升。
            """;

    // ==================== 根面板 ====================
    private final JBPanel<?> rootPanel;

    @Getter
    private CustomParamPanel customParamPanel;
    private JBCheckBox enableErrorLogCheckBox;
    private JBCheckBox enableInLineModelCheckBox;

    // ==================== 阅读界面设置组件 ====================
    private JButton fontColorButton;
    private ComboBox<String> fontNameComboBox;
    private JSpinner fontSizeSpinner;
    private JSpinner lineHeightSpinner;
    private JTextPane fontPreviewPane;

    public SettingsPanel() {
        // 1. 创建所有组件
        createComponents();

        // 2. 构建布局
        rootPanel = createRootPanel();

        // 3. 绑定事件监听器
        bindEventListeners();
    }

    // ==================== 组件创建方法 ====================

    private void createComponents() {
        createGeneralSettingsComponents();
        createReadingInterfaceComponents();
    }

    private void createGeneralSettingsComponents() {
        customParamPanel = new CustomParamPanel();

        enableErrorLogCheckBox = new JBCheckBox("开启异常日志");
        enableErrorLogCheckBox.setToolTipText("报错时给出报错日志");

        enableInLineModelCheckBox = new JBCheckBox("是否开启行内阅读模式");
        enableInLineModelCheckBox.setToolTipText("在代码行后显示章节内容");
    }

    private void createReadingInterfaceComponents() {
        // 字体颜色按钮（仅作为触发器，不显示颜色）
        fontColorButton = new JButton("选择颜色");
        fontColorButton.setPreferredSize(JBUI.size(120, 25));

        // 字体选择下拉框
        fontNameComboBox = createFontNameComboBox();

        // 字体大小 Spinner
        fontSizeSpinner = createFontSizeSpinner();

        // 行高 Spinner
        lineHeightSpinner = createLineHeightSpinner();

        // 字体预览面板
        fontPreviewPane = createFontPreviewPane();
    }

    @NotNull
    private ComboBox<String> createFontNameComboBox() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = Arrays.stream(ge.getAllFonts()).map(Font::getFontName).toArray(String[]::new);
        Arrays.sort(fontNames);

        ComboBox<String> comboBox = new ComboBox<>(fontNames);
        Dimension size = JBUI.size(200, 25);
        comboBox.setPreferredSize(size);
        comboBox.setMinimumSize(size);

        // 展示字体自身样式
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String fontName) {
                    setFont(new Font(fontName, Font.PLAIN, getFont().getSize()));
                }
                return this;
            }
        });

        return comboBox;
    }

    @NotNull
    private JSpinner createFontSizeSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        Dimension size = JBUI.size(80, 25);
        spinner.setPreferredSize(size);
        spinner.setMinimumSize(size);

        configureSpinnerFormatter(spinner);

        return spinner;
    }

    @NotNull
    private JSpinner createLineHeightSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(1.5, 0.5, 3.0, 0.1));
        Dimension size = JBUI.size(80, 25);
        spinner.setPreferredSize(size);
        spinner.setMinimumSize(size);

        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.0");
        spinner.setEditor(editor);

        configureSpinnerFormatter(spinner);

        return spinner;
    }

    private void configureSpinnerFormatter(JSpinner spinner) {
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        if (editor.getTextField().getFormatter() instanceof DefaultFormatter df) {
            df.setAllowsInvalid(false);
            df.setCommitsOnValidEdit(true);
        }
    }

    @NotNull
    private JTextPane createFontPreviewPane() {
        JTextPane pane = new JTextPane();
        pane.setText(FONT_PREVIEW_TEXT);
        pane.setEditable(false);
        return pane;
    }

    // ==================== 布局构建方法 ====================

    @NotNull
    private JBPanel<?> createRootPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));

        // 内容面板（垂直排列三个设置组）
        JBPanel<?> contentPanel = new JBPanel<>();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // 1. 自定义参数面板（最上方）
        contentPanel.add(customParamPanel);
        contentPanel.add(Box.createVerticalStrut(JBUI.scale(15)));

        // 2. 常规设置面板
        JPanel generalPanel = createGeneralSettingsPanel();
        contentPanel.add(generalPanel);
        contentPanel.add(Box.createVerticalStrut(JBUI.scale(10)));

        // 3. 阅读界面设置面板
        JPanel readingPanel = createReadingInterfacePanel();
        contentPanel.add(readingPanel);

        // 填充剩余空间
        contentPanel.add(Box.createVerticalGlue());

        panel.add(new JBScrollPane(contentPanel), BorderLayout.CENTER);

        return panel;
    }

    @NotNull
    private JPanel createGeneralSettingsPanel() {
        JPanel panel = FormBuilder.createFormBuilder()
                .addComponent(enableErrorLogCheckBox, JBUI.scale(5))
                .addComponent(enableInLineModelCheckBox, JBUI.scale(5))
                .getPanel();

        panel.setBorder(IdeBorderFactory.createTitledBorder("常规设置"));

        return panel;
    }

    @NotNull
    private JPanel createReadingInterfacePanel() {
        // 左侧表单
        JPanel formPanel = FormBuilder.createFormBuilder()
                .setVerticalGap(JBUI.scale(5))
                .addLabeledComponent(new JBLabel("正文字体颜色:"), fontColorButton, false)
                .addLabeledComponent(new JBLabel("正文字体:"), fontNameComboBox, false)
                .addLabeledComponent(new JBLabel("正文字体大小 (0为默认):"), fontSizeSpinner, false)
                .addLabeledComponent(new JBLabel("正文字体行高:"), lineHeightSpinner, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        // 右侧预览面板
        JBScrollPane previewScrollPane = new JBScrollPane(fontPreviewPane);
        previewScrollPane.setPreferredSize(JBUI.size(300, 150));
        previewScrollPane.setBorder(IdeBorderFactory.createTitledBorder("字体效果预览"));

        // 组合左右面板
        JBPanel<?> contentPanel = new JBPanel<>(new BorderLayout(JBUI.scale(10), 0));
        contentPanel.add(formPanel, BorderLayout.WEST);
        contentPanel.add(previewScrollPane, BorderLayout.CENTER);

        contentPanel.setBorder(IdeBorderFactory.createTitledBorder("阅读界面设置"));

        return contentPanel;
    }

    // ==================== 事件监听方法 ====================

    private void bindEventListeners() {
        // 颜色选择器点击事件
        fontColorButton.addActionListener(e -> openColorPicker());

        // 预览更新监听器
        fontNameComboBox.addActionListener(e -> updateFontPreview());
        fontSizeSpinner.addChangeListener(e -> updateFontPreview());
        lineHeightSpinner.addChangeListener(e -> updateFontPreview());
    }

    /**
     * 打开颜色选择器
     */
    private void openColorPicker() {
        Color currentColor = fontPreviewPane.getForeground();
        Color newColor = ColorPicker.showDialog(
                rootPanel,
                "选择字体颜色",
                currentColor,
                true,
                null,
                true
        );
        if (newColor != null) {
            fontPreviewPane.setForeground(newColor);
        }
    }

    private void updateFontPreview() {
        try {
            String fontName = (String) fontNameComboBox.getSelectedItem();

            int fontSize = (int) fontSizeSpinner.getValue();
            if (fontSize == 0) {
                fontSize = fontSizeSpinner.getFont().getSize();
            }

            double lineHeight = (double) lineHeightSpinner.getValue();

            updateFontPreview(new Font(fontName, Font.PLAIN, fontSize),
                    new JBColor(fontPreviewPane.getForeground(), fontPreviewPane.getForeground()),
                    lineHeight);
        } catch (Exception e) {
            log.error("更新字体预览时出错", e);
        }
    }

    private void updateFontPreview(PluginSettingsStorage.State state) {
        updateFontPreview(state.textBodyFont, state.textBodyFontColor, state.textBodyLineHeight);
    }

    private void updateFontPreview(Font font, JBColor textBodyFontColor, Double lineHeight) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Optional.ofNullable(font).ifPresent(fontPreviewPane::setFont);
            Optional.ofNullable(textBodyFontColor).ifPresent(fontPreviewPane::setForeground);
            Optional.ofNullable(lineHeight).ifPresent(this::applyLineHeight);
        });
    }

    private void applyLineHeight(double lineHeight) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        float lineSpacing = (float) (lineHeight - 1.0);
        StyleConstants.setLineSpacing(attrs, lineSpacing);

        StyledDocument doc = fontPreviewPane.getStyledDocument();
        doc.setParagraphAttributes(0, doc.getLength(), attrs, false);
    }

    // ==================== 公共接口方法 ====================

    public JComponent getComponent() {
        return rootPanel;
    }

    public void readSettings(PluginSettingsStorage.State state) {
        if (state == null) {
            return;
        }

        customParamPanel.setCustomParams(state.apiCustomParams);

        fontSizeSpinner.setValue(state.textBodyFont.getSize());
        fontNameComboBox.setSelectedItem(state.textBodyFont.getFontName());

        lineHeightSpinner.setValue(state.textBodyLineHeight);

        enableErrorLogCheckBox.setSelected(state.enableErrorLog);
        enableInLineModelCheckBox.setSelected(state.enableShowBodyInLine);

        updateFontPreview(state);
    }

    public List<PluginSettingsStorage.CustomParam> getCustomParams() {
        return customParamPanel.getCustomParams();
    }
}
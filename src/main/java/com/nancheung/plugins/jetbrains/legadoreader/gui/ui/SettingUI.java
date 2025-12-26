package com.nancheung.plugins.jetbrains.legadoreader.gui.ui;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColorPicker;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.nancheung.plugins.jetbrains.legadoreader.storage.PluginSettingsStorage;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

@Getter
public class SettingUI {

    // ==================== 常量 ====================
    private static final String FONT_PREVIEW_TEXT = """
            字体预览 Font Preview
            
            Designed to provide inspiration and improve productivity during the coding process.
            
            Legado Reader是 开源阅读APP 的Jetbrains IDE插件版，旨在随时随地在IDE中进行阅读，为编码过程带来灵感和效率的提升。
            """;

    // ==================== 根面板 ====================
    private final JBPanel<?> rootPanel;

    // ==================== 常规设置组件 ====================
    private JBTextArea apiCustomParamTextArea;
    private JBCheckBox enableErrorLogCheckBox;
    private JBCheckBox enableInLineModelCheckBox;

    // ==================== 阅读界面设置组件 ====================
    private JBLabel fontColorLabel;
    private ComboBox<String> fontFamilyComboBox;
    private JSpinner fontSizeSpinner;
    private JSpinner lineHeightSpinner;
    private JTextPane fontPreviewPane;

    // ==================== 构造函数 ====================
    public SettingUI() {
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
        // API 自定义参数文本区
        apiCustomParamTextArea = new JBTextArea(4, 30);
        apiCustomParamTextArea.setLineWrap(true);
        apiCustomParamTextArea.setWrapStyleWord(true);

        // 复选框
        enableErrorLogCheckBox = new JBCheckBox("开启异常日志");
        enableErrorLogCheckBox.setToolTipText("报错时给出报错日志");

        enableInLineModelCheckBox = new JBCheckBox("是否开启行内阅读模式");
    }

    private void createReadingInterfaceComponents() {
        // 字体颜色标签（显示为颜色块）
        fontColorLabel = new JBLabel(" ");
        fontColorLabel.setOpaque(true);
        fontColorLabel.setBackground(JBColor.BLACK);
        fontColorLabel.setPreferredSize(JBUI.size(60, 25));
        fontColorLabel.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        fontColorLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 字体选择下拉框
        fontFamilyComboBox = createFontFamilyComboBox();

        // 字体大小 Spinner
        fontSizeSpinner = createFontSizeSpinner();

        // 行高 Spinner
        lineHeightSpinner = createLineHeightSpinner();

        // 字体预览面板
        fontPreviewPane = createFontPreviewPane();
    }

    @NotNull
    private ComboBox<String> createFontFamilyComboBox() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontFamilies = ge.getAvailableFontFamilyNames();
        Arrays.sort(fontFamilies);

        ComboBox<String> comboBox = new ComboBox<>(fontFamilies);
        Dimension size = JBUI.size(200, 25);
        comboBox.setPreferredSize(size);
        comboBox.setMinimumSize(size);

        // 使用字体自身渲染
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String fontFamily) {
                    setFont(new Font(fontFamily, Font.PLAIN, getFont().getSize()));
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

        // 内容面板（垂直排列两个设置组）
        JBPanel<?> contentPanel = new JBPanel<>();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // 常规设置面板
        JPanel generalPanel = createGeneralSettingsPanel();
        contentPanel.add(generalPanel);
        contentPanel.add(Box.createVerticalStrut(JBUI.scale(10)));

        // 阅读界面设置面板
        JPanel readingPanel = createReadingInterfacePanel();
        contentPanel.add(readingPanel);

        // 填充剩余空间
        contentPanel.add(Box.createVerticalGlue());

        panel.add(new JBScrollPane(contentPanel), BorderLayout.CENTER);

        return panel;
    }

    @NotNull
    private JPanel createGeneralSettingsPanel() {
        // API 参数区域
        JBScrollPane apiParamScrollPane = new JBScrollPane(apiCustomParamTextArea);
        apiParamScrollPane.setPreferredSize(JBUI.size(300, 80));

        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(
                        new JBLabel("自定义请求参数 (key:@value 换行区分)"),
                        apiParamScrollPane,
                        JBUI.scale(5),
                        true
                )
                .addComponent(enableErrorLogCheckBox, JBUI.scale(10))
                .addComponent(enableInLineModelCheckBox, JBUI.scale(5))
                .getPanel();

        panel.setBorder(IdeBorderFactory.createTitledBorder("常规设置"));

        return panel;
    }

    @NotNull
    private JPanel createReadingInterfacePanel() {
        // 左侧表单 - 使用 GridBagLayout 手动布局
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(fontColorLabel, BorderLayout.WEST);

        JPanel formPanel = FormBuilder.createFormBuilder()
                .setVerticalGap(JBUI.scale(5))
                .addLabeledComponent(new JBLabel("正文字体颜色:"), panel, false)
                .addLabeledComponent(new JBLabel("正文字体:"), fontFamilyComboBox, false)
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
        fontColorLabel.addMouseListener(createColorPickerListener());

        // 预览更新监听器
        fontColorLabel.addPropertyChangeListener("background", evt -> updateFontPreview());
        fontFamilyComboBox.addActionListener(e -> updateFontPreview());
        fontSizeSpinner.addChangeListener(e -> updateFontPreview());
        lineHeightSpinner.addChangeListener(e -> updateFontPreview());
    }

    @NotNull
    private MouseAdapter createColorPickerListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Color currentColor = fontColorLabel.getBackground();
                Color newColor = ColorPicker.showDialog(
                        rootPanel,
                        "选择字体颜色",
                        currentColor,
                        true,
                        null,
                        true
                );
                if (newColor != null) {
                    fontColorLabel.setBackground(newColor);
                }
            }
        };
    }

    private void updateFontPreview() {
        SwingUtilities.invokeLater(() -> {
            try {
                Color fontColor = fontColorLabel.getBackground();
                String fontFamily = (String) fontFamilyComboBox.getSelectedItem();
                int fontSize = (int) fontSizeSpinner.getValue();
                double lineHeight = (double) lineHeightSpinner.getValue();

                if (fontSize == 0) {
                    fontSize = new JLabel().getFont().getSize();
                }

                fontPreviewPane.setForeground(fontColor);
                fontPreviewPane.setFont(new Font(fontFamily, Font.PLAIN, fontSize));

                applyLineHeight(lineHeight);
            } catch (Exception ignored) {
                // 忽略预览更新异常
            }
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

        // 字体颜色
        if (state.textBodyFontColorRgb != null) {
            fontColorLabel.setBackground(
                    new JBColor(new Color(state.textBodyFontColorRgb), new Color(state.textBodyFontColorRgb))
            );
        }

        // 字体大小
        if (state.textBodyFontSize != null && state.textBodyFontSize >= 0) {
            fontSizeSpinner.setValue(state.textBodyFontSize);
        }

        // API 自定义参数
        if (state.apiCustomParam != null && !state.apiCustomParam.isEmpty()) {
            apiCustomParamTextArea.setText(state.apiCustomParam);
        }

        // 复选框
        enableErrorLogCheckBox.setSelected(Boolean.TRUE.equals(state.enableErrorLog));
        enableInLineModelCheckBox.setSelected(Boolean.TRUE.equals(state.enableShowBodyInLine));

        // 字体名称
        String fontFamily = state.textBodyFontFamily;
        if (fontFamily == null || fontFamily.isEmpty()) {
            fontFamily = getDefaultFontFamily();
        }
        fontFamilyComboBox.setSelectedItem(fontFamily);

        // 行高
        if (state.textBodyLineHeight != null) {
            lineHeightSpinner.setValue(state.textBodyLineHeight);
        }

        // 更新预览
        updateFontPreview();
    }

    @NotNull
    private String getDefaultFontFamily() {
        try {
            return EditorColorsManager.getInstance()
                    .getGlobalScheme()
                    .getFont(EditorFontType.PLAIN)
                    .getFamily();
        } catch (Exception e) {
            return new JLabel().getFont().getFamily();
        }
    }
}

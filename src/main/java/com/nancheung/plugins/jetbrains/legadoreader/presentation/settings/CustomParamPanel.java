package com.nancheung.plugins.jetbrains.legadoreader.presentation.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import com.nancheung.plugins.jetbrains.legadoreader.storage.PluginSettingsStorage.CustomParam;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * 自定义参数面板组件
 *
 * <p>职责：
 * <ul>
 *   <li>UI管理：创建和布局表格、工具栏、标题</li>
 *   <li>实时验证：输入时验证参数名（空检查、重复检查）</li>
 *   <li>视觉反馈：使用 JComponent.outline 显示红色边框</li>
 *   <li>数据接口：提供 getCustomParams()、setCustomParams()、hasValidationErrors()</li>
 * </ul>
 *
 * @author NanCheung
 */
@Slf4j
public class CustomParamPanel extends JBPanel<CustomParamPanel> {

    // ==================== 验证错误枚举 ====================
    private enum ValidationError {
        EMPTY_NAME,      // 参数名为空
        DUPLICATE_NAME   // 参数名重复
    }

    // ==================== UI 组件 ====================
    @Getter
    private final TableView<CustomParam> table;
    private final ListTableModel<CustomParam> tableModel;

    // ==================== 验证状态 ====================
    private final Map<Integer, Set<ValidationError>> validationErrors = new HashMap<>();

    // ==================== 构造函数 ====================
    public CustomParamPanel() {
        super(new BorderLayout(0, JBUI.scale(5)));

        // 创建表格模型和表格
        ColumnInfo<CustomParam, String>[] columns = new ColumnInfo[]{
                createNameColumn(),
                createValueColumn()
        };
        this.tableModel = new ListTableModel<>(columns, new ArrayList<>());
        this.table = new TableView<>(tableModel);
        this.table.setPreferredScrollableViewportSize(JBUI.size(400, 120));

        // 初始化 UI
        initializeUI();
    }

    // ==================== UI 初始化 ====================

    private void initializeUI() {
        // 顶部标题（左对齐）
        JBLabel titleLabel = new JBLabel("自定义API请求头");
        titleLabel.setBorder(JBUI.Borders.emptyBottom(5));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(titleLabel, BorderLayout.NORTH);

        // 中央表格
        JPanel toolbarPanel = createToolbarDecorator();
        add(toolbarPanel, BorderLayout.CENTER);
    }

    // ==================== 列定义 ====================

    private ColumnInfo<CustomParam, String> createNameColumn() {
        return new ColumnInfo<>("参数名") {
            @Override
            public String valueOf(CustomParam item) {
                return item.name;
            }

            @Override
            public void setValue(CustomParam item, String value) {
                item.name = value;
                // 同步执行验证，确保重绘前验证状态已更新
                validateAllRows();
            }

            @Override
            public boolean isCellEditable(CustomParam item) {
                return true;
            }

            @Override
            public TableCellEditor getEditor(CustomParam item) {
                return createValidatingEditor();
            }

            @Override
            public TableCellRenderer getRenderer(CustomParam item) {
                return createCustomRenderer();
            }
        };
    }

    private ColumnInfo<CustomParam, String> createValueColumn() {
        return new ColumnInfo<CustomParam, String>("参数值") {
            @Override
            public String valueOf(CustomParam item) {
                return item.value;
            }

            @Override
            public void setValue(CustomParam item, String value) {
                item.value = value;
            }

            @Override
            public boolean isCellEditable(CustomParam item) {
                return true;
            }
        };
    }

    // ==================== 验证编辑器 ====================

    private TableCellEditor createValidatingEditor() {
        JBTextField textField = new JBTextField();

        // 设置紧凑样式（配合 outline 使用）
        textField.putClientProperty("JTextField.variant", "compact");

        // 添加文档监听器
        textField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                int row = table.getEditingRow();
                if (row < 0) {
                    return; // 不在编辑状态
                }

                String text = textField.getText();

                // 验证1：空检查
                boolean isEmpty = text.trim().isEmpty();

                // 验证2：重复检查
                boolean isDuplicate = !isEmpty && hasDuplicateName(row, text);

                // 更新验证状态
                updateValidationState(row, isEmpty, isDuplicate);

                // 更新UI
                textField.putClientProperty("JComponent.outline",
                    (isEmpty || isDuplicate) ? "error" : null);
            }
        });

        return new DefaultCellEditor(textField);
    }

    // ==================== 自定义渲染器 ====================

    private TableCellRenderer createCustomRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                          boolean isSelected, boolean hasFocus,
                                                          int row, int column) {
                Component component = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
                );

                // 在非编辑状态下显示错误（改变文本颜色和背景）
                if (component instanceof JLabel label && row >= 0 && row < tableModel.getRowCount()) {
                    // 直接使用 row 参数检查验证错误
                    boolean hasError = validationErrors.containsKey(row);

                    if (hasError) {
                        // 有错误时显示红色文本和浅红色背景
                        label.setForeground(com.intellij.ui.JBColor.RED);
                        label.setBackground(new com.intellij.ui.JBColor(
                            new java.awt.Color(255, 240, 240),  // Light模式：浅红色
                            new java.awt.Color(90, 50, 50)      // Dark模式：深红色
                        ));
                        label.setOpaque(true);  // 必须设置为true，背景色才会显示
                    } else {
                        // 无错误时恢复默认颜色
                        label.setForeground(null);
                        label.setBackground(null);
                        label.setOpaque(false);
                    }
                }

                return component;
            }
        };
    }

    // ==================== 验证逻辑 ====================

    private void updateValidationState(int row, boolean isEmpty, boolean isDuplicate) {
        Set<ValidationError> errors = validationErrors.computeIfAbsent(row, k -> new HashSet<>());
        errors.clear();

        if (isEmpty) {
            errors.add(ValidationError.EMPTY_NAME);
        }
        if (isDuplicate) {
            errors.add(ValidationError.DUPLICATE_NAME);
        }

        if (errors.isEmpty()) {
            validationErrors.remove(row);
        }
    }

    private boolean hasDuplicateName(int currentRow, String name) {
        if (name.trim().isEmpty()) {
            return false; // 空名称不参与重复检查
        }

        List<CustomParam> params = tableModel.getItems();
        for (int i = 0; i < params.size(); i++) {
            if (i != currentRow && name.equals(params.get(i).name)) {
                return true;
            }
        }
        return false;
    }

    private void validateAllRows() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            CustomParam param = tableModel.getItem(i);
            boolean isEmpty = param.name.trim().isEmpty();
            boolean isDuplicate = !isEmpty && hasDuplicateName(i, param.name);
            updateValidationState(i, isEmpty, isDuplicate);
        }
    }

    // ==================== ToolbarDecorator ====================

    private JPanel createToolbarDecorator() {
        return ToolbarDecorator.createDecorator(table)
                .setAddAction(button -> {
                    tableModel.addRow(new CustomParam("", ""));
                    int lastRow = tableModel.getRowCount() - 1;
                    table.setRowSelectionInterval(lastRow, lastRow);
                    // 自动开始编辑参数名列
                    table.editCellAt(lastRow, 0);
                    Component editorComponent = table.getEditorComponent();
                    if (editorComponent != null) {
                        editorComponent.requestFocusInWindow();
                    }
                })
                .setRemoveAction(button -> {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow >= 0) {
                        tableModel.removeRow(selectedRow);
                        validationErrors.remove(selectedRow);
                        reindexValidationErrors(selectedRow);
                        // 触发全局验证（删除可能解决重复问题）
                        validateAllRows();
                    }
                })
                .setMoveUpAction(button -> {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow > 0) {
                        tableModel.exchangeRows(selectedRow, selectedRow - 1);
                        swapValidationErrors(selectedRow, selectedRow - 1);
                        table.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                    }
                })
                .setMoveDownAction(button -> {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow < tableModel.getRowCount() - 1) {
                        tableModel.exchangeRows(selectedRow, selectedRow + 1);
                        swapValidationErrors(selectedRow, selectedRow + 1);
                        table.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
                    }
                })
                .createPanel();
    }

    // ==================== 验证状态同步 ====================

    /**
     * 删除行后重新索引验证错误
     */
    private void reindexValidationErrors(int deletedRow) {
        Map<Integer, Set<ValidationError>> newErrors = new HashMap<>();
        for (Map.Entry<Integer, Set<ValidationError>> entry : validationErrors.entrySet()) {
            int row = entry.getKey();
            if (row < deletedRow) {
                newErrors.put(row, entry.getValue());
            } else if (row > deletedRow) {
                newErrors.put(row - 1, entry.getValue());
            }
            // row == deletedRow 的错误直接丢弃
        }
        validationErrors.clear();
        validationErrors.putAll(newErrors);
    }

    /**
     * 交换两行的验证状态
     */
    private void swapValidationErrors(int row1, int row2) {
        Set<ValidationError> errors1 = validationErrors.get(row1);
        Set<ValidationError> errors2 = validationErrors.get(row2);

        if (errors1 != null) {
            validationErrors.put(row2, errors1);
        } else {
            validationErrors.remove(row2);
        }

        if (errors2 != null) {
            validationErrors.put(row1, errors2);
        } else {
            validationErrors.remove(row1);
        }
    }

    // ==================== 公共接口 ====================

    /**
     * 获取自定义参数列表
     */
    public List<CustomParam> getCustomParams() {
        return new ArrayList<>(tableModel.getItems());
    }

    /**
     * 设置自定义参数列表（创建副本，避免直接修改原始数据）
     */
    public void setCustomParams(List<CustomParam> params) {
        // 创建副本，避免直接修改原始存储的数据
        // 注意：必须使用可变列表（ArrayList），toList() 返回的是不可变列表
        List<CustomParam> paramsCopy = params.stream()
                .map(p -> new CustomParam(p.name, p.value))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        tableModel.setItems(paramsCopy);
        validationErrors.clear();
        validateAllRows();
    }

    /**
     * 是否存在验证错误
     */
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }
}

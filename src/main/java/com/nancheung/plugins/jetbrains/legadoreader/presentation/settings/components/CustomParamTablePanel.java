package com.nancheung.plugins.jetbrains.legadoreader.presentation.settings.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import com.nancheung.plugins.jetbrains.legadoreader.presentation.settings.SettingsViewModel;
import com.nancheung.plugins.jetbrains.legadoreader.presentation.settings.validation.ValidationError;
import com.nancheung.plugins.jetbrains.legadoreader.presentation.settings.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 自定义参数表格面板
 * 使用 IntelliJ TableView + ToolbarDecorator
 * 验证状态由外部传入，不自行管理
 */
@Slf4j
public final class CustomParamTablePanel extends JBPanel<CustomParamTablePanel> {

    // 预定义的错误颜色（避免每次渲染创建新对象）
    private static final JBColor ERROR_FOREGROUND = JBColor.RED;
    private static final JBColor ERROR_BACKGROUND = new JBColor(
        new Color(255, 240, 240),  // 浅色主题
        new Color(90, 50, 50)      // 深色主题
    );
    // 选中时的错误边框
    private static final Border ERROR_BORDER = BorderFactory.createLineBorder(JBColor.RED, 1);

    private final TableView<MutableParamEntry> table;
    private final ListTableModel<MutableParamEntry> tableModel;
    private final List<Consumer<List<SettingsViewModel.CustomParamEntry>>> changeListeners = new ArrayList<>();

    // 当前验证结果缓存
    private ValidationResult currentValidation = ValidationResult.valid();

    /**
     * 可变参数条目（内部使用，用于表格编辑）
     */
    private static class MutableParamEntry {
        String name;
        String value;

        MutableParamEntry(String name, String value) {
            this.name = name;
            this.value = value;
        }

        SettingsViewModel.CustomParamEntry toImmutable() {
            return new SettingsViewModel.CustomParamEntry(name, value);
        }
    }

    public CustomParamTablePanel() {
        super(new BorderLayout(0, JBUI.scale(5)));

        // 创建表格模型
        tableModel = createTableModel();
        table = new TableView<>(tableModel);
        table.setPreferredScrollableViewportSize(JBUI.size(400, 120));

        // 标题
        JBLabel titleLabel = new JBLabel("自定义API请求头");
        titleLabel.setBorder(JBUI.Borders.emptyBottom(5));
        add(titleLabel, BorderLayout.NORTH);

        // 带工具栏的表格
        add(createToolbarDecorator(), BorderLayout.CENTER);
    }

    private ListTableModel<MutableParamEntry> createTableModel() {
        ColumnInfo<MutableParamEntry, String>[] columns = new ColumnInfo[]{
            createNameColumn(),
            createValueColumn()
        };
        return new ListTableModel<>(columns, new ArrayList<>());
    }

    private ColumnInfo<MutableParamEntry, String> createNameColumn() {
        return new ColumnInfo<MutableParamEntry, String>("参数名") {
            @Override
            public String valueOf(MutableParamEntry item) {
                return item.name;
            }

            @Override
            public void setValue(MutableParamEntry item, String value) {
                item.name = value;
                notifyChange();
            }

            @Override
            public boolean isCellEditable(MutableParamEntry item) {
                return true;
            }

            @Override
            public TableCellRenderer getRenderer(MutableParamEntry item) {
                return new ErrorHighlightRenderer();
            }
        };
    }

    private ColumnInfo<MutableParamEntry, String> createValueColumn() {
        return new ColumnInfo<MutableParamEntry, String>("参数值") {
            @Override
            public String valueOf(MutableParamEntry item) {
                return item.value;
            }

            @Override
            public void setValue(MutableParamEntry item, String value) {
                item.value = value;
                notifyChange();
            }

            @Override
            public boolean isCellEditable(MutableParamEntry item) {
                return true;
            }
        };
    }

    /**
     * 错误高亮渲染器
     * 使用预定义颜色常量，避免重复创建
     */
    private class ErrorHighlightRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

            if (c instanceof JLabel label) {
                boolean hasError = currentValidation.hasErrorAt(row);
                if (hasError) {
                    // 设置 tooltip 显示错误信息
                    String errorMsg = currentValidation.errorsAt(row).stream()
                        .map(ValidationError::message)
                        .collect(Collectors.joining("\n"));
                    label.setToolTipText(errorMsg);

                    if (isSelected) {
                        // 选中时：保留选中样式，添加红色边框
                        label.setBorder(ERROR_BORDER);
                    } else {
                        // 未选中时：使用错误颜色
                        label.setForeground(ERROR_FOREGROUND);
                        label.setBackground(ERROR_BACKGROUND);
                        label.setOpaque(true);
                        label.setBorder(null);
                    }
                } else {
                    label.setForeground(null);
                    label.setBackground(null);
                    label.setOpaque(false);
                    label.setToolTipText(null);
                    label.setBorder(null);
                }
            }
            return c;
        }
    }

    /**
     * 显示验证错误
     * 由外部调用，传入验证结果
     */
    public void showValidationErrors(ValidationResult result) {
        this.currentValidation = result;
        table.repaint();  // 触发重绘
    }

    public void setItems(List<SettingsViewModel.CustomParamEntry> items) {
        List<MutableParamEntry> mutableItems = items.stream()
            .map(e -> new MutableParamEntry(e.name(), e.value()))
            .collect(Collectors.toCollection(ArrayList::new));
        tableModel.setItems(mutableItems);
    }

    public List<SettingsViewModel.CustomParamEntry> getItems() {
        return tableModel.getItems().stream()
            .map(MutableParamEntry::toImmutable)
            .collect(Collectors.toList());
    }

    public void addChangeListener(Consumer<List<SettingsViewModel.CustomParamEntry>> listener) {
        changeListeners.add(listener);
    }

    private void notifyChange() {
        List<SettingsViewModel.CustomParamEntry> items = getItems();
        changeListeners.forEach(l -> l.accept(items));
    }

    private JPanel createToolbarDecorator() {
        return ToolbarDecorator.createDecorator(table)
            .setAddAction(button -> {
                tableModel.addRow(new MutableParamEntry("", ""));
                int lastRow = tableModel.getRowCount() - 1;
                table.setRowSelectionInterval(lastRow, lastRow);
                // 自动开始编辑参数名列
                table.editCellAt(lastRow, 0);
                Component editorComponent = table.getEditorComponent();
                if (editorComponent != null) {
                    editorComponent.requestFocusInWindow();
                }
                notifyChange();
            })
            .setRemoveAction(button -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    tableModel.removeRow(selectedRow);
                    notifyChange();
                }
            })
            .setMoveUpAction(button -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow > 0) {
                    tableModel.exchangeRows(selectedRow, selectedRow - 1);
                    table.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                    notifyChange();
                }
            })
            .setMoveDownAction(button -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow < tableModel.getRowCount() - 1) {
                    tableModel.exchangeRows(selectedRow, selectedRow + 1);
                    table.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
                    notifyChange();
                }
            })
            .createPanel();
    }
}

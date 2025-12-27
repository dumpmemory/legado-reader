package com.nancheung.plugins.jetbrains.legadoreader.presentation.toolwindow.panel;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.nancheung.plugins.jetbrains.legadoreader.api.ApiUtil;
import com.nancheung.plugins.jetbrains.legadoreader.api.dto.BookDTO;
import com.nancheung.plugins.jetbrains.legadoreader.command.Command;
import com.nancheung.plugins.jetbrains.legadoreader.command.CommandBus;
import com.nancheung.plugins.jetbrains.legadoreader.command.CommandType;
import com.nancheung.plugins.jetbrains.legadoreader.command.payload.SelectBookPayload;
import com.nancheung.plugins.jetbrains.legadoreader.storage.AddressHistoryStorage;
import com.nancheung.plugins.jetbrains.legadoreader.storage.PluginSettingsStorage;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 书架面板组件
 * 负责显示书架列表、地址栏和错误提示
 *
 * @author NanCheung
 */
@Slf4j
public class BookshelfPanel extends JBPanel<BookshelfPanel> {

    // ==================== 卡片常量 ====================
    private static final String CARD_CONTENT = "CONTENT";
    private static final String CARD_ERROR = "ERROR";

    // ==================== 错误提示文本 ====================
    private static final String ERROR_MESSAGE = """
            请求内容失败，请检查web服务是否开启、url是否正确、网络是否正常？
            
            小提示：可以在File -> Settings -> Tools -> Legado Reader中进行更多设置哦~
            也可以在 Keymap -> Plugins -> Legado Reader 中查看所有快捷键并进行自定义设置~
            """;

    // ==================== UI 组件 ====================
    private JBTextField addressTextField;
    private ComboBox<String> addressHistoryBox;
    private JButton refreshBookshelfButton;
    private JBTable bookshelfTable;
    private JBPanel<?> bookshelfContentPanel;
    private CardLayout bookshelfContentLayout;

    // ==================== 数据模型（静态，多窗口共享） ====================
    private static final DefaultTableModel BOOK_SHELF_TABLE_MODEL =
            new DefaultTableModel(null, new String[]{"name", "current", "new", "author"}) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    public static final DefaultComboBoxModel<String> ADDRESS_HISTORY_BOX_MODEL = new DefaultComboBoxModel<>();

    // ==================== 书架数据 ====================
    private Map<String, BookDTO> bookshelf;
    private static final BiFunction<String, String, String> BOOK_MAP_KEY_FUNC = (author, name) -> author + "#" + name;

    // ==================== 构造函数 ====================
    public BookshelfPanel() {
        super(new BorderLayout());

        // 1. 创建地址栏组件
        JBPanel<?> addressBar = createAddressBar();
        this.add(addressBar, BorderLayout.NORTH);

        // 2. 中央内容区（使用 CardLayout 切换内容/错误）
        bookshelfContentLayout = new CardLayout();
        bookshelfContentPanel = new JBPanel<>(bookshelfContentLayout);

        // 2.1 内容卡片：书架表格
        bookshelfTable = createBookshelfTable();
        bookshelfContentPanel.add(new JBScrollPane(bookshelfTable), CARD_CONTENT);

        // 2.2 错误卡片：错误提示
        bookshelfContentPanel.add(wrapCentered(createErrorLabel()), CARD_ERROR);

        this.add(bookshelfContentPanel, BorderLayout.CENTER);

        // 3. 绑定事件监听器
        bindEventListeners();

        // 默认显示内容
        showContent();
    }

    // ==================== UI 创建方法 ====================

    /**
     * 创建地址栏
     */
    private JBPanel<?> createAddressBar() {
        JBPanel<?> addressBar = new JBPanel<>();
        addressBar.setLayout(new BoxLayout(addressBar, BoxLayout.X_AXIS));
        addressBar.setBorder(JBUI.Borders.empty(4));

        // 地址输入框
        addressTextField = new JBTextField("127.0.0.1:1122");
        addressTextField.setMinimumSize(JBUI.size(150, -1));
        addressTextField.setPreferredSize(JBUI.size(150, -1));
        addressTextField.setName("addressTextField");

        // 历史记录下拉框
        addressHistoryBox = new ComboBox<>(ADDRESS_HISTORY_BOX_MODEL);
        addressHistoryBox.setName("addressHistoryBox");

        // 刷新按钮
        refreshBookshelfButton = new JButton("刷新");
        refreshBookshelfButton.setName("refreshButton");

        addressBar.add(addressTextField);
        addressBar.add(Box.createHorizontalStrut(JBUI.scale(4)));
        addressBar.add(addressHistoryBox);
        addressBar.add(Box.createHorizontalStrut(JBUI.scale(4)));
        addressBar.add(refreshBookshelfButton);

        return addressBar;
    }

    /**
     * 创建书架表格
     */
    private JBTable createBookshelfTable() {
        JBTable table = new JBTable(BOOK_SHELF_TABLE_MODEL);
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return table;
    }

    /**
     * 创建错误标签
     */
    private JBLabel createErrorLabel() {
        JBLabel label = new JBLabel();
        label.setText("<html><center>" + ERROR_MESSAGE.replace("\n", "<br>") + "</center></html>");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setForeground(JBColor.GRAY);
        return label;
    }

    /**
     * 将组件包装在居中面板中
     */
    private JBPanel<?> wrapCentered(JComponent component) {
        JBPanel<?> wrapper = new JBPanel<>(new GridBagLayout());
        wrapper.add(component);
        return wrapper;
    }

    // ==================== 事件绑定方法 ====================

    /**
     * 绑定事件监听器
     */
    private void bindEventListeners() {
        // 刷新按钮
        refreshBookshelfButton.addActionListener(e -> {
            refreshBookshelfButton.setEnabled(false);
            AddressHistoryStorage.getInstance().addAddress(addressTextField.getText());
            setAddressUI();
            refreshBookshelf(
                    books -> refreshBookshelfButton.setEnabled(true),
                    error -> refreshBookshelfButton.setEnabled(true)
            );
        });

        // 表格点击
        bookshelfTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                handleBookSelection(evt);
            }
        });

        // 历史记录选择
        addressHistoryBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() != null) {
                addressTextField.setText(e.getItem().toString());
            }
        });
    }

    // ==================== 状态切换方法 ====================

    /**
     * 显示书架内容（隐藏错误）
     */
    public void showContent() {
        bookshelfContentLayout.show(bookshelfContentPanel, CARD_CONTENT);
    }

    /**
     * 显示书架错误（隐藏内容）
     */
    public void showError() {
        bookshelfContentLayout.show(bookshelfContentPanel, CARD_ERROR);
    }

    // ==================== 业务逻辑方法 ====================

    /**
     * 刷新书架
     *
     * @param acceptConsumer    成功回调
     * @param throwableConsumer 失败回调
     */
    public void refreshBookshelf(Consumer<List<BookDTO>> acceptConsumer, Consumer<Throwable> throwableConsumer) {
        CompletableFuture.supplyAsync(ApiUtil::getBookshelf)
                .thenAccept(books -> {
                    // 保存书架目录信息
                    this.bookshelf = books.stream()
                            .collect(Collectors.toMap(
                                    book -> BOOK_MAP_KEY_FUNC.apply(book.getAuthor(), book.getName()),
                                    Function.identity()
                            ));
                    // 设置书架目录UI
                    setBookshelfUI(books);

                    acceptConsumer.accept(books);
                }).exceptionally(throwable -> {
                    showError();

                    if (Boolean.TRUE.equals(PluginSettingsStorage.getInstance().getState().enableErrorLog)) {
                        log.error("获取书架列表失败", throwable.getCause());
                    }

                    throwableConsumer.accept(throwable);
                    return null;
                });
    }

    /**
     * 设置书架 UI
     */
    private void setBookshelfUI(List<BookDTO> books) {
        // 清空表格
        BOOK_SHELF_TABLE_MODEL.getDataVector().clear();

        // 添加表格数据
        books.stream().map(book -> {
            Vector<String> bookVector = new Vector<>();
            bookVector.add(book.getName());
            bookVector.add(book.getDurChapterTitle());
            bookVector.add(book.getLatestChapterTitle());
            bookVector.add(book.getAuthor());
            return bookVector;
        }).forEach(BOOK_SHELF_TABLE_MODEL::addRow);

        // 显示内容（隐藏错误）
        showContent();
    }

    /**
     * 处理书籍选择
     */
    private void handleBookSelection(MouseEvent evt) {
        int row = bookshelfTable.rowAtPoint(evt.getPoint());
        int col = bookshelfTable.columnAtPoint(evt.getPoint());

        if (row < 0 || col < 0) {
            return;
        }

        // 获取当前点击的书籍信息
        TableModel model = ((JTable) evt.getSource()).getModel();
        String name = model.getValueAt(row, 0).toString();
        String author = model.getValueAt(row, 3).toString();

        // 获取书籍信息
        BookDTO book = getBook(author, name);

        // 发送选择书籍事件
        if (book != null) {
            CommandBus.getInstance().dispatchAsync(Command.of(
                    CommandType.SELECT_BOOK,
                    new SelectBookPayload(book, book.getDurChapterIndex())
            ));
        }
    }

    /**
     * 设置地址 UI
     */
    private void setAddressUI() {
        List<String> addressHistoryList = AddressHistoryStorage.getInstance().getAddressList();
        // 设置书架面板的 ip输入框的历史记录
        ADDRESS_HISTORY_BOX_MODEL.removeAllElements();
        ADDRESS_HISTORY_BOX_MODEL.addAll(addressHistoryList);

        if (addressHistoryList.isEmpty()) {
            addressHistoryBox.setEnabled(false);
            ADDRESS_HISTORY_BOX_MODEL.addElement("127:0.0.1:1122");
            return;
        }

        // 设置书架面板的 ip输入框
        addressHistoryBox.setEnabled(true);
        ADDRESS_HISTORY_BOX_MODEL.setSelectedItem(addressHistoryList.getFirst());
        addressTextField.setText(addressHistoryList.getFirst());
    }

    /**
     * 获取书籍
     */
    private BookDTO getBook(String author, String name) {
        if (bookshelf == null) {
            return null;
        }
        return bookshelf.get(BOOK_MAP_KEY_FUNC.apply(author, name));
    }

    // ==================== 公共接口方法 ====================

    /**
     * 初始化地址历史记录
     * 在 ToolWindow 首次显示时调用
     */
    public void initAddressHistory() {
        setAddressUI();
    }
}

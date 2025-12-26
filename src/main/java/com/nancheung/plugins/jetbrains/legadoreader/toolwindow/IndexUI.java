package com.nancheung.plugins.jetbrains.legadoreader.toolwindow;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.nancheung.plugins.jetbrains.legadoreader.api.ApiUtil;
import com.nancheung.plugins.jetbrains.legadoreader.api.dto.BookDTO;
import com.nancheung.plugins.jetbrains.legadoreader.command.Command;
import com.nancheung.plugins.jetbrains.legadoreader.command.CommandBus;
import com.nancheung.plugins.jetbrains.legadoreader.command.CommandType;
import com.nancheung.plugins.jetbrains.legadoreader.command.payload.SelectBookPayload;
import com.nancheung.plugins.jetbrains.legadoreader.common.Constant;
import com.nancheung.plugins.jetbrains.legadoreader.event.PaginationEvent;
import com.nancheung.plugins.jetbrains.legadoreader.event.ReaderEvent;
import com.nancheung.plugins.jetbrains.legadoreader.event.ReaderEventListener;
import com.nancheung.plugins.jetbrains.legadoreader.event.ReadingEvent;
import com.nancheung.plugins.jetbrains.legadoreader.manager.ReadingSessionManager;
import com.nancheung.plugins.jetbrains.legadoreader.model.ReadingSession;
import com.nancheung.plugins.jetbrains.legadoreader.service.IPaginationManager;
import com.nancheung.plugins.jetbrains.legadoreader.service.PaginationManager;
import com.nancheung.plugins.jetbrains.legadoreader.storage.AddressHistoryStorage;
import com.nancheung.plugins.jetbrains.legadoreader.storage.PluginSettingsStorage;
import lombok.Getter;
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

@Slf4j
@Getter
public class IndexUI {

    // ==================== 卡片常量 ====================
    private static final String CARD_BOOKSHELF = "BOOKSHELF";
    private static final String CARD_TEXT_BODY = "TEXT_BODY";
    private static final String CARD_CONTENT = "CONTENT";
    private static final String CARD_ERROR = "ERROR";

    // ==================== UI 状态枚举 ====================
    private enum UIState {
        INITIALIZED,
        LOADING,
        SUCCESS,
        FAILED
    }

    private UIState currentState = UIState.INITIALIZED;

    // ==================== 根面板 ====================
    private JBPanel<?> rootPanel;
    private CardLayout mainCardLayout;

    // ==================== 书架面板组件 ====================
    private JBPanel<?> bookshelfPanel;
    private JBTextField addressTextField;
    private ComboBox<String> addressHistoryBox;
    private JButton refreshBookshelfButton;
    private JBScrollPane bookshelfScrollPane;
    private JBTable bookshelfTable;
    private JBLabel bookshelfErrorLabel;
    private JBPanel<?> bookshelfContentPanel;
    private CardLayout bookshelfContentLayout;

    // ==================== 正文面板组件 ====================
    private JBPanel<?> textBodyPanel;
    private ActionToolbar actionToolbar;
    private JBScrollPane textBodyScrollPane;
    private JTextPane textBodyPane;
    private JBLabel textBodyErrorLabel;
    private JBPanel<?> textBodyContentPanel;
    private CardLayout textBodyContentLayout;

    // ==================== 数据模型 ====================
    private static final DefaultTableModel BOOK_SHELF_TABLE_MODEL = new DefaultTableModel(
            null,
            new String[]{"name", "current", "new", "author"}
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public static final DefaultComboBoxModel<String> ADDRESS_HISTORY_BOX_MODEL = new DefaultComboBoxModel<>();

    // ==================== 单例实例 ====================
    private static IndexUI INSTANCE;

    // ==================== 书架数据 ====================
    private Map<String, BookDTO> bookshelf;
    private static final BiFunction<String, String, String> BOOK_MAP_KEY_FUNC = (author, name) -> author + "#" + name;

    // ==================== 错误提示文本 ====================
    private static final String ERROR_MESSAGE = """
            请求内容失败，请检查web服务是否开启、url是否正确、网络是否正常？
            
            小提示：可以在File -> Settings -> Tools -> Legado Reader中进行更多设置哦~
            """;

    // ==================== 构造函数 ====================
    public IndexUI() {
        // 1. 创建 UI 组件
        createRootPanel();

        // 2. 初始化组件状态
        initComponentStates();

        // 3. 绑定事件监听器
        bindEventListeners();

        // 4. 订阅阅读事件
        subscribeToReaderEvents();

        // 5. 初始加载书架
        initialLoadBookshelf();
    }

    // ==================== 组件创建方法 ====================

    /**
     * 创建根面板
     */
    private void createRootPanel() {
        mainCardLayout = new CardLayout();
        rootPanel = new JBPanel<>(mainCardLayout);

        // 创建书架面板
        bookshelfPanel = createBookshelfPanel();
        rootPanel.add(bookshelfPanel, CARD_BOOKSHELF);

        // 创建正文面板
        textBodyPanel = createTextBodyPanel();
        rootPanel.add(textBodyPanel, CARD_TEXT_BODY);

        // 默认显示书架
        mainCardLayout.show(rootPanel, CARD_BOOKSHELF);
    }

    /**
     * 创建书架面板
     */
    private JBPanel<?> createBookshelfPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());

        // 1. 顶部地址栏
        JBPanel<?> addressBar = createAddressBar();
        panel.add(addressBar, BorderLayout.NORTH);

        // 2. 中央内容区（使用 CardLayout 切换内容/错误）
        bookshelfContentLayout = new CardLayout();
        bookshelfContentPanel = new JBPanel<>(bookshelfContentLayout);

        // 2.1 内容卡片：书架表格
        bookshelfScrollPane = new JBScrollPane(createBookshelfTable());
        bookshelfContentPanel.add(bookshelfScrollPane, CARD_CONTENT);

        // 2.2 错误卡片：错误提示
        bookshelfErrorLabel = createErrorLabel();
        bookshelfContentPanel.add(wrapCentered(bookshelfErrorLabel), CARD_ERROR);

        panel.add(bookshelfContentPanel, BorderLayout.CENTER);

        return panel;
    }

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

        // 历史记录下拉框
        addressHistoryBox = new ComboBox<>(ADDRESS_HISTORY_BOX_MODEL);

        // 刷新按钮
        refreshBookshelfButton = new JButton("刷新");

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
        bookshelfTable = new JBTable(BOOK_SHELF_TABLE_MODEL);
        bookshelfTable.setFillsViewportHeight(true);
        bookshelfTable.setRowSelectionAllowed(true);
        bookshelfTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return bookshelfTable;
    }

    /**
     * 创建正文面板
     */
    private JBPanel<?> createTextBodyPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());

        // 1. 顶部工具栏
        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup actionGroup = (DefaultActionGroup) actionManager.getAction(Constant.PLUGIN_TOOL_BAR_ID);
        actionToolbar = actionManager.createActionToolbar(Constant.PLUGIN_TOOL_BAR_ID, actionGroup, true);
        actionToolbar.setTargetComponent(panel);

        JComponent toolbarComponent = actionToolbar.getComponent();
        toolbarComponent.setBorder(JBUI.Borders.empty());
        panel.add(toolbarComponent, BorderLayout.NORTH);

        // 2. 中央内容区
        textBodyContentLayout = new CardLayout();
        textBodyContentPanel = new JBPanel<>(textBodyContentLayout);

        // 2.1 内容卡片：正文
        textBodyPane = new JTextPane();
        textBodyPane.setEditable(false);
        textBodyScrollPane = new JBScrollPane(textBodyPane);
        textBodyContentPanel.add(textBodyScrollPane, CARD_CONTENT);

        // 2.2 错误卡片
        textBodyErrorLabel = createErrorLabel();
        textBodyContentPanel.add(wrapCentered(textBodyErrorLabel), CARD_ERROR);

        panel.add(textBodyContentPanel, BorderLayout.CENTER);

        return panel;
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

    // ==================== 初始化方法 ====================

    /**
     * 初始化组件状态
     */
    private void initComponentStates() {
        // 设置数据模型
        addressHistoryBox.setModel(ADDRESS_HISTORY_BOX_MODEL);
        bookshelfTable.setModel(BOOK_SHELF_TABLE_MODEL);

        // 默认显示内容（不显示错误）
        showBookshelfContent();
        showTextBodyContent();
    }

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

    /**
     * 订阅阅读事件
     */
    private void subscribeToReaderEvents() {
        ApplicationManager.getApplication()
                .getMessageBus()
                .connect()
                .subscribe(ReaderEventListener.TOPIC, (ReaderEventListener) event -> {
                    switch (event) {
                        case ReadingEvent e -> INSTANCE.onReadingEvent(e);
                        case PaginationEvent e -> INSTANCE.onPaginationEvent(e);
                        default -> {
                        }
                    }
                });
    }

    /**
     * 初始加载书架
     */
    private void initialLoadBookshelf() {
        refreshBookshelf(
                books -> refreshBookshelfButton.setEnabled(true),
                error -> refreshBookshelfButton.setEnabled(true)
        );
    }

    // ==================== 面板切换方法 ====================

    /**
     * 显示书架面板
     */
    public void showBookshelfPanel() {
        mainCardLayout.show(rootPanel, CARD_BOOKSHELF);
    }

    /**
     * 显示正文面板
     */
    public void showTextBodyPanel() {
        mainCardLayout.show(rootPanel, CARD_TEXT_BODY);
    }

    /**
     * 显示书架内容（隐藏错误）
     */
    public void showBookshelfContent() {
        bookshelfContentLayout.show(bookshelfContentPanel, CARD_CONTENT);
    }

    /**
     * 显示书架错误（隐藏内容）
     */
    public void showBookshelfError() {
        bookshelfContentLayout.show(bookshelfContentPanel, CARD_ERROR);
    }

    /**
     * 显示正文内容（隐藏错误）
     */
    public void showTextBodyContent() {
        textBodyContentLayout.show(textBodyContentPanel, CARD_CONTENT);
    }

    /**
     * 显示正文错误（隐藏内容）
     */
    public void showTextBodyError() {
        textBodyContentLayout.show(textBodyContentPanel, CARD_ERROR);
    }

    // ==================== 业务逻辑方法 ====================

    /**
     * 刷新书架
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
                    showBookshelfError();

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
        showBookshelfContent();
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

        // 加载章节（事件驱动）
        CommandBus.getInstance().dispatchAsync(Command.of(
                CommandType.SELECT_BOOK,
                new SelectBookPayload(book, book.getDurChapterIndex())
        ));
    }

    /**
     * 设置地址 UI
     */
    private void setAddressUI() {
        List<String> addressHistoryList = AddressHistoryStorage.getInstance().getAddressList();
        // 设置书架面板的ip输入框的历史记录
        ADDRESS_HISTORY_BOX_MODEL.removeAllElements();
        ADDRESS_HISTORY_BOX_MODEL.addAll(addressHistoryList);

        if (addressHistoryList.isEmpty()) {
            addressHistoryBox.setEnabled(false);
            ADDRESS_HISTORY_BOX_MODEL.addElement("无历史记录");
            return;
        }

        // 设置书架面板的ip输入框
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
     * 获取根组件
     */
    public JComponent getComponent() {
        return rootPanel;
    }

    /**
     * 获取单例实例
     */
    public static IndexUI getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new IndexUI();
        }
        return INSTANCE;
    }

    /**
     * 初始化地址历史记录
     * 在 ToolWindow 首次显示时调用
     */
    public void initAddressHistory() {
        setAddressUI();
    }

    // ==================== 事件处理方法 ====================

    /**
     * 阅读事件分发器
     */
    public void onReadingEvent(ReadingEvent event) {
        ApplicationManager.getApplication().invokeLater(() -> {
            switch (event.type()) {
                case CHAPTER_LOADING -> handleLoadingStarted(event);
                case CHAPTER_LOADED -> handleLoadingSuccess(event);
                case CHAPTER_LOAD_FAILED -> handleLoadingFailed(event);
                case SESSION_ENDED -> handleSessionEnded();
            }
        });
    }

    /**
     * 处理"会话结束"事件
     */
    private void handleSessionEnded() {
        currentState = UIState.INITIALIZED;
        log.info("会话结束，返回书架");

        // 显示书架面板
        showBookshelfPanel();

        // 清空正文内容
        textBodyPane.setText("");
    }

    /**
     * 处理分页事件
     */
    private void onPaginationEvent(PaginationEvent event) {
        ApplicationManager.getApplication().invokeLater(() -> {
            // 只处理页码变更事件（PAGE_CHANGED）
            if (event.type() != PaginationEvent.PaginationEventType.PAGE_CHANGED) {
                return;
            }

            // 如果正文面板不可见，跳过（用户可能在书架）
            if (!textBodyPanel.isVisible()) {
                log.debug("正文面板不可见，跳过光标同步");
                return;
            }

            // 获取当前页数据
            PaginationManager paginationManager = PaginationManager.getInstance();
            IPaginationManager.PageData currentPage = paginationManager.getCurrentPage();

            if (currentPage == null || currentPage.startPos() < 0) {
                log.warn("分页事件但无当前页数据或起始位置无效");
                return;
            }

            // 获取当前阅读会话以获取标题
            ReadingSession session = ReadingSessionManager.getInstance().getSession();
            if (session == null || session.currentContent() == null) {
                log.debug("无当前阅读会话，跳过光标同步");
                return;
            }

            // 计算标题长度（标题 + 换行符）
            String title = session.chapters().get(session.currentChapterIndex()).getTitle();
            int titleLength = (title != null && !title.isEmpty()) ? title.length() + 1 : 0;

            // 计算光标位置
            int caretPosition = titleLength + currentPage.startPos();

            // 限制在有效范围内
            String currentText = textBodyPane.getText();
            caretPosition = Math.min(caretPosition, currentText.length());

            // 设置光标位置
            textBodyPane.setCaretPosition(caretPosition);

            // 滚动到光标位置（确保光标可见）
            try {
                Rectangle viewRect = textBodyPane.modelToView2D(caretPosition).getBounds();
                textBodyPane.scrollRectToVisible(viewRect);
            } catch (javax.swing.text.BadLocationException e) {
                log.debug("滚动到光标位置失败: {}", e.getMessage());
            }

            log.debug("光标同步完成：页码 {}/{}, 光标位置 {}",
                    event.currentPage(), event.totalPages(), caretPosition);
        });
    }

    /**
     * 处理"开始加载"事件
     */
    private void handleLoadingStarted(ReadingEvent event) {
        currentState = UIState.LOADING;

        log.info("UI 进入加载状态: book={}, chapterIndex={}",
                event.book().getName(), event.chapter().getIndex());

        // 切换到正文面板并显示内容区
        showTextBodyPanel();
        showTextBodyContent();

        // 设置加载提示
        textBodyPane.setText("加载中...");

        // 获取焦点
        textBodyPane.requestFocus();
    }

    /**
     * 处理"加载成功"事件
     */
    private void handleLoadingSuccess(ReadingEvent event) {
        currentState = UIState.SUCCESS;

        log.info("UI 加载成功: book={}, chapter={}",
                event.book().getName(), event.chapter().getTitle());

        // 设置正文字体样式
        PluginSettingsStorage storage = PluginSettingsStorage.getInstance();
        Color fontColor = storage.getTextBodyFontColor();
        Font font = storage.getTextBodyFont();
        double lineHeight = storage.getTextBodyLineHeight();

        textBodyPane.setForeground(new JBColor(fontColor, fontColor));
        textBodyPane.setFont(font);

        // 设置正文内容
        String title = event.chapter().getTitle();
        String content = event.content();
        textBodyPane.setText(title + "\n" + content);

        // 应用行高（在 setText 之后）
        applyLineHeight(textBodyPane, lineHeight);

        // 设置光标位置
        textBodyPane.setCaretPosition(event.chapterPosition());
    }

    /**
     * 应用行高到 JTextPane
     */
    private void applyLineHeight(JTextPane textPane, double lineHeight) {
        javax.swing.text.SimpleAttributeSet attrs = new javax.swing.text.SimpleAttributeSet();
        float lineSpacing = (float) (lineHeight - 1.0);
        javax.swing.text.StyleConstants.setLineSpacing(attrs, lineSpacing);

        javax.swing.text.StyledDocument doc = textPane.getStyledDocument();
        doc.setParagraphAttributes(0, doc.getLength(), attrs, false);
    }

    /**
     * 处理"加载失败"事件
     */
    private void handleLoadingFailed(ReadingEvent event) {
        currentState = UIState.FAILED;

        log.warn("UI 加载失败: book={}, chapterIndex={}, error={}",
                event.book().getName(),
                event.chapter().getIndex(),
                event.error() != null ? event.error().getMessage() : "Unknown");

        // 显示错误提示
        showTextBodyError();

        // 记录详细错误日志
        if (Boolean.TRUE.equals(PluginSettingsStorage.getInstance().getState().enableErrorLog)) {
            log.error("章节加载失败", event.error());
        }
    }
}

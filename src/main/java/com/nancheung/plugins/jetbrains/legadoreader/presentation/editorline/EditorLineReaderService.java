package com.nancheung.plugins.jetbrains.legadoreader.presentation.editorline;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.nancheung.plugins.jetbrains.legadoreader.event.PaginationEvent;
import com.nancheung.plugins.jetbrains.legadoreader.event.ReaderEvent;
import com.nancheung.plugins.jetbrains.legadoreader.event.ReaderEventListener;
import com.nancheung.plugins.jetbrains.legadoreader.event.ReadingEvent;
import com.nancheung.plugins.jetbrains.legadoreader.event.SettingsChangedEvent;
import com.nancheung.plugins.jetbrains.legadoreader.presentation.common.UIEventSubscriber;
import com.nancheung.plugins.jetbrains.legadoreader.service.PaginationManager;
import com.nancheung.plugins.jetbrains.legadoreader.storage.PluginSettingsStorage;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * 编辑器行内阅读服务，请使用事件订阅模式，不要直接调用此类的方法
 * 实现 IReader 接口（保持兼容），但实际逻辑已迁移到事件订阅模式
 * 订阅 ReaderEventListener，监听章节和分页事件以触发编辑器刷新
 *
 * @author NanCheung
 */
@Slf4j
public class EditorLineReaderService extends UIEventSubscriber {

    private final PaginationManager paginationManager;

    /**
     * 构造函数
     * 订阅阅读事件，当章节切换或分页时自动刷新编辑器
     */
    public EditorLineReaderService() {
        super();
        this.paginationManager = PaginationManager.getInstance();

        log.debug("EditorLineReaderService 已初始化");
    }

    /**
     * 处理阅读事件
     * 当章节加载成功时，重新分页并定位页码
     *
     * @param event 阅读事件
     */
    @Override
    protected void onReadingEvent(ReadingEvent event) {
        if (event.type() != ReadingEvent.ReadingEventType.CHAPTER_LOADED) {
            return;
        }

        // 获取内容并重新分页
        String content = event.content();

        int pageSize = new JLabel().getFont().getSize() * 2;
        paginationManager.paginate(content, pageSize);

        // 根据方向定位页码
        if (event.direction() == ReadingEvent.Direction.PREVIOUS) {
            // 上一章，定位到最后一页
            paginationManager.goToLastPage();
            log.debug("上一章，定位到最后一页");
        } else {
            // 下一章或跳转，定位到第一页
            paginationManager.goToFirstPage();
            log.debug("下一章或跳转，定位到第一页");
        }

        // 刷新编辑器
        refreshEditor();

        log.info("EditorLine 事件处理完成：{}", event.chapter().getTitle());
    }

    /**
     * 处理分页事件
     * 当分页变更时，刷新编辑器显示
     *
     * @param event 分页事件
     */
    @Override
    protected void onPaginationEvent(PaginationEvent event) {
        // 刷新编辑器显示新的页码
        refreshEditor();
        log.debug("分页事件：页码 {}/{}", event.currentPage(), event.totalPages());
    }

    /**
     * 处理设置变更事件
     * 当用户在设置页面保存字体设置后，触发编辑器重绘
     *
     * @param event 设置变更事件
     */
    @Override
    protected void onSettingsChangedEvent(SettingsChangedEvent event) {
        // 只处理字体设置变更
        if (event.type() != SettingsChangedEvent.SettingsChangedType.FONT_SETTINGS
                && event.type() != SettingsChangedEvent.SettingsChangedType.ALL_SETTINGS) {
            return;
        }

        // 触发编辑器重绘（ReaderEditorLinePainter 会读取最新设置）
        refreshEditor();

        log.info("EditorLine 设置变更处理完成：字体样式已刷新");
    }

    /**
     * 刷新编辑器，触发行内内容重绘
     * 在 EDT 线程中执行，确保线程安全
     */
    private void refreshEditor() {
        ApplicationManager.getApplication().invokeLater(() -> {
            // 获取当前打开的项目
            Project[] openProjects = ProjectManager.getInstance().getOpenProjects();

            for (Project project : openProjects) {
                if (project != null && !project.isDisposed()) {
                    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                    if (editor != null && !editor.isDisposed()) {
                        // 触发编辑器内容组件重绘
                        editor.getContentComponent().repaint();
                        log.debug("触发编辑器重绘");
                    }
                }
            }
        });
    }
}

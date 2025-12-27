package com.nancheung.plugins.jetbrains.legadoreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.nancheung.plugins.jetbrains.legadoreader.presentation.toolwindow.MainReaderPanel;
import org.jetbrains.annotations.NotNull;

public class BackBookshelfAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        backBookshelf();
    }

    private void backBookshelf() {
        MainReaderPanel mainPanel = MainReaderPanel.getInstance();

        // 刷新书架
        mainPanel.getBookshelfPanel().refreshBookshelf(bookDTOS -> {
        }, throwable -> {
        });

        // 显示书架面板
        mainPanel.showBookshelfPanel();
    }
}

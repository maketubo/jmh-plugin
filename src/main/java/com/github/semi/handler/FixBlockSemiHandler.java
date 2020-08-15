package com.github.semi.handler;

import com.github.semi.ISemiHandler;
import com.github.semi.SemiEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;

/**
 * @author maketubo
 * @version 1.0
 * @ClassName OnBlockEndSemiHandler
 * @description
 * @date 2020/5/10 22:09
 * @since JDK 1.8
 */
public class FixBlockSemiHandler implements ISemiHandler {
    @Override
    public boolean isEnabledForCase(SemiEvent event) {
        return false;
    }

    @Override
    public void execute(Editor editor, Caret caret, DataContext dataContext, SemiEvent enterEvent) {

    }
}

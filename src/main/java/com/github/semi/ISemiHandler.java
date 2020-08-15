package com.github.semi;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;

/**
 * @author maketubo
 * @version 1.0
 * @ClassName IEnterHandler
 * @description
 * @date 2020/5/10 15:14
 * @since JDK 1.8
 */
public interface ISemiHandler {
    /**
     * judge should be case or not
     */
    boolean isEnabledForCase(SemiEvent event);

    /**
     *
     */
    void execute(Editor editor, Caret caret, DataContext dataContext, SemiEvent enterEvent);
}

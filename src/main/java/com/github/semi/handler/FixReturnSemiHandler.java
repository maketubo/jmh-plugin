package com.github.semi.handler;

import com.github.semi.SemiEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;

import java.util.regex.Pattern;

/**
 * @author maketubo
 * @version 1.0
 * @ClassName OnReturnEnterHandler
 * @description
 * @date 2020/5/10 15:15
 * @since JDK 1.8
 */
public class FixReturnSemiHandler extends BaseSemiHandler {

    private static final Pattern RETURN_PATTERN = Pattern.compile("\\s*return\\s*.*", Pattern.DOTALL);

    @Override
    public boolean isEnabledForCase(SemiEvent event) {
        return RETURN_PATTERN.matcher(event.getCurrentLine()).matches();
    }

    @Override
    public void execute(Editor editor, Caret caret, DataContext dataContext, SemiEvent enterEvent) {
//        String s = str + enterEvent.getTotalContent()
//                .subSequence(enterEvent.getCurrentLineStart(),
//                        Math.min(enterEvent.getCaretIndex(), enterEvent.getCurrentLineStartWsEndOffset()));
        appendToEnd(";", editor, enterEvent);
        formatCurrentLine(editor, dataContext);
    }
}

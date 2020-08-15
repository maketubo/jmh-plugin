package com.github.semi;


import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author maketubo
 * @version 1.0
 * @ClassName SuperEnterAction
 * @description
 * @date 2020/5/10 15:12
 * @since JDK 1.8
 */
public class SemiAction extends EditorAction {

    public SemiAction() {
        super(new Handler());
    }

    private static class Handler extends EditorWriteActionHandler {

        List<ISemiHandler> matchHandlers = new ArrayList<>();
        SemiEvent enterEvent;

        public Handler() {
            super(true);
        }

        @Override
        public boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
            final Document document = editor.getDocument();
            final CharSequence chars = document.getCharsSequence();
            int offset = editor.getCaretModel().getOffset();
            int lineNumber = document.getLineNumber(offset);
            int lineStart = document.getLineStartOffset(lineNumber);
            int lineEnd = document.getLineEndOffset(lineNumber);
            final CharSequence currentLine = chars.subSequence(lineStart, lineEnd);
            enterEvent = new SemiEvent(currentLine);
            matchHandlers = HandlerManager.getHandlers().stream()
                    .filter(handler -> handler.isEnabledForCase(enterEvent))
                    .collect(Collectors.toList());
            boolean empty = matchHandlers.isEmpty();
            if (!empty) {
                enterEvent.setTotalContent(chars);
                enterEvent.setCaretIndex(offset);
                enterEvent.setCurrentLineNum(lineNumber);
                enterEvent.setCurrentLineStart(lineStart);
                enterEvent.setCurrentLineEnd(lineEnd);
                int lineStartWsEndOffset = CharArrayUtil.shiftForward(chars, lineStart, " \t"); //currentLineStartWsEndOffset
                enterEvent.setCurrentLineStartWsEndOffset(lineStartWsEndOffset);
            }
            return !empty;
        }

        @Override
        public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
            matchHandlers.forEach(matchHandler -> matchHandler.execute(editor, caret, dataContext, enterEvent));
        }

    }


}

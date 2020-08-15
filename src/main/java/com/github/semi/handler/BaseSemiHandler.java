package com.github.semi.handler;

import com.github.semi.SemiEvent;
import com.github.semi.ISemiHandler;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

/**
 * @author maketubo
 * @version 1.0
 * @ClassName BaseEnterHandler
 * @description
 * @date 2020/5/10 15:29
 * @since JDK 1.8
 */
public abstract class BaseSemiHandler implements ISemiHandler {

    /**
     * append special string to line end
     */
    public void appendToEnd(String str, Editor editor, SemiEvent enterEvent) {
        editor.getDocument().insertString(enterEvent.getCurrentLineEnd(), str);
        editor.getCaretModel().moveToOffset(enterEvent.getCurrentLineEnd() + str.length());
        EditorModificationUtil.scrollToCaret(editor);
        editor.getSelectionModel().removeSelection();
    }

    /**
     * format code in current line 
     */
    public void formatCurrentLine(Editor editor, DataContext dataContext) {
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project != null) {
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            editor.getSelectionModel().selectLineAtCaret();
            if (file != null) {
                ReformatCodeProcessor processor = new ReformatCodeProcessor(file, editor.getSelectionModel());
                processor.runWithoutProgress();
                editor.getSelectionModel().removeSelection();
            }
        }
    }
}

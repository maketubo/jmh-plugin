package com.github.jmh;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testIntegration.LanguageTestCreators;
import com.intellij.testIntegration.TestCreator;
import com.intellij.testIntegration.createTest.CreateTestAction;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;


/**
 * @author maketubo
 * @version 1.0
 * @ClassName JmhAction
 * @description
 * @date 2020/5/10 22:34
 * @since JDK 1.8
 */
public class JmhAction extends AnAction {

    static Project project =  null;
    static PsiFile file =  null;
    static Editor editor  =  null;
    private static final Logger LOG = Logger.getInstance(JmhAction.class);
    private static TestCreator creator = null;
    static {
        creator = LanguageTestCreators.INSTANCE.allForLanguage(JavaLanguage.INSTANCE).get(0);
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getData(CommonDataKeys.PROJECT);
        file = e.getData(CommonDataKeys.PSI_FILE);
        editor = e.getData(CommonDataKeys.EDITOR);
        createTest(project, editor, file);
    }

    public void createTest(Project project, Editor editor, PsiFile file) {
        try {
            PsiElement element = findElement(file, editor.getCaretModel().getOffset());
            new CreateJmhAction().invoke(project, editor, element);
        }
        catch (IncorrectOperationException e) {
            LOG.warn(e);
        }
    }

    private static PsiElement findElement(PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element == null && offset == file.getTextLength()) {
            element = file.findElementAt(offset - 1);
        }
        return element;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        String text = creator instanceof ItemPresentation ? ((ItemPresentation)creator).getPresentableText() : null;
        project = e.getData(CommonDataKeys.PROJECT);
        file = e.getData(CommonDataKeys.PSI_FILE);
        editor = e.getData(CommonDataKeys.EDITOR);
        Presentation presentation = e.getPresentation();
        presentation.setText(ObjectUtils.notNull(text, "jmh..."));
        presentation.setEnabledAndVisible(isAvailable(project, editor, file));
    }

    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = findElement(file, offset);
        return CreateTestAction.isAvailableForElement(element);
    }

    @Override
    public boolean isDumbAware() {
        return DumbService.isDumbAware(creator);
    }
}

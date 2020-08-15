package com.github;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;

public class HelloWorld extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        String message =  "thank to use tools from https://github.com/maketubo!\n" +
                "welcome contribute pr!";
        String title = "hello";
        Messages.showMessageDialog(project, message, title, Messages.getInformationIcon());
    }
}

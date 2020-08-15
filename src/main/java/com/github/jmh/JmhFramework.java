package com.github.jmh;

import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.lang.Language;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/*
 *
 * @author maketubo
 * @version 1.0
 * @date 2020/6/18 23:54
 * @param null
 * @return
 * @since JDK 1.8
 */
public interface JmhFramework {

    ExtensionPointName<JmhFramework> EXTENSION_NAME = ExtensionPointName.create("com.github.jmhFramework");

    @NotNull
    String getName();

    @NotNull
    Icon getIcon();

    boolean isLibraryAttached(@NotNull Module module);

    @Nullable
    String getLibraryPath();

    @NotNull
    Language getLanguage();

    String getDefaultSuperClass();

    ExternalLibraryDescriptor[] getFrameworkLibraryDescriptor();

    boolean isJmhClass(@NotNull PsiElement clazz);

    @Nullable
    PsiElement findSetUpMethod(@NotNull PsiElement clazz);

    @Nullable
    PsiElement findTearDownMethod(@NotNull PsiElement clazz);

    FileTemplateDescriptor getSetUpMethodFileTemplateDescriptor();

    FileTemplateDescriptor getTearDownMethodFileTemplateDescriptor();

    FileTemplateDescriptor getPsvmTemplateDescriptor();

    FileTemplateDescriptor getJmhClassFileTemplateDescriptor();

    boolean isPotentialTestClass(PsiClass targetClass);

    FileTemplateDescriptor getBenchmarkMethodFileTemplateDescriptor();
}

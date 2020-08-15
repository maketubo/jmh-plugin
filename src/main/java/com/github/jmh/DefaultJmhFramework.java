package com.github.jmh;


import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.icons.AllIcons;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.lang.Language;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.roots.JavaProjectModelModificationService;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.Icon;
import java.util.Collections;

/**
 * @author maketubo
 * @version 1.0
 * @ClassName DefaultJmhFramework
 * @description
 * @date 2020/6/18 23:55
 * @since JDK 1.8
 */
public class DefaultJmhFramework implements JmhFramework {

    public static final Icon speed = IconLoader.getIcon("/nodes/cvs_global.svg", AllIcons.class);

    @NotNull
    @Override
    public String getName() {
        return "jmh";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return speed;
    }

    @Override
    public boolean isLibraryAttached(@NotNull Module module) {
        GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
        PsiClass c = JavaPsiFacade.getInstance(module.getProject()).findClass(getMarkerClassFQName(), scope);
        return c != null;
    }

    protected String getMarkerClassFQName() {
        return "org.openjdk.jmh.annotations.Benchmark";
    }

    @Nullable
    @Override
    public String getLibraryPath() {
        return null;
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return null;
    }

    @Override
    public String getDefaultSuperClass() {
        return null;
    }

    @Override
    public ExternalLibraryDescriptor[] getFrameworkLibraryDescriptor() {
        return new ExternalLibraryDescriptor[] {JmhExternalLibraryDescriptor.JMH_CORE,
                JmhExternalLibraryDescriptor.JMH_ANNO};
    }

    @Override
    public boolean isJmhClass(@NotNull PsiElement clazz) {
        return false;
    }

    @Nullable
    @Override
    public PsiElement findSetUpMethod(@NotNull PsiElement clazz) {
        return null;
    }

    @Nullable
    @Override
    public PsiElement findTearDownMethod(@NotNull PsiElement clazz) {
        return null;
    }

    @Override
    public FileTemplateDescriptor getSetUpMethodFileTemplateDescriptor() {
        return new FileTemplateDescriptor("Jmh Setup Method.java");
    }

    @Override
    public FileTemplateDescriptor getTearDownMethodFileTemplateDescriptor() {
        return new FileTemplateDescriptor("Jmh TearDown Method.java");
    }

    @Override
    public FileTemplateDescriptor getPsvmTemplateDescriptor() {
        return new FileTemplateDescriptor("Jmh Main Method.java");
    }

    @Override
    public FileTemplateDescriptor getJmhClassFileTemplateDescriptor() {
        return new FileTemplateDescriptor("Jmh Class.java");
    }

    @Override
    public boolean isPotentialTestClass(PsiClass targetClass) {
        return false;
    }

    @Override
    public FileTemplateDescriptor getBenchmarkMethodFileTemplateDescriptor() {
        return new FileTemplateDescriptor("Benchmark Method.java");
    }

    public Promise<Void> setupLibrary(Module module) {
        ExternalLibraryDescriptor[] descriptor = getFrameworkLibraryDescriptor();
        if (descriptor != null && descriptor.length > 0) {
            for(ExternalLibraryDescriptor desc : descriptor){
                JavaProjectModelModificationService.getInstance(module.getProject()).addDependency(module, desc, DependencyScope.TEST);
            }
            return Promises.resolvedPromise(null);
        }
        else {
            String path = getLibraryPath();
            if (path != null) {
                OrderEntryFix.addJarsToRoots(Collections.singletonList(path), null, module, null);
                return Promises.resolvedPromise(null);
            }
        }
        return Promises.rejectedPromise();
    }
}

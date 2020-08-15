package com.github.jmh;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.TestFrameworks;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.roots.TestModuleProperties;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testIntegration.TestFramework;
import com.intellij.testIntegration.TestIntegrationUtils;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author maketubo
 * @version 1.0
 * @ClassName CreateJmhAction
 * @description
 * @date 2020/5/27 0:14
 * @since JDK 1.8
 */
public class CreateJmhAction extends PsiElementBaseIntentionAction {

    private static final String CREATE_TEST_IN_THE_SAME_ROOT = "create.test.in.the.same.root";

    @NotNull
    public String getText() {
        return "create Java Microbenchmark Harness class";
    }

    @NotNull
    public String getFamilyName() {
        return getText();
    }

    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (!isAvailableForElement(element)) return false;

        PsiClass psiClass = getContainingClass(element);

        assert psiClass != null;
        PsiElement leftBrace = psiClass.getLBrace();
        if (leftBrace == null) return false;
        if (element.getTextOffset() >= leftBrace.getTextOffset()) return false;

        //TextRange declarationRange = HighlightNamesUtil.getClassDeclarationTextRange(psiClass);
        //if (!declarationRange.contains(element.getTextRange())) return false;

        return true;
    }

    public static boolean isAvailableForElement(PsiElement element) {
        if (!TestFramework.EXTENSION_NAME.hasAnyExtensions()) return false;

        if (element == null) return false;

        PsiClass psiClass = getContainingClass(element);

        if (psiClass == null) return false;

        PsiFile file = psiClass.getContainingFile();
        if (file.getContainingDirectory() == null || JavaProjectRootsUtil.isOutsideJavaSourceRoot(file)) return false;

        if (psiClass.isAnnotationType() ||
                psiClass instanceof PsiAnonymousClass) {
            return false;
        }

        return TestFrameworks.detectFramework(psiClass) == null;
    }

    public void invoke(final @NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        final Module srcModule = ModuleUtilCore.findModuleForPsiElement(element); //module info
        if (srcModule == null) return;

        final PsiClass srcClass = getContainingClass(element); //class info

        if (srcClass == null) return;
        // dir info
        PsiDirectory srcDir = element.getContainingFile().getContainingDirectory();
        PsiPackage srcPackage = JavaDirectoryService.getInstance().getPackage(srcDir); //package info
        //last open file path and junit config property
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        propertiesComponent.setValue(CREATE_TEST_IN_THE_SAME_ROOT, false);
        Module testModule = suggestModuleForTests(project, srcModule); // outputModule
        final List<VirtualFile> testRootUrls = computeTestRoots(testModule);
        if (testRootUrls.isEmpty() && computeSuitableTestRootUrls(testModule).isEmpty()) {
            testModule = srcModule;
            if (!propertiesComponent.getBoolean(CREATE_TEST_IN_THE_SAME_ROOT)) {
                if (Messages.showOkCancelDialog(project, "Create test in the same source root?", "No Test Roots Found", Messages.getQuestionIcon()) !=
                        Messages.OK) {
                    return;
                }
                propertiesComponent.setValue(CREATE_TEST_IN_THE_SAME_ROOT, true);
            }
        }

        final CreateJmhDialog d = createJmhDialog(project, testModule, srcClass, srcPackage);
        if (!d.showAndGet()) {
            return;
        }

        CommandProcessor.getInstance().executeCommand(project, () -> {
            final JmhGenerator generator = JmhGenerators.INSTANCE.forLanguage(JavaLanguage.INSTANCE);
            DumbService.getInstance(project).withAlternativeResolveEnabled(() -> generator.generateTest(project, d));
        }, CodeInsightBundle.message("intention.create.test"), this);
    }

    @NotNull
    public static Module suggestModuleForTests(@NotNull Project project, @NotNull Module productionModule) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (productionModule.equals(TestModuleProperties.getInstance(module).getProductionModule())) {
                return module;
            }
        }

        if (computeSuitableTestRootUrls(productionModule).isEmpty()) {
            final HashSet<Module> modules = new HashSet<>();
            ModuleUtilCore.collectModulesDependsOn(productionModule, modules);
            modules.remove(productionModule);
            List<Module> modulesWithTestRoot = modules.stream()
                    .filter(module -> !computeSuitableTestRootUrls(module).isEmpty())
                    .limit(2)
                    .collect(Collectors.toList());
            if (modulesWithTestRoot.size() == 1) return modulesWithTestRoot.get(0);
        }

        return productionModule;
    }

    protected CreateJmhDialog createJmhDialog(Project project, Module srcModule, PsiClass srcClass, PsiPackage srcPackage) {
        return new CreateJmhDialog(project, getText(), srcClass, srcPackage, srcModule);
    }

    static List<String> computeSuitableTestRootUrls(@NotNull Module module) {
        return suitableTestSourceFolders(module).map(SourceFolder::getUrl).collect(Collectors.toList());
    }

    protected static List<VirtualFile> computeTestRoots(@NotNull Module mainModule) {
        if (!computeSuitableTestRootUrls(mainModule).isEmpty()) {
            //create test in the same module, if the test source folder doesn't exist yet it will be created
            return suitableTestSourceFolders(mainModule)
                    .map(SourceFolder::getFile)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        //suggest to choose from all dependencies modules
        final HashSet<Module> modules = new HashSet<>();
        ModuleUtilCore.collectModulesDependsOn(mainModule, modules);
        return modules.stream()
                .flatMap(CreateJmhAction::suitableTestSourceFolders)
                .map(SourceFolder::getFile)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Stream<SourceFolder> suitableTestSourceFolders(@NotNull Module module) {
        Predicate<SourceFolder> forGeneratedSources = JavaProjectRootsUtil::isForGeneratedSources;
        return Arrays.stream(ModuleRootManager.getInstance(module).getContentEntries())
                .flatMap(entry -> entry.getSourceFolders(JavaSourceRootType.TEST_SOURCE).stream())
                .filter(forGeneratedSources.negate());
    }

    /**
     * @deprecated use {@link #computeTestRoots(Module)} instead
     */
    @Deprecated
    protected static void checkForTestRoots(Module srcModule, Set<? super VirtualFile> testFolders) {
        testFolders.addAll(computeTestRoots(srcModule));
    }

    @Nullable
    protected static PsiClass getContainingClass(PsiElement element) {
        PsiClass aClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);
        if (aClass == null) return null;
        return TestIntegrationUtils.findOuterClass(element);
    }

    public boolean startInWriteAction() {
        return false;
    }
}

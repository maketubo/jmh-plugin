package com.github.jmh;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.CodeInsightUtil;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.impl.analysis.ImportsHighlightUtil;
import com.intellij.codeInsight.template.Template;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JVMElementFactories;
import com.intellij.psi.JVMElementFactory;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.util.classMembers.MemberInfo;
import com.intellij.testIntegration.TestIntegrationUtils;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * jmh类的具体生成逻辑
 * @author maketubo
 * @version 1.0
 * @ClassName JmhGenerator
 * @description
 * @date 2020/5/28 0:07
 * @since JDK 1.8
 */
public class JmhGenerator {
    public JmhGenerator() {
    }

    public PsiElement generateTest(final Project project, final CreateJmhDialog d) {
        return PostprocessReformattingAspect.getInstance(project).postponeFormattingInside(
                () -> ApplicationManager.getApplication().runWriteAction(new Computable<PsiElement>() {
                    @Override
                    public PsiElement compute() {
                        try {
                            IdeDocumentHistory.getInstance(project).includeCurrentPlaceAsChangePlace();

                            PsiClass targetClass = createTestClass(d);
                            if (targetClass == null) {
                                return null;
                            }
                            final JmhFramework frameworkDescriptor = d.getSelectedTestFrameworkDescriptor();
                            final String defaultSuperClass = frameworkDescriptor.getDefaultSuperClass();
                            final String superClassName = d.getSuperClassName();
                            if (!Comparing.strEqual(superClassName, defaultSuperClass)) {
                                addSuperClass(targetClass, project, superClassName);
                            }

                            PsiFile file = targetClass.getContainingFile();
                            Editor editor = CodeInsightUtil.positionCursorAtLBrace(project, file, targetClass);
                            addTestMethods(editor,
                                    targetClass,
                                    d.getTargetClass(),
                                    frameworkDescriptor,
                                    d.getSelectedMethods(),
                                    d.shouldGeneratedBefore(),
                                    d.shouldGeneratedAfter());

                            if (file instanceof PsiJavaFile) {
                                PsiImportList list = ((PsiJavaFile)file).getImportList();
                                if (list != null) {
                                    PsiImportStatementBase[] importStatements = list.getAllImportStatements();
                                    if (importStatements.length > 0) {
                                        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(list);
                                        if (virtualFile != null) {
                                            Set<String> imports = new HashSet<>();
                                            for (PsiImportStatementBase base : importStatements) {
                                                imports.add(base.getText());
                                            }
                                            virtualFile.putCopyableUserData(ImportsHighlightUtil.IMPORTS_FROM_TEMPLATE, imports);
                                        }
                                    }
                                }
                            }

                            return targetClass;
                        }
                        catch (IncorrectOperationException e) {
                            showErrorLater(project, d.getClassName());
                            return null;
                        }
                    }
                }));
    }

    @Nullable
    private static PsiClass createTestClass(CreateJmhDialog d) {
        final JmhFramework testFrameworkDescriptor = d.getSelectedTestFrameworkDescriptor();
        final FileTemplateDescriptor fileTemplateDescriptor = JmhIntegrationUtils.MethodKind.MAIN_CLASS.getFileTemplateDescriptor(testFrameworkDescriptor);
        final PsiDirectory targetDirectory = d.getTargetDirectory();

        final PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(targetDirectory);
        if (aPackage != null) {
            final GlobalSearchScope scope = GlobalSearchScopesCore.directoryScope(targetDirectory, false);
            final PsiClass[] classes = aPackage.findClassByShortName(d.getClassName(), scope);
            if (classes.length > 0) {
                if (!FileModificationService.getInstance().preparePsiElementForWrite(classes[0])) {
                    return null;
                }
                return classes[0];
            }
        }

        if (fileTemplateDescriptor != null) {
            final PsiClass classFromTemplate = createTestClassFromCodeTemplate(d, fileTemplateDescriptor, targetDirectory);
            if (classFromTemplate != null) {
                return classFromTemplate;
            }
        }

        return JavaDirectoryService.getInstance().createClass(targetDirectory, d.getClassName());
    }

    private static PsiClass createTestClassFromCodeTemplate(final CreateJmhDialog d,
                                                            final FileTemplateDescriptor fileTemplateDescriptor,
                                                            final PsiDirectory targetDirectory) {
        final String templateName = fileTemplateDescriptor.getFileName();
        final FileTemplate fileTemplate = FileTemplateManager.getInstance(targetDirectory.getProject()).getCodeTemplate(templateName);
        final Properties defaultProperties = FileTemplateManager.getInstance(targetDirectory.getProject()).getDefaultProperties();
        Properties properties = new Properties(defaultProperties);
        properties.setProperty(FileTemplate.ATTRIBUTE_NAME, d.getClassName());
        Map<String, String> externalProperties = d.getExternalProperties();
        properties.putAll(externalProperties);
        final PsiClass targetClass = d.getTargetClass();
        if (targetClass != null && targetClass.isValid()) {
            properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, targetClass.getQualifiedName());
        }
        try {
            final PsiElement psiElement = FileTemplateUtil.createFromTemplate(fileTemplate, templateName, properties, targetDirectory);
            if (psiElement instanceof PsiClass) {
                return (PsiClass)psiElement;
            }
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }

    private static void addSuperClass(PsiClass targetClass, Project project, String superClassName) throws IncorrectOperationException {
        if (superClassName == null) return;
        final PsiReferenceList extendsList = targetClass.getExtendsList();
        if (extendsList == null) return;

        PsiElementFactory ef = JavaPsiFacade.getElementFactory(project);
        PsiJavaCodeReferenceElement superClassRef;

        PsiClass superClass = findClass(project, superClassName);
        if (superClass != null) {
            superClassRef = ef.createClassReferenceElement(superClass);
        }
        else {
            superClassRef = ef.createFQClassNameReferenceElement(superClassName, GlobalSearchScope.allScope(project));
        }
        final PsiJavaCodeReferenceElement[] referenceElements = extendsList.getReferenceElements();
        if (referenceElements.length == 0) {
            extendsList.add(superClassRef);
        } else {
            referenceElements[0].replace(superClassRef);
        }
    }

    @Nullable
    private static PsiClass findClass(Project project, String fqName) {
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        return JavaPsiFacade.getInstance(project).findClass(fqName, scope);
    }

    public static void addTestMethods(Editor editor,
                                      PsiClass targetClass,
                                      final JmhFramework descriptor,
                                      Collection<? extends MemberInfo> methods,
                                      boolean generateBefore,
                                      boolean generateAfter) throws IncorrectOperationException {
        addTestMethods(editor, targetClass, null, descriptor, methods, generateBefore, generateAfter);
    }

    public static void addTestMethods(Editor editor,
                                      PsiClass targetClass,
                                      @Nullable PsiClass sourceClass,
                                      final JmhFramework descriptor,
                                      Collection<? extends MemberInfo> methods,
                                      boolean generateBefore,
                                      boolean generateAfter) throws IncorrectOperationException {
        final Set<String> existingNames = new HashSet<>();
        PsiMethod anchor = null;
        if (generateBefore && descriptor.findSetUpMethod(targetClass) == null) {
            anchor = generateMethod(JmhIntegrationUtils.MethodKind.SET_UP, descriptor, targetClass, sourceClass, editor, "setUp", existingNames, anchor);
        }

        if (generateAfter && descriptor.findTearDownMethod(targetClass) == null) {
            anchor = generateMethod(JmhIntegrationUtils.MethodKind.TEAR_DOWN, descriptor, targetClass, sourceClass, editor, "tearDown", existingNames, anchor);
        }

        final Template template = JmhIntegrationUtils.createTestMethodTemplate(JmhIntegrationUtils.MethodKind.PSVM, descriptor,
                targetClass, sourceClass, null, true, existingNames);
        JVMElementFactory elementFactory = JVMElementFactories.getFactory(targetClass.getLanguage(), targetClass.getProject());
        final String prefix = elementFactory != null ? elementFactory.createMethodFromText(template.getTemplateText(), targetClass).getName() : "";
        existingNames.addAll(ContainerUtil.map(targetClass.getAllMethods(), method -> StringUtil.decapitalize(StringUtil.trimStart(method.getName(), prefix))));
        generateMethod(JmhIntegrationUtils.MethodKind.PSVM, descriptor, targetClass, sourceClass, editor, "psvm", existingNames, anchor);
        for (MemberInfo m : methods) {
            anchor = generateMethod(JmhIntegrationUtils.MethodKind.BENCHMARK, descriptor, targetClass, sourceClass, editor, m.getMember().getName(), existingNames, anchor);
        }
    }

    private static void showErrorLater(final Project project, final String targetClassName) {
        ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project,
                CodeInsightBundle.message("intention.error.cannot.create.class.message", targetClassName),
                CodeInsightBundle.message("intention.error.cannot.create.class.title")));
    }

    private static PsiMethod generateMethod(@NotNull JmhIntegrationUtils.MethodKind methodKind,
                                            JmhFramework descriptor,
                                            PsiClass targetClass,
                                            @Nullable PsiClass sourceClass,
                                            Editor editor,
                                            @Nullable String name,
                                            Set<? super String> existingNames, PsiMethod anchor) {
        PsiMethod dummyMethod = TestIntegrationUtils.createDummyMethod(targetClass);
        PsiMethod method = (PsiMethod)(anchor == null ? targetClass.add(dummyMethod) : targetClass.addAfter(dummyMethod, anchor));
        PsiDocumentManager.getInstance(targetClass.getProject()).doPostponedOperationsAndUnblockDocument(editor.getDocument());
        JmhIntegrationUtils.runTestMethodTemplate(methodKind, descriptor, editor, targetClass, sourceClass, method, name, true, existingNames);
        return method;
    }

    @Override
    public String toString() {
        return CodeInsightBundle.message("intention.create.test.dialog.java");
    }
}

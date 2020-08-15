package com.github.jmh;

import com.github.message.PromptBundle;
import com.intellij.CommonBundle;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.codeStyle.JavaCodeStyleSettings;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil;
import com.intellij.refactoring.ui.MemberSelectionTable;
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo;
import com.intellij.refactoring.util.RefactoringMessageUtil;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.refactoring.util.classMembers.MemberInfo;
import com.intellij.testIntegration.TestIntegrationUtils;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.RecentsManager;
import com.intellij.ui.ReferenceEditorComboWithBrowseButton;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  default dependency
 *          <dependency>
 *             <groupId>org.openjdk.jmh</groupId>
 *             <artifactId>jmh-core</artifactId>
 *             <version>${jmh.version}</version>
 *             <scope>compile</scope>
 *         </dependency>
 *         <dependency>
 *             <groupId>org.openjdk.jmh</groupId>
 *             <artifactId>jmh-generator-annprocess</artifactId>
 *             <version>${jmh.version}</version>
 *             <scope>compile</scope>
 *         </dependency>
 *   jmh library    [jmh-core & jmh-generator-annprocess]
 *   class name     current class name + Jmh
 *   supper class   [] ...
 *   Destination package [current package in source or test source] ...
 *   Generator :  BenchmarkMode []
 *                [] warmUp => iterations []
 *                fork [num]
 *                threads [num]
 *                Measurement iterations [], time [], timeUnit []
 *                outputFile [default/absolutePath]
 *                OutputTimeUnit []
 *  generate jmh for : [] show inherited methods
 *    [] A ()
 *    [] B ()
 *
 *
 *                             [] Ok [] cancle
 *  after click ok
 *
 *  @Benchmark
 *  public void A() {
 *      // u can write Benchmark code here now
 *
 *  }
 *  @Setup
 *  public void setUp() {
 *
 *  }
 *
 *  @TearDown
 *  public void tearDown() {
 *
 *  }
 *
 *   public static void main(String[] args) throws Exception {
 *     Options opt = new OptionsBuilder()
 *       .include(".*" + ${currentClass}.class.getSimpleName())
 *       .addProfiler("gc")
 *       .output("F:\\${somePath}\\Benchmark.log")
 *       .build();
 *
 *     new Runner(opt).run();
 *   }
 *
 * @author maketubo
 * @version 1.0
 * @ClassName CreateJmhDialog
 * @description
 * @date 2020/5/27 0:30
 * @since JDK 1.8
 */
public class CreateJmhDialog extends DialogWrapper {
    private static final String BENCHMARK_MODE_KEY = "benchmarkMode";
    private static final String RECENTS_KEY = "CreateJmhDialog.RecentsKey";
    private static final String RECENT_SUPERS_KEY = "CreateJmhDialog.Recents.Supers";
    private static final String DEFAULT_LIBRARY_NAME_PROPERTY = CreateJmhDialog.class.getName() + ".defaultLibrary";
    private static final String DEFAULT_LIBRARY_SUPERCLASS_NAME_PROPERTY = CreateJmhDialog.class.getName() + ".defaultLibrarySuperClass";
    private static final String SHOW_INHERITED_MEMBERS_PROPERTY = CreateJmhDialog.class.getName() + ".includeInheritedMembers";

    //project info
    private final Project myProject;
    private final PsiClass myTargetClass;
    private final PsiPackage myTargetPackage;
    private final Module myTargetModule;

    protected PsiDirectory myTargetDirectory;
    private JmhFramework mySelectedFramework;
    private HashMap<String, String> externalProperties;


    private final ComboBox<JmhFramework> myLibrariesCombo = new ComboBox<>(new DefaultComboBoxModel<>());
    private final ComboBox<String> benchmarkModeCombo = new ComboBox<>(new DefaultComboBoxModel<>());
    private EditorTextField myTargetClassNameField;
    private EditorTextField forkNumField;
    private EditorTextField warmUpNumField;
    private EditorTextField threadNumField;
    private ReferenceEditorComboWithBrowseButton mySuperClassField;
    private ReferenceEditorComboWithBrowseButton myTargetPackageField;
    // jmh setup and jmh tearDown
    private final JCheckBox myGenerateBeforeBox = new JCheckBox(
            CodeInsightBundle.message("intention.create.test.dialog.setUp"));
    private final JCheckBox myGenerateAfterBox = new JCheckBox(
            CodeInsightBundle.message("intention.create.test.dialog.tearDown"));


    private final JCheckBox myShowInheritedMethodsBox = new JCheckBox(PromptBundle
            .message("intention.create.jmh.dialog.show.inherited"));
    private final MemberSelectionTable myMethodsTable = new MemberSelectionTable(
            Collections.emptyList(), null);
    private final JButton myFixLibraryButton = new JButton(CodeInsightBundle.message("intention.create.test.dialog.fix.library"));
    private JPanel myFixLibraryPanel;
    private JLabel myFixLibraryLabel;
    private String defaultMode = "Mode.Throughput";

    public CreateJmhDialog(@NotNull Project project,
                            @NotNull String title,
                            PsiClass targetClass,
                            PsiPackage targetPackage,
                            Module targetModule) {
        super(project, true);
        myProject = project;

        myTargetClass = targetClass;
        myTargetPackage = targetPackage;
        myTargetModule = targetModule;

        setTitle(title);
        init();
    }

    protected String suggestTestClassName(PsiClass targetClass) {
        JavaCodeStyleSettings customSettings = JavaCodeStyleSettings.getInstance(targetClass.getContainingFile());
        String prefix = customSettings.TEST_NAME_PREFIX;
        String suffix = customSettings.TEST_NAME_SUFFIX;
        return prefix + targetClass.getName() + suffix;
    }

    private boolean isSuperclassSelectedManually() {
        String superClass = mySuperClassField.getText();
        if (StringUtil.isEmptyOrSpaces(superClass)) {
            return false;
        }

        for (JmhFramework framework : JmhFramework.EXTENSION_NAME.getExtensions()) {
            if (superClass.equals(framework.getDefaultSuperClass())) {
                return false;
            }
            if (superClass.equals(getLastSelectedSuperClassName(framework))) {
                return false;
            }
        }

        return true;
    }

    private void onLibrarySelected(JmhFramework descriptor) {
        if (descriptor.isLibraryAttached(myTargetModule)) {
            myFixLibraryPanel.setVisible(false);
        }
        else {
            myFixLibraryPanel.setVisible(true);
            String text = CodeInsightBundle.message("intention.create.test.dialog.library.not.found", descriptor.getName());
            myFixLibraryLabel.setText(text);
            myFixLibraryButton.setVisible(descriptor instanceof DefaultJmhFramework
                    && ((DefaultJmhFramework)descriptor).getFrameworkLibraryDescriptor() != null
                    || descriptor.getLibraryPath() != null);
        }

        String libraryDefaultSuperClass = descriptor.getDefaultSuperClass();
        String lastSelectedSuperClass = getLastSelectedSuperClassName(descriptor);
        String superClass = lastSelectedSuperClass != null ? lastSelectedSuperClass : libraryDefaultSuperClass;

        if (isSuperclassSelectedManually()) {
            if (superClass != null) {
                String currentSuperClass = mySuperClassField.getText();
                mySuperClassField.appendItem(superClass);
                mySuperClassField.setText(currentSuperClass);
            }
        }
        else {
            mySuperClassField.appendItem(StringUtil.notNullize(superClass));
            mySuperClassField.getChildComponent().setSelectedItem(StringUtil.notNullize(superClass));
        }

        mySelectedFramework = descriptor;
    }

    private void updateMethodsTable() {
        List<MemberInfo> methods = TestIntegrationUtils.extractClassMethods(
                myTargetClass, myShowInheritedMethodsBox.isSelected());

        Set<PsiMember> selectedMethods = new HashSet<>();
        for (MemberInfo each : myMethodsTable.getSelectedMemberInfos()) {
            selectedMethods.add(each.getMember());
        }
        for (MemberInfo each : methods) {
            each.setChecked(selectedMethods.contains(each.getMember()));
        }

        myMethodsTable.setMemberInfos(methods);
    }

    private String getDefaultLibraryName() {
        return "Jmh";
    }

    private String getLastSelectedSuperClassName(JmhFramework framework) {
        return getProperties().getValue(getDefaultSuperClassPropertyName(framework));
    }

    private void saveDefaultLibraryNameAndSuperClass() {
        getProperties().setValue(DEFAULT_LIBRARY_NAME_PROPERTY, mySelectedFramework.getName());
        getProperties().setValue(getDefaultSuperClassPropertyName(mySelectedFramework), mySuperClassField.getText());
    }

    private static String getDefaultSuperClassPropertyName(JmhFramework framework) {
        return DEFAULT_LIBRARY_SUPERCLASS_NAME_PROPERTY + "." + framework.getName();
    }

    private void restoreShowInheritedMembersStatus() {
        myShowInheritedMethodsBox.setSelected(getProperties().getBoolean(SHOW_INHERITED_MEMBERS_PROPERTY));
    }

    private void saveShowInheritedMembersStatus() {
        getProperties().setValue(SHOW_INHERITED_MEMBERS_PROPERTY, myShowInheritedMethodsBox.isSelected());
    }

    private PropertiesComponent getProperties() {
        return PropertiesComponent.getInstance(myProject);
    }

    @Override
    protected String getDimensionServiceKey() {
        return getClass().getName();
    }

    @Override
    protected String getHelpId() {
        return "reference.dialogs.createJmh";
    }

    @Override
    protected void doHelpAction() {
        BrowserUtil.browse("http://baidu.com");
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myTargetClassNameField;
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints constr = new GridBagConstraints();

        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.anchor = GridBagConstraints.WEST;

        int gridy = 1;

        constr.insets = insets(4);
        constr.gridy = gridy++;
        constr.gridx = 0;
        constr.weightx = 0;
        // label [jmh library]
        final JLabel libLabel = new JLabel(PromptBundle.message("jmh.library.label.description"));
        libLabel.setLabelFor(myLibrariesCombo);
        panel.add(libLabel, constr);
        // [myLibrariesCombo] [jmh library] x:1 y:1
        constr.gridx = 1;
        constr.weightx = 1;
        constr.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(myLibrariesCombo, constr);

        // when here without a jmh dependency, myFixLibraryPanel show
        myFixLibraryPanel = new JPanel(new BorderLayout());
        myFixLibraryLabel = new JLabel();
        myFixLibraryLabel.setIcon(AllIcons.Actions.IntentionBulb);
        myFixLibraryPanel.add(myFixLibraryLabel, BorderLayout.CENTER);
        myFixLibraryPanel.add(myFixLibraryButton, BorderLayout.EAST);

        constr.insets = insets(1);
        constr.gridy = gridy++;
        constr.gridx = 0;
        panel.add(myFixLibraryPanel, constr);

        constr.gridheight = 1;

        constr.insets = insets(6);
        constr.gridy = gridy++;
        constr.gridx = 0;
        constr.weightx = 0;
        constr.gridwidth = 1;
        // input class name label x:0 y:2
        panel.add(new JLabel(PromptBundle.message("jmh.generate.class.name")), constr);

        myTargetClassNameField = new EditorTextField(suggestTestClassName(myTargetClass));
        myTargetClassNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent e) {
                getOKAction().setEnabled(PsiNameHelper.getInstance(myProject).isIdentifier(getClassName()));
            }
        });

        constr.gridx = 1;
        constr.weightx = 1;
        panel.add(myTargetClassNameField, constr);

        constr.insets = insets(1);
        constr.gridy = gridy++;
        constr.gridx = 0;
        constr.weightx = 0;
        //choose super class location
        panel.add(new JLabel(PromptBundle.message("jmh.target.super.class")), constr);
        // [] ...
        mySuperClassField = new ReferenceEditorComboWithBrowseButton(new MyChooseSuperClassAction(), null, myProject, true,
                JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE, RECENT_SUPERS_KEY);
        mySuperClassField.setMinimumSize(mySuperClassField.getPreferredSize());
        constr.gridx = 1;
        constr.weightx = 1;
        panel.add(mySuperClassField, constr);
        // Destination package label x:0 y 4
        constr.insets = insets(1);
        constr.gridy = gridy++;
        constr.gridx = 0;
        constr.weightx = 0;
        panel.add(new JLabel(PromptBundle.message("jmh.destination.package.label")), constr);

        constr.gridx = 1;
        constr.weightx = 1;

        // [] ...
        String targetPackageName = myTargetPackage != null ? myTargetPackage.getQualifiedName() : "";
        myTargetPackageField = new PackageNameReferenceEditorCombo(targetPackageName,
                myProject, RECENTS_KEY,
                PromptBundle.message("jmh.create.class.package.chooser.title"));

        new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                myTargetPackageField.getButton().doClick();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)),
                myTargetPackageField.getChildComponent());
        JPanel targetPackagePanel = new JPanel(new BorderLayout());
        targetPackagePanel.add(myTargetPackageField, BorderLayout.CENTER);
        panel.add(targetPackagePanel, constr);

        constr.gridx = 0;
        constr.gridy = gridy++;
        constr.weightx = 0;
        //choose super class location
        panel.add(new JLabel(PromptBundle.message("jmh.benchmarkMode.label")), constr);
        constr.gridx = 1;
        constr.weightx = 1;
        panel.add(benchmarkModeCombo, constr);
        prepareBenchmarkModeComboModel();

        constr.gridx = 0;
        constr.gridy = gridy++;
        constr.weightx = 0;
        //choose super class location
        panel.add(new JLabel(PromptBundle.message("jmh.fork.label")), constr);
        forkNumField = new EditorTextField("2");
        generateNumField(panel, constr, forkNumField);

        constr.gridx = 0;
        constr.gridy = gridy++;
        constr.weightx = 0;
        //choose super class location
        panel.add(new JLabel(PromptBundle.message("jmh.warmUp.label")), constr);
        warmUpNumField = new EditorTextField("3");
        generateNumField(panel, constr, warmUpNumField);

        constr.gridx = 0;
        constr.gridy = gridy++;
        constr.weightx = 0;
        //choose super class location
        panel.add(new JLabel(PromptBundle.message("jmh.thread.label")), constr);
        threadNumField = new EditorTextField("8");
        generateNumField(panel, constr, threadNumField);
//        constr.gridx = 1;
//        constr.gridy = gridy++;
//        panel.add(myGenerateBeforeBox, constr);


        constr.insets = insets(6);
        constr.gridy = gridy++;
        constr.gridx = 0;
        constr.weightx = 0;
        panel.add(new JLabel(CodeInsightBundle.message("intention.create.test.dialog.generate")), constr);

        constr.gridx = 1;
        panel.add(myGenerateBeforeBox, constr);

        constr.insets = insets(1);
        constr.gridy = gridy++;
        panel.add(myGenerateAfterBox, constr);

        constr.insets = insets(6);
        constr.gridy = gridy++;
        constr.gridx = 0;
        constr.weightx = 0;
        final JLabel membersLabel = new JLabel(CodeInsightBundle.message("intention.create.test.dialog.select.methods"));
        membersLabel.setLabelFor(myMethodsTable);
        panel.add(membersLabel, constr);

        constr.gridx = 1;
        constr.weightx = 1;
        panel.add(myShowInheritedMethodsBox, constr);

        constr.insets = insets(1, 8);
        constr.gridy = gridy++;
        constr.gridx = 0;
        constr.gridwidth = GridBagConstraints.REMAINDER;
        constr.fill = GridBagConstraints.BOTH;
        constr.weighty = 1;
        panel.add(ScrollPaneFactory.createScrollPane(myMethodsTable), constr);

        myLibrariesCombo.setRenderer(SimpleListCellRenderer.create((label, value, index) -> {
            if (value != null) {
                label.setText(value.getName());
                label.setIcon(value.getIcon());
            }
        }));
        //if there is not test source, the class may create in now location
        final boolean hasTestRoots = !ModuleRootManager.getInstance(myTargetModule).getSourceRoots(JavaModuleSourceRootTypes.TESTS).isEmpty();
        final List<JmhFramework> attachedLibraries = new ArrayList<>();
        final String defaultLibrary = getDefaultLibraryName();
        JmhFramework defaultDescriptor = null;

        final DefaultComboBoxModel<JmhFramework> model = (DefaultComboBoxModel<JmhFramework>)myLibrariesCombo.getModel();

        final List<JmhFramework> descriptors = new SmartList<>(JmhFramework.EXTENSION_NAME.getExtensionList());
        descriptors.sort((d1, d2) -> Comparing.compare(d1.getName(), d2.getName()));

        for (final JmhFramework descriptor : descriptors) {
            model.addElement(descriptor);
            if (hasTestRoots && descriptor.isLibraryAttached(myTargetModule)) {
                attachedLibraries.add(descriptor);
            }

            if (Comparing.equal(defaultLibrary, descriptor.getName())) {
                defaultDescriptor = descriptor;
            }
        }

        //依赖的下拉框选择事件
        myLibrariesCombo.addActionListener(e -> {
            final Object selectedItem = myLibrariesCombo.getSelectedItem();
            if (selectedItem != null) {
                final DumbService dumbService = DumbService.getInstance(myProject);
                dumbService.setAlternativeResolveEnabled(true);
                try {
                    onLibrarySelected((JmhFramework)selectedItem);
                }
                finally {
                    dumbService.setAlternativeResolveEnabled(false);
                }
            }
        });

        if (defaultDescriptor != null && (attachedLibraries.contains(defaultDescriptor) || attachedLibraries.isEmpty())) {
            myLibrariesCombo.setSelectedItem(defaultDescriptor);
        }
        else {
            myLibrariesCombo.setSelectedIndex(0);
        }

        myFixLibraryButton.addActionListener(e -> {
            if (mySelectedFramework instanceof DefaultJmhFramework) {
                ((DefaultJmhFramework)mySelectedFramework).setupLibrary(myTargetModule)
                        .onSuccess(__ -> myFixLibraryPanel.setVisible(false));
            }
            else {
                OrderEntryFix.addJarToRoots(mySelectedFramework.getLibraryPath(), myTargetModule, null);
                myFixLibraryPanel.setVisible(false);
            }
        });



        myShowInheritedMethodsBox.addActionListener(e -> updateMethodsTable());
        restoreShowInheritedMembersStatus();
        updateMethodsTable();
        return panel;
    }

    private void generateNumField(JPanel panel, GridBagConstraints constr, EditorTextField numField) {
        numField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent e) {
                if(numField.getText() != null && !numField.getText().trim().isEmpty()) {
                    getOKAction().setEnabled(numField.getText().matches("\\d+"));
                }
            }
        });
        constr.gridx = 1;
        constr.weightx = 1;
        panel.add(numField, constr);
    }

    private void prepareBenchmarkModeComboModel() {
        benchmarkModeCombo.setRenderer(SimpleListCellRenderer.create((label, value, index) -> {
            if (value != null) {
                label.setText(value);
            }
        }));
        final DefaultComboBoxModel<String> benchmarkModeComboModel = (DefaultComboBoxModel<String>)benchmarkModeCombo.getModel();
        benchmarkModeComboModel.addElement("Mode.Throughput");
        benchmarkModeComboModel.addElement("Mode.AverageTime");
        benchmarkModeComboModel.addElement("Mode.SampleTime");
        benchmarkModeComboModel.addElement("Mode.SingleShotTime");
        benchmarkModeComboModel.addElement("Mode.All");
        //默认选择
        if (defaultMode != null) {
            benchmarkModeCombo.setSelectedItem(defaultMode);
        } else {
            benchmarkModeCombo.setSelectedIndex(0);
        }
    }

    private static Insets insets(int top) {
        return insets(top, 0);
    }

    private static Insets insets(int top, int bottom) {
        return JBUI.insets(top, 8, bottom, 8);
    }

    public String getClassName() {
        return myTargetClassNameField.getText();
    }

    public PsiClass getTargetClass() {
        return myTargetClass;
    }

    @Nullable
    public String getSuperClassName() {
        String result = mySuperClassField.getText().trim();
        if (result.length() == 0) return null;
        return result;
    }

    public PsiDirectory getTargetDirectory() {
        return myTargetDirectory;
    }

    public Collection<MemberInfo> getSelectedMethods() {
        return myMethodsTable.getSelectedMemberInfos();
    }

    public boolean shouldGeneratedAfter() {
        return myGenerateAfterBox.isSelected();
    }

    public boolean shouldGeneratedBefore() {
        return myGenerateBeforeBox.isSelected();
    }

    public JmhFramework getSelectedJmhFrameworkDescriptor() {
        return mySelectedFramework;
    }


    /*
     * ok action
     *
     */
    @Override
    protected void doOKAction() {
        RecentsManager.getInstance(myProject).registerRecentEntry(RECENTS_KEY, myTargetPackageField.getText());
        RecentsManager.getInstance(myProject).registerRecentEntry(RECENT_SUPERS_KEY, mySuperClassField.getText());

        String errorMessage = null;
        try {
            myTargetDirectory = selectTargetDirectory();
            if (myTargetDirectory == null) return;
        }
        catch (IncorrectOperationException e) {
            errorMessage = e.getMessage();
        }

        if (errorMessage == null) {
            try {
                errorMessage = checkCanCreateClass();
            }
            catch (IncorrectOperationException e) {
                errorMessage = e.getMessage();
            }
        }

        if (errorMessage != null) {
            final int result = Messages
                    .showOkCancelDialog(myProject, errorMessage + ". Update existing class?", CommonBundle.getErrorTitle(), Messages.getErrorIcon());
            if (result == Messages.CANCEL) {
                return;
            }
        }

        // save jmh externalProperties
//        getExternalProperties().put("measureIterations", "10");
//        getExternalProperties().put("measureTimes", "5");
        getExternalProperties().put(BENCHMARK_MODE_KEY, String.valueOf(benchmarkModeCombo.getSelectedItem()));
        getExternalProperties().put("warmupIterations", warmUpNumField.getText());
        getExternalProperties().put("threadNum", threadNumField.getText());
        getExternalProperties().put("forkNum", forkNumField.getText());


        saveDefaultLibraryNameAndSuperClass();
        saveShowInheritedMembersStatus();
        super.doOKAction();
    }

    protected String checkCanCreateClass() {
        return RefactoringMessageUtil.checkCanCreateClass(myTargetDirectory, getClassName());
    }

    @Nullable
    private PsiDirectory selectTargetDirectory() throws IncorrectOperationException {
        final String packageName = getPackageName();
        final PackageWrapper targetPackage = new PackageWrapper(PsiManager.getInstance(myProject), packageName);

        final VirtualFile selectedRoot = ReadAction.compute(() -> {
            final List<VirtualFile> testFolders = CreateJmhAction.computeTestRoots(myTargetModule);
            List<VirtualFile> roots;
            if (testFolders.isEmpty()) {
                roots = new ArrayList<>();
                List<String> urls = CreateJmhAction.computeSuitableTestRootUrls(myTargetModule);
                for (String url : urls) {
                    try {
                        ContainerUtil.addIfNotNull(roots, VfsUtil.createDirectories(VfsUtilCore.urlToPath(url)));
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (roots.isEmpty()) {
                    JavaProjectRootsUtil.collectSuitableDestinationSourceRoots(myTargetModule, roots);
                }
                if (roots.isEmpty()) return null;
            }
            else {
                roots = new ArrayList<>(testFolders);
            }

            if (roots.size() == 1) {
                return roots.get(0);
            }
            else {
                PsiDirectory defaultDir = chooseDefaultDirectory(targetPackage.getDirectories(), roots);
                return MoveClassesOrPackagesUtil.chooseSourceRoot(targetPackage, roots, defaultDir);
            }
        });

        if (selectedRoot == null) return null;

        return WriteCommandAction.writeCommandAction(myProject).withName(CodeInsightBundle.message("create.directory.command"))
                .compute(() -> RefactoringUtil.createPackageDirectoryInSourceRoot(targetPackage, selectedRoot));
    }

    @Nullable
    private PsiDirectory chooseDefaultDirectory(PsiDirectory[] directories, List<VirtualFile> roots) {
        List<PsiDirectory> dirs = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(myProject);
        for (VirtualFile file : ModuleRootManager.getInstance(myTargetModule).getSourceRoots(JavaSourceRootType.TEST_SOURCE)) {
            final PsiDirectory dir = psiManager.findDirectory(file);
            if (dir != null) {
                dirs.add(dir);
            }
        }
        if (!dirs.isEmpty()) {
            for (PsiDirectory dir : dirs) {
                final String dirName = dir.getVirtualFile().getPath();
                if (dirName.contains("generated")) continue;
                return dir;
            }
            return dirs.get(0);
        }
        for (PsiDirectory dir : directories) {
            final VirtualFile file = dir.getVirtualFile();
            for (VirtualFile root : roots) {
                if (VfsUtilCore.isAncestor(root, file, false)) {
                    final PsiDirectory rootDir = psiManager.findDirectory(root);
                    if (rootDir != null) {
                        return rootDir;
                    }
                }
            }
        }
        return ModuleManager.getInstance(myProject)
                .getModuleDependentModules(myTargetModule)
                .stream().flatMap(module -> ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.TEST_SOURCE).stream())
                .map(root -> psiManager.findDirectory(root)).findFirst().orElse(null);
    }

    private String getPackageName() {
        String name = myTargetPackageField.getText();
        return name != null ? name.trim() : "";
    }

    public JmhFramework getSelectedTestFrameworkDescriptor() {
        return mySelectedFramework;
    }

    public Map<String, String> getExternalProperties() {
        if(externalProperties == null) {
            externalProperties = new HashMap<>();
            //set default value
            externalProperties.put(BENCHMARK_MODE_KEY, "Mode.Throughput");
            externalProperties.put("warmupIterations", "3");
            externalProperties.put("measureIterations", "10");
            externalProperties.put("measureTimes", "5");
            externalProperties.put("threadNum", "8");
            externalProperties.put("forkNum", "2");
        }
        return externalProperties;
    }

    /*
     *  standard choose action
     */
    private class MyChooseSuperClassAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            TreeClassChooserFactory f = TreeClassChooserFactory.getInstance(myProject);
            TreeClassChooser dialog =
                    f.createAllProjectScopeChooser(CodeInsightBundle.message("intention.create.test.dialog.choose.super.class"));
            dialog.showDialog();
            PsiClass aClass = dialog.getSelected();
            if (aClass != null) {
                String superClass = aClass.getQualifiedName();

                mySuperClassField.appendItem(superClass);
                mySuperClassField.getChildComponent().setSelectedItem(superClass);
            }
        }
    }
}

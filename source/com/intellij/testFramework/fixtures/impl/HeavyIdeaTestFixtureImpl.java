/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.testFramework.fixtures.impl;

import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.idea.IdeaTestApplication;
import com.intellij.lang.properties.PropertiesReferenceManager;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.impl.EditorFactoryImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.builders.ModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mike
 */
class HeavyIdeaTestFixtureImpl extends BaseFixture implements IdeaProjectTestFixture {

  @NonNls private static final String PROJECT_FILE_PREFIX = "temp";
  @NonNls private static final String PROJECT_FILE_SUFFIX = ".ipr";

  private Project myProject;
  private Set<File> myFilesToDelete = new HashSet<File>();
  private IdeaTestApplication myApplication;
  private final List<ModuleFixtureBuilder> myModuleFixtureBuilders = new ArrayList<ModuleFixtureBuilder>();

  protected void addModuleFixtureBuilder(ModuleFixtureBuilder builder) {
    myModuleFixtureBuilders.add(builder);
  }

  public void setUp() throws Exception {
    super.setUp();

    initApplication();
    setUpProject();
  }

  protected void setUpProject() throws Exception {
    ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();

    File projectFile = File.createTempFile(PROJECT_FILE_PREFIX, PROJECT_FILE_SUFFIX);
    myFilesToDelete.add(projectFile);

    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectFile);
    myProject = projectManager.newProject(FileUtil.getNameWithoutExtension(projectFile), projectFile.getPath(), false, false);

    for (ModuleFixtureBuilder moduleFixtureBuilder: myModuleFixtureBuilders) {
      moduleFixtureBuilder.getFixture().setUp();
    }

    PropertiesReferenceManager.getInstance(myProject).projectOpened();

    StartupManagerImpl sm = (StartupManagerImpl)StartupManager.getInstance(myProject);
    sm.runStartupActivities();
    sm.runPostStartupActivities();

    ProjectManagerEx.getInstanceEx().setCurrentTestProject(myProject);
  }


  protected void initApplication() throws Exception {
    myApplication = IdeaTestApplication.getInstance();
    myApplication.setDataProvider(new MyDataProvider());
  }

  public void tearDown() throws Exception {
    ProjectManagerEx.getInstanceEx().setCurrentTestProject(null);
    ApplicationManager.getApplication().runWriteAction(EmptyRunnable.getInstance()); // Flash posponed formatting if any.
    FileDocumentManager.getInstance().saveAllDocuments();

    IdeaTestCase.doPostponedFormatting(myProject);

    Disposer.dispose(myProject);

    for (final File fileToDelete : myFilesToDelete) {
      delete(fileToDelete);
    }

    myApplication.setDataProvider(null);

    EditorFactory editorFactory = EditorFactory.getInstance();
    final Editor[] allEditors = editorFactory.getAllEditors();
    ((EditorFactoryImpl)editorFactory).validateEditorsAreReleased(getProject());
    for (Editor editor : allEditors) {
      editorFactory.releaseEditor(editor);
    }
    assert 0 == editorFactory.getAllEditors().length : "There are unrealeased editors";

    super.tearDown();
  }

  public Project getProject() {
    assert myProject != null : "setUp() should be called first";
    return myProject;
  }

  public Module getModule() {
    Module[] modules = ModuleManager.getInstance(getProject()).getModules();
    return modules.length == 0 ? null : modules[0];
  }

  private class MyDataProvider implements DataProvider {
    @Nullable
    public Object getData(@NonNls String dataId) {
      if (dataId.equals(DataConstants.PROJECT)) {
        return myProject;
      }
      else if (dataId.equals(DataConstants.EDITOR)) {
        return FileEditorManager.getInstance(myProject).getSelectedTextEditor();
      }
      else {
        return null;
      }
    }
  }

  private static void delete(File file) {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      for (File fileToDelete : files) {
        delete(fileToDelete);
      }
    }

    boolean b = file.delete();
    if (!b && file.exists()) {
      throw new IllegalStateException("Can't delete " + file.getAbsolutePath());
    }
  }
}

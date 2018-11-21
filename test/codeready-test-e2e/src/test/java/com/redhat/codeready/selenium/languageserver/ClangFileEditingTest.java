/*
* Copyright (c) 2018 Red Hat, Inc.

* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Red Hat, Inc. - initial API and implementation
*/
package com.redhat.codeready.selenium.languageserver;

import static org.eclipse.che.selenium.core.constant.TestMenuCommandsConstants.Assistant.ASSISTANT;
import static org.eclipse.che.selenium.core.constant.TestMenuCommandsConstants.Assistant.FIND_DEFINITION;
import static org.eclipse.che.selenium.core.constant.TestMenuCommandsConstants.Assistant.Refactoring.LS_RENAME;
import static org.eclipse.che.selenium.core.constant.TestMenuCommandsConstants.Assistant.Refactoring.REFACTORING;
import static org.eclipse.che.selenium.core.project.ProjectTemplates.CPP;
import static org.eclipse.che.selenium.pageobject.CodenvyEditor.ContextMenuLocator.FORMAT;
import static org.eclipse.che.selenium.pageobject.CodenvyEditor.MarkerLocator.ERROR;
import static org.openqa.selenium.Keys.F4;

import com.google.inject.Inject;
import java.net.URL;
import java.nio.file.Paths;
import org.eclipse.che.selenium.core.client.TestProjectServiceClient;
import org.eclipse.che.selenium.core.workspace.InjectTestWorkspace;
import org.eclipse.che.selenium.core.workspace.TestWorkspace;
import org.eclipse.che.selenium.core.workspace.WorkspaceTemplate;
import org.eclipse.che.selenium.pageobject.CodenvyEditor;
import org.eclipse.che.selenium.pageobject.Consoles;
import org.eclipse.che.selenium.pageobject.Ide;
import org.eclipse.che.selenium.pageobject.Menu;
import org.eclipse.che.selenium.pageobject.ProjectExplorer;
import org.openqa.selenium.Keys;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** @author Skoryk Serhii */
public class ClangFileEditingTest {
  private static final String PROJECT_NAME = "console-cpp-simple";
  private static final String CPP_FILE_NAME = "hello.cc";
  private static final String H_FILE_NAME = "iseven.h";
  private static final String PATH_TO_CPP_FILE = PROJECT_NAME + "/" + CPP_FILE_NAME;
  private static final String LS_INIT_MESSAGE =
      "Initialized language server 'org.eclipse.che.plugin.clangd.languageserver'";

  @InjectTestWorkspace(template = WorkspaceTemplate.ECLIPSE_CPP_GCC)
  private TestWorkspace workspace;

  @Inject private Ide ide;
  @Inject private Menu menu;
  @Inject private Consoles consoles;
  @Inject private CodenvyEditor editor;
  @Inject private ProjectExplorer projectExplorer;
  @Inject private TestProjectServiceClient testProjectServiceClient;

  @BeforeClass
  public void setUp() throws Exception {
    URL resource = ClangFileEditingTest.this.getClass().getResource("/projects/console-cpp-simple");
    testProjectServiceClient.importProject(
        workspace.getId(), Paths.get(resource.toURI()), PROJECT_NAME, CPP);

    ide.open(workspace);
    ide.waitOpenedWorkspaceIsReadyToUse();

    projectExplorer.waitAndSelectItem(PROJECT_NAME);
    projectExplorer.openItemByPath(PROJECT_NAME);
    projectExplorer.openItemByPath(PATH_TO_CPP_FILE);
    editor.waitTabIsPresent(CPP_FILE_NAME);
  }

  @Test
  public void checkCodeValidation() {
    projectExplorer.openItemByPath(PATH_TO_CPP_FILE);
    editor.waitActive();

    // make error in code and check error marker with message
    editor.waitAllMarkersInvisibility(ERROR);
    editor.goToCursorPositionVisible(14, 1);
    editor.typeTextIntoEditor("c");
    editor.waitMarkerInPosition(ERROR, 14);
    editor.moveCursorToText("cint");
    editor.waitTextInHoverPopup("unknown type name 'cint'");

    // restore content and check error marker invisibility
    editor.goToCursorPositionVisible(14, 1);
    editor.typeTextIntoEditor(Keys.DELETE.toString());
    editor.waitAllMarkersInvisibility(ERROR);

    // comment line by Ctrl+'/' buttons
    projectExplorer.openItemByPath(PROJECT_NAME + "/hello.cpp");
    editor.waitActive();

    editor.goToPosition(21, 1);
    editor.launchCommentCodeFeature();
    editor.waitTextIntoEditor("//  return 0;");

    // uncomment line by Ctrl+'/' buttons
    editor.launchCommentCodeFeature();
    editor.waitTextIntoEditor("  return 0;");

    // check Signature Help feature
    editor.selectTabByName(CPP_FILE_NAME);
    editor.goToPosition(17, 1);
    editor.deleteCurrentLineAndInsertNew();
    editor.typeTextIntoEditor("  std::abs(");

    editor.waitSignaturesContainer();
    editor.waitProposalIntoSignaturesContainer("abs(int __x) -> int");
    editor.closeSignaturesContainer();
    editor.waitSignaturesContainerIsClosed();

    editor.deleteCurrentLineAndInsertNew();
  }

  @Test
  public void checkAutocompleteFeature() {
    projectExplorer.openItemByPath(PATH_TO_CPP_FILE);
    editor.waitActive();

    // check contents of autocomplete container
    editor.goToPosition(17, 1);
    editor.deleteCurrentLineAndInsertNew();
    editor.typeTextIntoEditor("std::cou");
    editor.launchAutocompleteAndWaitContainer();
    editor.waitProposalIntoAutocompleteContainer("cout ostream");
    editor.waitProposalIntoAutocompleteContainer("wcout wostream");
    editor.closeAutocomplete();

    editor.deleteCurrentLineAndInsertNew();
  }

  @Test
  public void checkFindDefinitionFeature() {
    projectExplorer.openItemByPath(PROJECT_NAME + "/hello.cpp");
    editor.waitActive();

    // check Find Definition feature from Assistant menu
    editor.goToPosition(20, 20);
    menu.runCommand(ASSISTANT, FIND_DEFINITION);
    editor.waitTabIsPresent(H_FILE_NAME);
    editor.clickOnCloseFileIcon(H_FILE_NAME);

    // check Find Definition feature by pressing F4
    editor.selectTabByName("hello.cpp");
    editor.goToPosition(20, 20);
    editor.typeTextIntoEditor(F4.toString());
    editor.waitTabIsPresent(H_FILE_NAME);
  }

  @Test
  public void checkRenameFieldFeature() {
    projectExplorer.openItemByPath(PROJECT_NAME + "/iseven.h");
    editor.waitActive();

    editor.goToCursorPositionVisible(15, 18);
    menu.runCommand(ASSISTANT, REFACTORING, LS_RENAME);
    editor.doRenamingByLanguageServerField("args");
    editor.waitTextIntoEditor("args");

    editor.waitAllMarkersInvisibility(ERROR);
  }

  @Test
  public void checkSelectedCodeFormatFeature() {
    projectExplorer.openItemByPath(PROJECT_NAME + "/hello.cpp");
    editor.waitActive();

    // check selected line formatting
    editor.selectLines(18, 1);
    editor.openContextMenuInEditor();
    editor.clickOnItemInContextMenu(FORMAT);
    editor.waitTextIntoEditor("  int x = 4;");
  }

  @Test
  public void checkFormatCodeFeature() {
    projectExplorer.openItemByPath(PROJECT_NAME + "/iseven.cpp");
    editor.waitActive();

    editor.openContextMenuInEditor();
    editor.clickOnItemInContextMenu(FORMAT);
    editor.waitTextIntoEditor("int isEven(int x) { return x % 2 == 0; }");
  }
}

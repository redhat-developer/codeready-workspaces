/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package com.redhat.codeready.selenium.miscellaneous;

import static org.eclipse.che.commons.lang.NameGenerator.generate;
import static org.eclipse.che.selenium.core.constant.TestMenuCommandsConstants.Assistant.ASSISTANT;
import static org.eclipse.che.selenium.core.constant.TestMenuCommandsConstants.Assistant.FILE_STRUCTURE;
import static org.eclipse.che.selenium.core.project.ProjectTemplates.MAVEN_SPRING;

import com.google.inject.Inject;
import java.net.URL;
import java.nio.file.Paths;
import org.eclipse.che.selenium.core.client.TestProjectServiceClient;
import org.eclipse.che.selenium.core.workspace.TestWorkspace;
import org.eclipse.che.selenium.pageobject.CodenvyEditor;
import org.eclipse.che.selenium.pageobject.Consoles;
import org.eclipse.che.selenium.pageobject.FileStructure;
import org.eclipse.che.selenium.pageobject.Ide;
import org.eclipse.che.selenium.pageobject.Loader;
import org.eclipse.che.selenium.pageobject.Menu;
import org.eclipse.che.selenium.pageobject.ProjectExplorer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** @author Aleksandr Shmaraev on 11.12.15 */
public class FileStructureBaseOperationTest {
  private static final String PROJECT_NAME = generate("project", 4);

  private static final String CLASS_MEMBERS_1 =
      "AppController\n" + "secretNum\n" + "handleRequest(HttpServletRequest, HttpServletResponse)";

  private static final String CLASS_MEMBERS_2 =
      "AppController\n"
          + "secretNum\n"
          + "handleRequest(HttpServletRequest, HttpServletResponse):ModelAndView\n";

  private static final String INHERITED_MEMBERS =
      "Object() - java.lang.Object\n"
          + "registerNatives() - java.lang.Object\n"
          + "getClass() - java.lang.Object\n"
          + "hashCode() - java.lang.Object\n"
          + "equals(...) - java.lang.Object\n"
          + "clone() - java.lang.Object\n"
          + "toString() - java.lang.Object\n"
          + "notify() - java.lang.Object\n"
          + "notifyAll() - java.lang.Object\n"
          + "wait(...) - java.lang.Object\n"
          + "wait(...) - java.lang.Object\n"
          + "wait() - java.lang.Object\n"
          + "finalize() - java.lang.Object\n"
          + "<clinit>() - java.lang.Object";

  @Inject private TestWorkspace workspace;
  @Inject private Ide ide;
  @Inject private ProjectExplorer projectExplorer;
  @Inject private CodenvyEditor editor;
  @Inject private FileStructure fileStructure;
  @Inject private Menu menu;
  @Inject private Loader loader;
  @Inject private TestProjectServiceClient testProjectServiceClient;
  @Inject private Consoles consoles;

  @BeforeClass
  public void setUp() throws Exception {
    URL resource = getClass().getResource("/projects/guess-project");
    testProjectServiceClient.importProject(
        workspace.getId(), Paths.get(resource.toURI()), PROJECT_NAME, MAVEN_SPRING);

    ide.open(workspace);
    consoles.waitJDTLSProjectResolveFinishedMessage(PROJECT_NAME);
  }

  @Test
  public void checkFileStructureBaseOperations() {
    ide.waitOpenedWorkspaceIsReadyToUse();
    projectExplorer.waitItem(PROJECT_NAME);
    projectExplorer.quickExpandWithJavaScript();

    // Check the opening and closing the 'file structure' form
    projectExplorer.openItemByPath(
        PROJECT_NAME + "/src/main/java/org/eclipse/qa/examples/AppController.java");
    editor.waitActive();
    menu.runCommand(ASSISTANT, FILE_STRUCTURE);
    fileStructure.waitFileStructureFormIsOpen("AppController");
    fileStructure.launchFileStructureFormByKeyboard();
    fileStructure.closeFileStructureFormByEscape();

    loader.waitOnClosed();
    fileStructure.waitFileStructureFormIsClosed();
    fileStructure.launchFileStructureFormByKeyboard();
    fileStructure.launchFileStructureFormByKeyboard();
    loader.waitOnClosed();
    fileStructure.waitFileStructureFormIsOpen("AppController");
    fileStructure.clickFileStructureCloseIcon();
    loader.waitOnClosed();
    fileStructure.waitFileStructureFormIsClosed();

    // Show inherited members
    menu.runCommand(ASSISTANT, FILE_STRUCTURE);
    loader.waitOnClosed();
    fileStructure.waitFileStructureFormIsOpen("AppController");
    fileStructure.waitExpectedTextInFileStructure(CLASS_MEMBERS_1);
    fileStructure.waitExpectedTextIsNotPresentInFileStructure(INHERITED_MEMBERS);
    fileStructure.launchFileStructureFormByKeyboard();
    fileStructure.waitExpectedTextInFileStructure(CLASS_MEMBERS_2);
    fileStructure.waitExpectedTextInFileStructure(INHERITED_MEMBERS);
    fileStructure.launchFileStructureFormByKeyboard();
    fileStructure.waitExpectedTextInFileStructure(CLASS_MEMBERS_1);
    fileStructure.waitExpectedTextIsNotPresentInFileStructure(INHERITED_MEMBERS);
    fileStructure.closeFileStructureFormByEscape();
    editor.closeAllTabs();
    loader.waitOnClosed();

    // Check the the 'file structure' is not present in the menu
    projectExplorer.openItemByPath(PROJECT_NAME + "/src/main/webapp/index.jsp");
    editor.waitActive();
    menu.runCommand(ASSISTANT);
    menu.waitCommandIsNotPresentInMenu(FILE_STRUCTURE);
  }
}

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
package com.redhat.codeready.selenium.editor;

import org.eclipse.che.selenium.editor.SplitEditorFeatureTest;

/** @author Aleksandr Shmaraiev */
public class CodeReadySplitEditorFeatureTest extends SplitEditorFeatureTest {

  @Override
  protected String getJavaFileNameFromTabTitle() {
    return "MemberRegistration";
  }

  @Override
  protected void expandProjectExplorerAndOpenFile() {
    String javaFileName = getJavaFileNameFromTabTitle() + ".java";
    String pathTopackage =
        PROJECT_NAME + "/src/main/java/org.jboss.as.quickstarts.kitchensinkjsp/controller";

    projectExplorer.expandPathInProjectExplorerAndOpenFile(pathTopackage, javaFileName);
    editor.waitTabIsPresent(getJavaFileNameFromTabTitle());
    editor.waitTabSelection(0, getJavaFileNameFromTabTitle());
    editor.waitActive();
  }

  @Override
  protected void waitAndSelectItem() {
    String pathToJavaFile =
        PROJECT_NAME
            + "/src/main/java/org/jboss/as/quickstarts/kitchensinkjsp/controller/MemberRegistration.java";
    projectExplorer.waitAndSelectItem(pathToJavaFile);
  }

  @Override
  protected void selectSample() {
    String sampleName = "kitchensink-example";
    wizard.selectSample(sampleName);
  }
}

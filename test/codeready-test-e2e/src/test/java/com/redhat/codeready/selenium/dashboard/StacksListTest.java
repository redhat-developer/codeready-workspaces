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
package com.redhat.codeready.selenium.dashboard;

import static java.lang.String.format;
import static org.eclipse.che.selenium.core.constant.TestStacksConstants.DOTNET;
import static org.eclipse.che.selenium.core.constant.TestStacksConstants.NODE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import org.eclipse.che.selenium.pageobject.dashboard.Dashboard;
import org.eclipse.che.selenium.pageobject.dashboard.stacks.StackDetails;
import org.eclipse.che.selenium.pageobject.dashboard.stacks.Stacks;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** @author Skoryk Serhii */
public class StacksListTest {

  @Inject private StackDetails stackDetails;
  @Inject private Dashboard dashboard;
  @Inject private Stacks stacks;

  @BeforeClass
  public void setUp() {
    dashboard.open();
  }

  @BeforeMethod
  public void openStacksListPage() {
    dashboard.waitDashboardToolbarTitle();
    dashboard.selectStacksItemOnDashboard();
    stacks.waitToolbarTitleName();
  }

  @AfterClass
  public void deleteCreatedStacks() {
    dashboard.selectStacksItemOnDashboard();
    stacks.waitToolbarTitleName();
    stacks.selectAllStacksByBulk();
    deleteStack();
  }

  @Test
  public void checkStacksList() {
    // check UI views of Stacks list
    stacks.waitToolbarTitleName();
    stacks.waitDocumentationLink();
    stacks.waitAddStackButton();
    stacks.waitBuildStackFromRecipeButton();
    stacks.waitFilterStacksField();

    // check that all Stack list headers are present
    ArrayList<String> headers = stacks.getStacksListHeaders();
    assertTrue(headers.contains("NAME"));
    assertTrue(headers.contains("DESCRIPTION"));
    assertTrue(headers.contains("COMPONENTS"));
    assertTrue(headers.contains("ACTIONS"));

    // check Java 1.8 stack info
    assertTrue(stacks.isStackItemExisted("Java 1.8"));
    assertEquals(
        stacks.getStackDescription("Java 1.8"), "Default Java Stack with OpenJDK 1.8, Maven 3.5");
    assertEquals(stacks.getStackComponents("Java 1.8"), "EAP, Maven, OpenJDK, RHEL");
  }

  @Test
  public void checkStacksSelectingByCheckbox() {
    String stackName = createDuplicatedStack("Java 1.8");

    // select stacks by checkbox and check it is selected
    stacks.selectStackByCheckbox(stackName);
    assertTrue(stacks.isStackChecked(stackName));
    stacks.selectStackByCheckbox(stackName);
    assertFalse(stacks.isStackChecked(stackName));

    // click on the Bulk button and check that created stack is checked
    stacks.selectAllStacksByBulk();
    assertTrue(stacks.isStackChecked(stackName));
  }

  @Test
  public void checkStacksFiltering() {
    // filter stacks by nonexistent name
    stacks.typeToSearchInput("*");
    stacks.waitNoStacksFound();

    // search stacks by a full name
    stacks.typeToSearchInput("java");
    assertTrue(stacks.isStackItemExisted("Java 1.8"));
    assertTrue(stacks.isStackItemExisted("Java EAP"));
    assertFalse(stacks.isStackItemExisted(NODE.getName()));

    stacks.typeToSearchInput("node");
    assertTrue(stacks.isStackItemExisted(NODE.getName()));
    assertFalse(stacks.isStackItemExisted("Java EAP"));
    assertFalse(stacks.isStackItemExisted("Java 1.8"));

    // search stacks by a part name
    stacks.typeToSearchInput("ne");
    assertTrue(stacks.isStackItemExisted(DOTNET.getName()));
    assertFalse(stacks.isStackItemExisted("Java EAP"));
    assertFalse(stacks.isStackItemExisted("Java 1.8"));
  }

  @Test
  public void checkStacksSorting() {
    ArrayList<String> stackNamesListBeforeSorting, stackNamesListAfterSorting;
    // click on sort button to initialize it
    stacks.clickOnSortStacksByNameButton();

    // get stacks names list and click on sort stacks button
    stackNamesListBeforeSorting = stacks.getStacksNamesList();
    stacks.clickOnSortStacksByNameButton();

    // check that Stacks list reverted
    stackNamesListAfterSorting = stacks.getStacksNamesList();
    Collections.reverse(stackNamesListBeforeSorting);
    assertEquals(stackNamesListBeforeSorting, stackNamesListAfterSorting);

    stacks.clickOnSortStacksByNameButton();
    stackNamesListBeforeSorting = stacks.getStacksNamesList();
    Collections.reverse(stackNamesListAfterSorting);
    assertEquals(stackNamesListBeforeSorting, stackNamesListAfterSorting);
  }

  @Test
  public void checkStackActionButtons() {
    String stackName;

    // create stack duplicate by Duplicate Stack button
    stackName = createDuplicatedStack("Java 1.8");
    assertTrue(stacks.isDuplicatedStackExisted(stackName));

    // delete stack by the Action delete stack button
    deleteStackByActionDeleteButton(stackName);

    stackName = createDuplicatedStack(NODE.getName());
    assertTrue(stacks.isDuplicatedStackExisted(stackName));
    deleteStackByActionDeleteButton(stackName);
  }

  private void deleteStack() {
    stacks.clickOnDeleteStackButton();
    stacks.clickOnDeleteDialogButton();
    dashboard.waitNotificationMessage("Selected stacks have been successfully removed.");
    dashboard.waitNotificationIsClosed();
  }

  private String createDuplicatedStack(String stack) {
    String createdStackName = "";

    dashboard.selectStacksItemOnDashboard();
    stacks.waitToolbarTitleName();
    stacks.clickOnDuplicateStackButton(stack);

    for (String name : stacks.getStacksNamesList()) {
      if (name.contains(stack + "-copy-")) {
        createdStackName = name;
      }
    }

    return createdStackName;
  }

  private void deleteStackByActionDeleteButton(String name) {
    // delete stack by the Action delete stack button
    stacks.clickOnDeleteActionButton(name);
    stacks.clickOnDeleteDialogButton();
    dashboard.waitNotificationMessage(format("Stack %s has been successfully removed.", name));
    dashboard.waitNotificationIsClosed();
    assertFalse(stacks.isStackItemExisted(name));
  }
}

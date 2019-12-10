/*
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package com.redhat.codeready.selenium.pageobject.ocp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.provider.OpenShiftWebConsoleUrlProvider;
import org.eclipse.che.selenium.core.webdriver.SeleniumWebDriverHelper;
import org.eclipse.che.selenium.pageobject.ocp.OpenShiftProjectCatalogPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

@Singleton
public class CodenvyOpenShiftProjectCatalogPage extends OpenShiftProjectCatalogPage {
  private final SeleniumWebDriverHelper seleniumWebDriverHelper;
  private final SeleniumWebDriver seleniumWebDriver;
  private final OpenShiftWebConsoleUrlProvider openShiftWebConsoleUrlProvider;

  @FindBy(xpath = Locators.TITLE_XPATH)
  private WebElement title;

  @Inject
  public CodenvyOpenShiftProjectCatalogPage(
      SeleniumWebDriver seleniumWebDriver,
      SeleniumWebDriverHelper seleniumWebDriverHelper,
      OpenShiftWebConsoleUrlProvider openShiftWebConsoleUrlProvider) {
    super(seleniumWebDriver, seleniumWebDriverHelper, openShiftWebConsoleUrlProvider);
    this.seleniumWebDriver = seleniumWebDriver;
    this.seleniumWebDriverHelper = seleniumWebDriverHelper;
    this.openShiftWebConsoleUrlProvider = openShiftWebConsoleUrlProvider;

    PageFactory.initElements(seleniumWebDriver, this);
  }

  public void waitProject(String projectNamePart, int timeout) {
    waitOnOpen(timeout);
    String projectItemXpath = String.format(Locators.PROJECT_ITEM_XPATH_TEMPLATE, projectNamePart);
    seleniumWebDriverHelper.waitVisibility(By.xpath(projectItemXpath), timeout);
  }

  public void waitProjectAbsence(String projectNamePart, int timeout) {
    waitOnOpen(timeout);
    String projectItemXpath = String.format(Locators.PROJECT_ITEM_XPATH_TEMPLATE, projectNamePart);
    seleniumWebDriverHelper.waitInvisibility(By.xpath(projectItemXpath), timeout);
  }

  private void waitOnOpen(int timeout) {
    seleniumWebDriverHelper.waitVisibility(title, timeout);
  }
}

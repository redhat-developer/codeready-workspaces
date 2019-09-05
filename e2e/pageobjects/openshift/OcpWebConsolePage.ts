/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import 'reflect-metadata';
import { injectable, inject } from 'inversify';
import { DriverHelper } from '../../utils/DriverHelper';
import { CLASSES } from '../../inversify.types';
import { TestConstants } from '../../TestConstants';
import { By } from 'selenium-webdriver';

@injectable()
export class OcpWebConsolePage {

    private static readonly CODEREADY_OPERATOR_LOGO_NAME: string = '//h1[text()=\'Red Hat CodeReady Workspaces\']';
    private static readonly CODEREADY_PREFIX_URL: string = 'codeready-';

    constructor(
        @inject(CLASSES.DriverHelper) private readonly driverHelper: DriverHelper) { }

    async waitNavpanelOpenShift () {
        const navPanelOpenShiftLocator: By = By.css('nav[class=pf-c-nav]');
        await this.driverHelper.waitVisibility(navPanelOpenShiftLocator);
    }

    async clickOnCatalogListNavPanelOpenShift () {
        const catalogListLocator: By = By.xpath('//a[text()=\'Catalog\']');
        await this.driverHelper.waitAndClick(catalogListLocator);
    }

    async clickOnOperatorHubItemNavPanel () {
        const operatorHubItemLocator: By = By.xpath('//a[text()=\'OperatorHub\']');
        await this.driverHelper.waitAndClick(operatorHubItemLocator);
    }

    async waitOperatorHubMainPage () {
        const catalogOperatorHubPageLocator: By = By.xpath('//span[@id=\'resource-title\' and text()=\'OperatorHub\']');
        await this.driverHelper.waitVisibility(catalogOperatorHubPageLocator);
    }

    async clickOnCodeReadyWorkspacesOperatorIcon (codeReadyOperatorTitle: string) {
        const catalogCrwOperatorTitleLocator: By = By.css(`a[data-test^=${codeReadyOperatorTitle}-openshift]`);
        await this.driverHelper.waitAndClick(catalogCrwOperatorTitleLocator, TestConstants.TS_SELENIUM_LOAD_PAGE_TIMEOUT);
    }
    async clickOnInstallCodeReadyWorkspacesButton () {
        const installCrwOperatorButtonLocator: By = By.xpath('//button[text()=\'Install\']');
        await this.driverHelper.waitAndClick(installCrwOperatorButtonLocator);
    }

    async waitCreateOperatorSubscriptionPage () {
        const createOperatorSubscriptionPageLocator: By = By.xpath('//h1[text()=\'Create Operator Subscription\']');
        await this.driverHelper.waitVisibility(createOperatorSubscriptionPageLocator);
    }

    async clickOnDropdownNamespaceListOnSubscriptionPage () {
        const selectNamespaceLocator: By = By.id('dropdown-selectbox');
        await this.driverHelper.waitAndClick(selectNamespaceLocator);
    }

    async waitListBoxNamespacesOnSubscriptionPage () {
        const listBoxNamespaceLocator: By = By.css('ul[class^=dropdown-menu]');
        await this.driverHelper.waitVisibility(listBoxNamespaceLocator);
    }

    async selectDefinedNamespaceOnSubscriptionPage (projectName: string) {
        const namespaceItemInDropDownLocator: By = By.id(`${projectName}-Project-link`);
        await this.driverHelper.waitAndClick(namespaceItemInDropDownLocator);
    }

    async clickOnSubscribeButtonOnSubscriptionPage () {
        const subscribeOperatorButtonLocator: By = By.xpath('//button[text()=\'Subscribe\']');
        await this.driverHelper.waitAndClick(subscribeOperatorButtonLocator);
    }

    async waitSubscriptionOverviewPage () {
        const subscriptionOverviewPageLocator: By = By.xpath('//h2[text()=\'Subscription Overview\']');
        await this.driverHelper.waitVisibility(subscriptionOverviewPageLocator);
    }

    async waitUpgradeStatusOnSubscriptionOverviewPage () {
        const upgradeStatuslocator: By = By.xpath('//span[text()=\' Up to date\']');
        await this.driverHelper.waitVisibility(upgradeStatuslocator, TestConstants.TS_SELENIUM_LOAD_PAGE_TIMEOUT);
    }

    async waitCatalogSourceNameOnSubscriptionOverviewPage (catalogSourceName: string, projectName: string) {
        const catalogSourceNameLolcator: By = By.css(`a[title=\'${catalogSourceName}-${projectName}\']`);
        await this.driverHelper.waitVisibility(catalogSourceNameLolcator);
    }

    async selectInstalledOperatorsOnNavPanel () {
        const installedOperatorsItemNavPanelLocator: By = By.xpath('//a[text()=\'Installed Operators\']');
        await this.driverHelper.waitAndClick(installedOperatorsItemNavPanelLocator);
    }

    async waitCodeReadyWorkspacesOperatorLogoName () {
        await this.driverHelper.waitVisibility(By.xpath(OcpWebConsolePage.CODEREADY_OPERATOR_LOGO_NAME));
    }

    async waitStatusInstalledCodeReadyWorkspacesOperator () {
        const statusInstalledCrwOperatorLocator: By = By.xpath('//span[text()=\'InstallSucceeded\']');
        await this.driverHelper.waitVisibility(statusInstalledCrwOperatorLocator, TestConstants.TS_SELENIUM_LOAD_PAGE_TIMEOUT);
    }

    async clickOnCodeReadyWorkspacesOperatorLogoName () {
        await this.driverHelper.waitAndClick(By.xpath(OcpWebConsolePage.CODEREADY_OPERATOR_LOGO_NAME));
    }

    async waitOverviewCsvCrwOperator () {
        await this.driverHelper.waitVisibility(By.xpath(OcpWebConsolePage.CODEREADY_OPERATOR_LOGO_NAME));
    }

    async clickCreateNewCheClusterLink () {
        const createNewCheLusterLinkLocator: By = By.xpath('//a[text()=\' Create New\']');
        await this.driverHelper.waitAndClick(createNewCheLusterLinkLocator);
    }

    async waitCreateCheClusterYaml () {
        const createCheClusterYamlLocator: By = By.xpath('//h1[text()=\'Create Che Cluster\']');
        await this.driverHelper.waitVisibility(createCheClusterYamlLocator);
    }

    async clickOnCreateCheClusterButton () {
        const createCheClusterButtonLocator: By = By.id('save-changes');
        await this.driverHelper.waitAndClick(createCheClusterButtonLocator);
    }

    async waitResourcesCheClusterTitle () {
        const resourcesCheClusterTitleLocator: By = By.id('resource-title');
        await this.driverHelper.waitVisibility(resourcesCheClusterTitleLocator);
    }

    async waitResourcesCheClusterTimestamp () {
        const resourcesCheClusterTimestampLocator: By = By.xpath('//div[contains(@class, \'timestamp\')]/div[text()=\'a minute ago\']');
        await this.driverHelper.waitVisibility(resourcesCheClusterTimestampLocator, TestConstants.TS_SELENIUM_LOAD_PAGE_TIMEOUT);
    }
    async clickOnCheClusterResourcesName () {
        const cheClusterResourcesNameLocator: By = By.css('a[class=co-resource-item__resource-name]');
        await this.driverHelper.waitAndClick(cheClusterResourcesNameLocator);
    }

    async clickCheClusterOverviewExpandButton () {
        const cheClusterOverviewExpandButton: By = By.css('label[class=\'btn compaction-btn btn-default\']');
        await this.driverHelper.waitAndClick(cheClusterOverviewExpandButton);
    }

    async waitKeycloakAdminConsoleUrl (projectName: string) {
        const keyCloakAdminWebConsoleUrl: By = By.partialLinkText(`keycloak-${projectName}`);
        await this.driverHelper.waitVisibility(keyCloakAdminWebConsoleUrl, TestConstants.TS_SELENIUM_INSTALL_CODEREADY_WORKSPACES_TIMEOUT);
    }

    async waitCodeReadyWorkspacesUrl (projectName: string) {
        const eclipseCheUrlLocator: By = By.partialLinkText(`${OcpWebConsolePage.CODEREADY_PREFIX_URL}${projectName}`);
        await this.driverHelper.waitVisibility(eclipseCheUrlLocator, TestConstants.TS_SELENIUM_INSTALL_CODEREADY_WORKSPACES_TIMEOUT);
    }

    async clickOnCodeReadyWorkspacesUrl (projectName: string) {
        const eclipseCheUrlLocator: By = By.partialLinkText(`${OcpWebConsolePage.CODEREADY_PREFIX_URL}${projectName}`);
        await this.driverHelper.waitAndClick(eclipseCheUrlLocator);
    }
}

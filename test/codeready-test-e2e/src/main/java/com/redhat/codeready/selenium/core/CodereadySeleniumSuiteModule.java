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
package com.redhat.codeready.selenium.core;

import com.google.inject.AbstractModule;
import com.redhat.codeready.selenium.core.client.keycloak.cli.CodeReadyOpenShiftKeycloakCliCommandExecutor;
import com.redhat.codeready.selenium.core.executor.hotupdate.CodeReadyHotUpdateUtil;
import com.redhat.codeready.selenium.core.utils.CodeReadyWorkspaceDtoDeserializer;
import org.eclipse.che.selenium.core.CheSeleniumSuiteModule;
import org.eclipse.che.selenium.core.client.keycloak.cli.OpenShiftKeycloakCliCommandExecutor;
import org.eclipse.che.selenium.core.executor.hotupdate.HotUpdateUtil;
import org.eclipse.che.selenium.core.utils.WorkspaceDtoDeserializer;

/**
 * Guice module per suite.
 *
 * @author Dmytro Nochevnov
 */
public class CodereadySeleniumSuiteModule extends AbstractModule {

  @Override
  public void configure() {
    bind(OpenShiftKeycloakCliCommandExecutor.class)
        .to(CodeReadyOpenShiftKeycloakCliCommandExecutor.class);

    install(new CheSeleniumSuiteModule());

    bind(HotUpdateUtil.class).to(CodeReadyHotUpdateUtil.class);
    bind(WorkspaceDtoDeserializer.class).to(CodeReadyWorkspaceDtoDeserializer.class);
  }
}

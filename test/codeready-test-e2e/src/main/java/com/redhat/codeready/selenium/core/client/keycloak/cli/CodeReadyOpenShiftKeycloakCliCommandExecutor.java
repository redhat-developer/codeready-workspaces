/*
* Copyright (c) 2019 Red Hat, Inc.

* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v2.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Red Hat, Inc. - initial API and implementation
*/
package com.redhat.codeready.selenium.core.client.keycloak.cli;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import org.eclipse.che.selenium.core.client.keycloak.cli.OpenShiftKeycloakCliCommandExecutor;
import org.eclipse.che.selenium.core.executor.OpenShiftCliCommandExecutor;

/**
 * This class is aimed to call Keycloak CLI commands inside OpenShift pod.
 *
 * @author Dmytro Nochevnov
 */
@Singleton
public class CodeReadyOpenShiftKeycloakCliCommandExecutor
    extends OpenShiftKeycloakCliCommandExecutor {
  private static final String DEFAULT_CHE_OPENSHIFT_PROJECT = "eclipse-che";
  private static final String DEFAULT_KEYCLOAK_APP = "keycloak";
  private static final String DEFAULT_INTERNAL_PATH_TO_KEYCLOAK_CLI =
      "/opt/jboss/keycloak/bin/kcadm.sh";

  private String keycloakPodName;

  @Inject private OpenShiftCliCommandExecutor openShiftCliCommandExecutor;

  @Inject(optional = true)
  @Named("che.openshift.project")
  private String cheOpenshiftProject;

  @Inject(optional = true)
  @Named("env.keycloak.openshift.app")
  private String keycloakApp;

  @Inject(optional = true)
  @Named("env.keycloak.cli.internal.path")
  private String internalPathToKeycloakCli;

  @Override
  public String execute(String command) throws IOException {
    if (keycloakPodName == null || keycloakPodName.trim().isEmpty()) {
      obtainKeycloakPodName();
    }

    String openShiftKeycloakCliCommand =
        format(
            "exec %s -- %s %s",
            keycloakPodName,
            internalPathToKeycloakCli != null
                ? internalPathToKeycloakCli
                : DEFAULT_INTERNAL_PATH_TO_KEYCLOAK_CLI,
            command);

    return openShiftCliCommandExecutor.execute(openShiftKeycloakCliCommand);
  }

  private void obtainKeycloakPodName() throws IOException {
    // obtain name of keycloak pod
    String getKeycloakPodNameCommand =
        format(
            "get pod --namespace=%s -l app=%s --no-headers | grep 'keycloak' | awk '{print $1}'",
            cheOpenshiftProject != null ? cheOpenshiftProject : DEFAULT_CHE_OPENSHIFT_PROJECT,
            keycloakApp != null ? keycloakApp : DEFAULT_KEYCLOAK_APP);

    keycloakPodName = openShiftCliCommandExecutor.execute(getKeycloakPodNameCommand);

    if (keycloakPodName.trim().isEmpty()) {
      String errorMessage =
          format(
              "Keycloak pod is not found at project %s at OpenShift instance.",
              cheOpenshiftProject != null ? cheOpenshiftProject : DEFAULT_CHE_OPENSHIFT_PROJECT);

      throw new RuntimeException(errorMessage);
    }
  }
}

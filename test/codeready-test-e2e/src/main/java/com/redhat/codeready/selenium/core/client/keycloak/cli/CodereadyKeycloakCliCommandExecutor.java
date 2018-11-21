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
package com.redhat.codeready.selenium.core.client.keycloak.cli;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import org.eclipse.che.selenium.core.client.keycloak.cli.KeycloakCliCommandExecutor;
import org.eclipse.che.selenium.core.executor.OpenShiftCliCommandExecutor;

/**
 * This class is aimed to call Keycloak CLI commands inside OpenShift pod.
 *
 * @author Dmytro Nochevnov
 */
@Singleton
public class CodereadyKeycloakCliCommandExecutor implements KeycloakCliCommandExecutor {
  private static final String DEFAULT_OPENSHIFT_CHE_NAMESPACE = "codeready";

  private String keycloakPodName;

  @Inject private OpenShiftCliCommandExecutor openShiftCliCommandExecutor;

  @Inject(optional = true)
  @Named("che.openshift.project")
  private String codereadyNamespace;

  @Override
  public String execute(String command) throws IOException {
    if (keycloakPodName == null || keycloakPodName.trim().isEmpty()) {
      obtainKeycloakPodName();
    }

    String openShiftKeycloakCliCommand =
        format("exec %s -- /opt/eap/bin/kcadm.sh %s", keycloakPodName, command);

    return openShiftCliCommandExecutor.execute(openShiftKeycloakCliCommand);
  }

  private void obtainKeycloakPodName() throws IOException {
    // obtain name of keycloak pod
    String getKeycloakPodNameCommand =
        format(
            "get pod --namespace=%s -l app=rh-sso --no-headers | awk '{print $1}'",
            codereadyNamespace != null ? codereadyNamespace : DEFAULT_OPENSHIFT_CHE_NAMESPACE);

    keycloakPodName = openShiftCliCommandExecutor.execute(getKeycloakPodNameCommand);

    if (keycloakPodName.trim().isEmpty()) {
      String errorMessage =
          format(
              "Keycloak pod is not found at namespace %s at OpenShift instance.",
              codereadyNamespace != null ? codereadyNamespace : DEFAULT_OPENSHIFT_CHE_NAMESPACE);

      throw new RuntimeException(errorMessage);
    }
  }
}

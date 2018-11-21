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
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import org.eclipse.che.selenium.core.client.keycloak.cli.KeycloakCliClient;
import org.eclipse.che.selenium.core.client.keycloak.cli.KeycloakCliCommandExecutor;
import org.eclipse.che.selenium.core.provider.AdminTestUserProvider;
import org.eclipse.che.selenium.core.provider.RemovableUserProvider;
import org.eclipse.che.selenium.core.user.AdminTestUser;
import org.eclipse.che.selenium.core.user.DefaultTestUser;
import org.eclipse.che.selenium.core.user.TestUser;
import org.eclipse.che.selenium.core.user.TestUserFactory;
import org.eclipse.che.selenium.core.user.TestUserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is java-client of 'keycloak/bin/kcadm.sh' command line application
 *
 * @author Dmytro Nochevnov
 */
@Singleton
public class CodereadyKeycloakCliClient {
  private static final Logger LOG = LoggerFactory.getLogger(KeycloakCliClient.class);
  private static final Pattern EXTRACT_USER_ID_PATTERN =
      Pattern.compile("^.*Created new user with id '(.*)'.*$", Pattern.DOTALL);

  // we need to inject AdminTestUser separately to avoid circular dependency error
  @Inject private AdminTestUserProvider adminTestUserProvider;

  private final TestUserFactory<DefaultTestUser> defaultTestUserFactory;
  private final TestUserFactory<TestUserImpl> testUserFactory;

  @Inject private KeycloakCliCommandExecutor executor;

  @Inject
  public CodereadyKeycloakCliClient(
      TestUserFactory<TestUserImpl> testUserFactory,
      TestUserFactory<DefaultTestUser> defaultTestUserFactory) {
    this.testUserFactory = testUserFactory;
    this.defaultTestUserFactory = defaultTestUserFactory;
  }

  public TestUserImpl createUser(RemovableUserProvider testUserProvider) throws IOException {
    long currentTimeInMillisec = System.currentTimeMillis();
    String username = "user" + currentTimeInMillisec;
    String email = username + "@1.com";
    String password = String.valueOf(currentTimeInMillisec);

    String userId = doCreateUser(username, email, password);

    LOG.info("Test user with name='{}' and id='{}' has been created.", username, userId);

    return testUserFactory.create(username, email, password, testUserProvider);
  }

  public DefaultTestUser createDefaultUser(RemovableUserProvider testUserProvider)
      throws IOException {
    long currentTimeInMillisec = System.currentTimeMillis();
    String username = "user" + currentTimeInMillisec;
    String email = username + "@1.com";
    String password = String.valueOf(currentTimeInMillisec);

    String userId = doCreateUser(username, email, password);

    LOG.info("Default test user with name='{}' and id='{}' has been created.", username, userId);

    return defaultTestUserFactory.create(username, email, password, testUserProvider);
  }

  private String doCreateUser(String username, String email, String password) throws IOException {
    String authPartOfCommand =
        format(
            "--no-config --server http://0.0.0.0:8080/auth --user %s --password %s --realm master",
            adminTestUserProvider.get().getName(), adminTestUserProvider.get().getPassword());

    String createUserCommand =
        format(
            "create users -r codeready -s username=%s -s enabled=true %s 2>&1",
            username, authPartOfCommand);
    String response = executor.execute(createUserCommand);
    if (!response.contains("Created new user with id ")) {
      throw new IOException("Test user creation error: " + response);
    }

    String userId = extractUserId(response);

    try {
      String setTestUsersPermanentPasswordCommand =
          format(
              "set-password -r codeready --username %s --new-password %s %s 2>&1",
              username, password, authPartOfCommand);
      executor.execute(setTestUsersPermanentPasswordCommand);

      String setEmailCommand =
          format(
              "update users/%s -r codeready --set email=%s %s 2>&1",
              userId, email, authPartOfCommand);
      executor.execute(setEmailCommand);

      String addUserRoleToUserCommand =
          format(
              "add-roles -r codeready --uusername %s --rolename user %s 2>&1",
              username, authPartOfCommand);
      executor.execute(addUserRoleToUserCommand);

      String addReadTokenRoleToUserCommand =
          format(
              "add-roles -r codeready --uusername %s --cclientid broker --rolename read-token %s 2>&1",
              username, authPartOfCommand);
      executor.execute(addReadTokenRoleToUserCommand);
    } catch (IOException e) {
      // clean up user
      delete(userId, username);
      throw e;
    }

    return userId;
  }

  /** Adds role "read-token" of client "broker" to admin user */
  public void setupAdmin(AdminTestUser adminTestUser) {
    String authPartOfCommand =
        format(
            "--no-config --server http://0.0.0.0:8080/auth --user %s --password %s --realm master",
            adminTestUser.getName(), adminTestUser.getPassword());

    String addReadTokenRoleToUserCommand =
        format(
            "add-roles -r codeready --uusername %s --cclientid broker --rolename read-token %s 2>&1",
            adminTestUser.getName(), authPartOfCommand);

    try {
      executor.execute(addReadTokenRoleToUserCommand);
    } catch (IOException e) {
      // ignore error of adding role to admin because of it can be added before
    }
  }

  public void delete(TestUser testUser) throws IOException {
    delete(testUser.getId(), testUser.getName());
  }

  private String extractUserId(String response) {
    Matcher matcher = EXTRACT_USER_ID_PATTERN.matcher(response);

    if (matcher.find()) {
      return matcher.group(1);
    }

    throw new RuntimeException(
        String.format("User id wasn't found in create user response '%s'.", response));
  }

  private void delete(String userId, String username) throws IOException {
    String commandToDeleteUser =
        format(
            "delete users/%s -r codeready -s username=%s --no-config --server http://0.0.0.0:8080/auth --user %s --password %s --realm master 2>&1",
            userId,
            username,
            adminTestUserProvider.get().getName(),
            adminTestUserProvider.get().getPassword());

    executor.execute(commandToDeleteUser);

    LOG.info("Test user with name='{}' has been removed.", username);
  }
}

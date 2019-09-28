# Copyright (c) 2018 Red Hat, Inc.
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#   Red Hat, Inc. - initial API and implementation

###
# Builder Image
#
FROM eclipse/che-theia-dev:ubi as builder

ENV NODE_OPTIONS="--max-old-space-size=4096"

# add previously checked out theia and che-theia sources (in subdir) 
ADD theia-source-code ${HOME}/theia-source-code

# Add patches if any
# ADD src/patches ${HOME}/patches

# # Apply patches
# RUN if [ -d "${HOME}/patches/${THEIA_VERSION}" ]; then \
#       echo "Applying patches for Theia version ${THEIA_VERSION}"; \
#       for file in $(find "${HOME}/patches/${THEIA_VERSION}" -name '*.patch'); do \
#         echo "Patching with ${file}"; \
#         cd ${HOME}/theia-source-code && patch -p1 < ${file}; \
#       done \
#     fi

#TODO what are these for?
ARG CDN_PREFIX=""
ARG MONACO_CDN_PREFIX=""

# Generate che-theia
WORKDIR ${HOME}/theia-source-code

# run che:theia init command and alias che-theia repository to use local sources insted of cloning
RUN che:theia init -c ${HOME}/theia-source-code/che-theia/che-theia-init-sources.yml --alias https://github.com/eclipse/che-theia=${HOME}/theia-source-code/che-theia

RUN che:theia cdn --theia="${CDN_PREFIX}" --monaco="${MONACO_CDN_PREFIX}"

# Compile Theia

# Unset GITHUB_TOKEN environment variable if it is empty.
# This is needed for some tools which use this variable and will fail with 401 Unauthorized error if it is invalid.
# For example, vscode ripgrep downloading is an example of such case.
RUN if [ -z $GITHUB_TOKEN ]; then unset GITHUB_TOKEN; fi && \
    yarn

# Run into production mode
RUN che:theia production

# Compile plugins
RUN if [ -z $GITHUB_TOKEN ]; then unset GITHUB_TOKEN; fi && \
    cd plugins && ./foreach_yarn

# change permissions
RUN find production -exec sh -c "chgrp 0 {}; chmod g+rwX {}" \; 2>log.txt


################# 
# PHASE TWO: runtime image
################# 

# https://access.redhat.com/containers/?tab=tags#/registry.access.redhat.com/ubi8/nodejs-10
FROM registry.access.redhat.com/ubi8/nodejs-10:1-41
ENV SUMMARY="Red Hat CodeReady Workspaces - Theia container" \
    DESCRIPTION="Red Hat CodeReady Workspaces - Theia container" \
    PRODNAME="codeready-workspaces" \
    COMPNAME="theia-rhel8"

LABEL summary="$SUMMARY" \
      description="$DESCRIPTION" \
      io.k8s.description="$DESCRIPTION" \
      io.k8s.display-name="$DESCRIPTION" \
      io.openshift.tags="$PRODNAME,$COMPNAME" \
      com.redhat.component="$PRODNAME-$COMPNAME-container" \
      name="$PRODNAME/$COMPNAME" \
      version="2.0" \
      license="EPLv2" \
      maintainer="Nick Boldt <nboldt@redhat.com>" \
      io.openshift.expose-services="" \
      usage=""

USER root

ENV USE_LOCAL_GIT=true \
    HOME=/home/theia \
    THEIA_DEFAULT_PLUGINS=local-dir:///default-theia-plugins \
    # Specify the directory of git (avoid to search at init of Theia)
    LOCAL_GIT_DIRECTORY=/usr \
    GIT_EXEC_PATH=/usr/libexec/git-core \
    # Ignore from port plugin the default hosted mode port
    PORT_PLUGIN_EXCLUDE_3130=TRUE \
    THEIA_YEOMAN_PLUGIN="https://github.com/eclipse/theia-yeoman-plugin/releases/download/untagged-c11870b25a17d20bb7a7/theia_yeoman_plugin.theia" \
    VSCODE_GIT="https://github.com/che-incubator/vscode-git/releases/download/1.30.1/vscode-git-1.3.0.1.vsix"

COPY --from=builder /home/theia-dev/theia-source-code/production/plugins /default-theia-plugins

EXPOSE 3100 3130

RUN yum install -y sudo git bzip2 which bash curl openssh less && \
    userdel default && useradd -u 1001 -G root -d ${HOME} -s /bin/sh theia \
    && echo "%wheel ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers \
    # Create /projects for Che
    && mkdir /projects \
    # Create root node_modules in order to not use node_modules in each project folder
    && mkdir /node_modules \
    # Download yeoman generator plug-in
    && echo "Fetch THEIA_YEOMAN_PLUGIN = ${THEIA_YEOMAN_PLUGIN}" \
    && curl -sSL -o /default-theia-plugins/theia_yeoman_plugin.theia ${theia_yeoman_plugin} \
    # Download vscode git plug-in
    && echo "Fetch VSCODE_GIT = ${VSCODE_GIT}" \
    && curl -sSL -o /default-theia-plugins/vscode-git-1.3.0.1.vsix ${VSCODE_GIT} \
    && for f in "${HOME}" "/etc/passwd" "/etc/group /node_modules /default-theia-plugins /projects"; do\
           sudo chgrp -R 0 ${f} && \
           sudo chmod -R g+rwX ${f}; \
       done \
    && cat /etc/passwd | sed s#root:x.*#root:x:\${USER_ID}:\${GROUP_ID}::\${HOME}:/bin/bash#g > ${HOME}/passwd.template \
    && cat /etc/group | sed s#root:x:0:#root:x:0:0,\${USER_ID}:#g > ${HOME}/group.template \
    # Add yeoman, theia plugin generator and typescript (to have tsc/typescript working)
    # TODO why use @theia/generator-plugin@0.0.1-1562578105 when version in theia-dev is different?
    && cd $HOME && npm install -g yarn && yarn global add yo @theia/generator-plugin typescript@2.9.2 \
    && mkdir -p ${HOME}/.config/insight-nodejs/ \
    && chmod -R 777 ${HOME}/.config/ \
    # Disable the statistics for yeoman
    && echo '{"optOut": true}' > $HOME/.config/insight-nodejs/insight-yo.json \
    # Link yarn global modules for yeoman
    && mv /usr/lib/node_modules/* /usr/local/share/.config/yarn/global/node_modules \
    && rm -rf /usr/lib/node_modules && ln -s /usr/local/share/.config/yarn/global/node_modules /usr/local/lib/ \
    # Cleanup tmp folder
    && rm -rf /tmp/* \
    # Cleanup yarn cache
    && yarn cache clean \
    # Change permissions to allow editing of files for openshift user
    && find ${HOME} -exec sh -c "chgrp 0 {}; chmod g+rwX {}" \;

# TODO collect deps in /usr/local/share/.config/yarn/global/node_modules to tarball for Brew

COPY --chown=theia:root --from=builder /home/theia-dev/theia-source-code/production /home/theia
USER theia
WORKDIR /projects
ADD src/entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]

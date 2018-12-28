#!/bin/bash -xe
# script to build eclipse-che in #projectncl

#set version & compute qualifier from best available in Indy
# or use commandline overrides for version and suffix
version=6.14.1

# TODO NOS-1485 build this in NCL directly
# don't build dashboard from source; include from upstream binary in http://oss.sonatype.org/content/repositories/snapshots/
includeDashboardFromSource=0

# to build che
suffix="" # normally we compute this from version of org/eclipse/che/depmgt/maven-depmgt-pom but can override if needed
upstreamPom="org/eclipse/che/depmgt/maven-depmgt-pom" # usually use depmgt/maven-depmgt-pom but can also align to org/eclipse/che/parent/maven-parent-pom for codeready-workspaces build

# to build crw
isWorkspacesBuild=0 # set to 1 for workspaces build (shortcut to enable upstreamPom2)
suffix2="" # normally we compute this from version of org/eclipse/che/assembly-main but can override if needed
upstreamPom2="" # eg., org/eclipse/che/assembly-main

doSedReplacements=1

##########################################################################################
# enable support for CI builds
##########################################################################################

INDY=""
# read commandline args
while [[ "$#" -gt 0 ]]; do
  case $1 in
    '-crw') isWorkspacesBuild=1; upstreamPom2="org/eclipse/che/depmgt/maven-depmgt-pom"; upstreamPom="org/eclipse/che/assembly-main"; shift 0;;
    '-v') version="$2"; shift 1;; #eg., 6.12.0
    '-s') suffix="$2"; shift 1;; # eg., redhat-00007
    '-s2') suffix2="$2"; shift 1;; # eg., redhat-00007
    '-lsjdtv') lsjdtVersion="$2"; shift 1;; # eg., 0.0.2 or 0.0.2-SNAPSHOT
    '-dv') dashboardVersion="$2"; includeDashboardFromSource=0;  shift 1;; # eg., 6.11.1 or 6.13.0-SNAPSHOT; use "NO" to exclude dashboard (NOS-1485: test building it instead of including it)
    '-idfs') includeDashboardFromSource=1; dashboardVersion="NO"; shift 0;;
    '-up') upstreamPom="$2"; shift 1;; # eg., 6.11.1 or 6.13.0-SNAPSHOT
    '-up2') upstreamPom2="$2"; shift 1;; # eg., 6.11.1 or 6.13.0-SNAPSHOT
    '-PROFILES') PROFILES="$2"; shift 1;; # override default profiles
    '-MVNFLAGS') MVNFLAGS="$2"; shift 1;; # add more mvn flags
    '-INDY') INDY="$2"; shift 1;; # override for default INDY URL
    '-ns') doSedReplacements=0; shift 0;; # don't do sed replacements (testing NCL-4195)
    *) OTHER="${OTHER} $1"; shift 0;; 
  esac
  shift 1
done

if [[ ! ${suffix} ]]; then # compute it from version of org/eclipse/che/depmgt/maven-depmgt-pom
  tmpfile=/tmp/maven-metadata-${version}.html
  # external 1: http://indy.cloud.pnc.engineering.redhat.com/api/group/static/org/eclipse/che/depmgt/maven-depmgt-pom/ or /che/che-parent/
  # external 2: http://indy.cloud.pnc.engineering.redhat.com/api/content/maven/group/builds-untested+shared-imports+public/org/eclipse/che/depmgt/maven-depmgt-pom/
  UPSTREAM_POM="api/content/maven/group/builds-untested+shared-imports+public/${upstreamPom}/maven-metadata.xml"
  if [[ ! ${INDY} ]]; then 
    INDY=http://indy.project-newcastle.svc.cluster.local
  fi
  if [[ ! $(wget ${INDY} -q -S 2>&1 | egrep "200|302|OK") ]]; then
    INDY=http://pnc-indy-branch-nightly.project-newcastle.svc.cluster.local
  fi
  if [[ ! $(wget ${INDY} -q -S 2>&1 | egrep "200|302|OK") ]]; then
    INDY=http://pnc-indy-master-nightly.project-newcastle.svc.cluster.local
  fi
  if [[ ! $(wget ${INDY} -q -S 2>&1 | egrep "200|302|OK") ]]; then
    echo "[WARNING] Could not load Indy"
  fi
  wget ${INDY}/${UPSTREAM_POM} -O ${tmpfile}
  suffix=$(grep ${version} ${tmpfile} | grep "<latest>" | egrep '.redhat-[0-9]{5}' | sed -e "s#.\+>\([0-9.]\+\.\)\(redhat-[0-9]\{5\}\).*#\2#" | sort -r | head -1)
  rm -f ${tmpfile}
fi

if [[ ${isWorkspacesBuild} -gt 0 ]] || [[ ${upstreamPom2} ]]; then # compute it
  tmpfile=/tmp/maven-metadata-${version}.html
  # external 1: http://indy.cloud.pnc.engineering.redhat.com/api/group/static/org/eclipse/che/depmgt/maven-depmgt-pom/ or /che/che-parent/
  # external 2: http://indy.cloud.pnc.engineering.redhat.com/api/content/maven/group/builds-untested+shared-imports+public/org/eclipse/che/depmgt/maven-depmgt-pom/
  UPSTREAM_POM="api/content/maven/group/builds-untested+shared-imports+public/${upstreamPom2}/maven-metadata.xml"
  if [[ ! ${INDY} ]]; then 
    INDY=http://indy.project-newcastle.svc.cluster.local
  fi
  if [[ ! $(wget ${INDY} -q -S 2>&1 | egrep "200|302|OK") ]]; then
    INDY=http://pnc-indy-branch-nightly.project-newcastle.svc.cluster.local
  fi
  if [[ ! $(wget ${INDY} -q -S 2>&1 | egrep "200|302|OK") ]]; then
    INDY=http://pnc-indy-master-nightly.project-newcastle.svc.cluster.local
  fi
  if [[ ! $(wget ${INDY} -q -S 2>&1 | egrep "200|302|OK") ]]; then
    echo "[WARNING] Could not load org/eclipse/che/depmgt/maven-depmgt-pom from Indy"
  fi
  wget ${INDY}/${UPSTREAM_POM} -O ${tmpfile}
  suffix2=$(grep ${version} ${tmpfile} | grep "<latest>" | egrep '.redhat-[0-9]{5}' | sed -e "s#.\+>\([0-9.]\+\.\)\(redhat-[0-9]\{5\}\).*#\2#" | sort -r | head -1)
  rm -f ${tmpfile}
fi

#TODO when dashboard is built in NCL, remove from here
if [[ ! ${dashboardVersion} ]]; then
  dashboardVersion=${version}-SNAPSHOT
fi

# replace pme version with the version from upstream parent pom, so we can resolve parent pom version 
# and all artifacts in che-* builds use the same qualifier
# TODO: might be able to skip this step once PNC 1.4 / PME 3.1 is rolled out:
# see https://docs.engineering.redhat.com/display/JPC/PME+3.1
# temp w/ timestamp: 6.12.0.t20180917-201638-873-redhat-00001
# temp w/o timestamp: 6.12.0.temporary-redhat-00001-47358abd
# persistent: 6.12.0.redhat-00001-ec28abe6
# pmeVersionSHA=$(git describe --tags)
# pmeSuffix=${pmeVersion#${version}.}; echo $suffix
# replace 6.14.x (.2) with 6.14.1
if [[ ${suffix} ]] && [[ ${doSedReplacements} -gt 0 ]]; then
  versionRoot=${version%.*}
  echo "[INFO] Replacing ${versionRoot}.* with ${version}.${suffix} ..."
  for d in $(find . -name pom.xml); do sed -i "s#\(version>\)${versionRoot}.*\(</version>\)#\1${version}.${suffix}\2#g" $d; done
  for d in $(find . -name pom.xml); do sed -i "s#\(<che.\+version>\)${versionRoot}.*\(</che.\+version>\)#\1${version}.${suffix}\2#g" $d; done
  for d in $(find . -name pom.xml); do sed -i "s#\(<version>${versionRoot}.*\)-SNAPSHOT#\1.${suffix}#g" $d; done # may not be needed 
  sed -i "s#\(<.\+\.version>.\+\)-SNAPSHOT#\1.${suffix}#g" $d pom.xml # may not be needed
  cat pom.xml | grep version | egrep -v "}|xml version" 
  echo "[INFO] Replaced ${versionRoot}.* with ${version}.${suffix}"
  if [[ ${suffix2} ]]; then
    echo "[INFO] Replacing parent version with ${version}.${suffix2} ..."
    perl -0777 -p -i -e 's|(\ +<parent>.*?<\/parent>)| $1 =~ /(<version>.+<\/version>)/?"    <parent>\n        <artifactId>maven-depmgt-pom</artifactId>\n        <groupId>org.eclipse.che.depmgt</groupId>\n        <version>'"${version}.${suffix2}"'</version>\n    </parent>":$1|gse' pom.xml
    cat pom.xml | grep version | egrep -v "}|xml version" 
    echo "[INFO] Replaced parent with ${version}.${suffix2}"
  fi
fi

# TODO when dashboard is built in NCL, remove from here
# fix properties mangled by above sed/perl changes or PME: want these to be -SNAPSHOT, not -redhat-0000x
propsToFix=che.ls.jdt.version
if [[ ${includeDashboardFromSource} -lt 1 ]]; then
  propsToFix="che.dashboard.version che.ls.jdt.version"
fi
for prop in ${propsToFix}; do
  for d in $(find . -name pom.xml); do
    sed -i "s#\(<${prop}.\+\).redhat-[0-9]\{5\}\(</${prop}>\)#\1-SNAPSHOT\2#g" $d
    propval=$(grep "<${prop}>" $d | sed -e "s#.*<${prop}>\(.\+\)</${prop}>.*#\1#")
    if [[ ${propval} ]]; then
      echo "[INFO] Set ${prop} = ${propval}"
      MVNFLAGS="${MVNFLAGS} -D${prop}=${propval}"
      propval=""
    fi
  done
done

# TODO when dashboard is built in NCL, remove from here
# cmdline overrides for che.dashboard.version and che.ls.jdt.version
if [[ $dashboardVersion ]] && [[ $dashboardVersion != "NO" ]]; then
  MVNFLAGS="${MVNFLAGS} -Dche.dashboard.version=${dashboardVersion}"
fi
if [[ $lsjdtVersion ]] && [[ $lsjdtVersion != "NO" ]]; then
  MVNFLAGS="${MVNFLAGS} -Dche.ls.jdt.version=${lsjdtVersion}"
fi

##########################################################################################
# set up npm environment
##########################################################################################

uname -a
go version
node -v
npm version
mvn -v

export NCL_PROXY="http://${buildContentId}+tracking:${accessToken}@${proxyServer}:${proxyPort}"
# wget proxies
export http_proxy="${NCL_PROXY}"
export https_proxy="${NCL_PROXY}"

export nodeDownloadRoot=http://nodejs.org:80/dist/
export npmDownloadRoot=http://registry.npmjs.org:80/npm/-/
export npmRegistryURL=http://registry.npmjs.org:80/
export YARN_REGISTRY=http://registry.yarnpkg.com:80/

export NCL_CA="-----BEGIN CERTIFICATE-----
MIICGzCCAYSgAwIBAgIJAPECUDwVnjyvMA0GCSqGSIb3DQEBCwUAMCUxEDAOBgNV
BAMMB1Rlc3QgQ0ExETAPBgNVBAoMCFRlc3QgT3JnMB4XDTE4MDkyMTAyMzUzOVoX
DTI4MDczMDAyMzUzOVowJTEQMA4GA1UEAwwHVGVzdCBDQTERMA8GA1UECgwIVGVz
dCBPcmcwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALzHqk5GpAVKPruIxB7Q
VnkDdt89IR7OmnlKYTvS9C8lb9vSfpgt25db1pXt+NAQfWUe4iXu/3HXCQE+T2ir
ONjQRM9fqlVCiUvmECeo+XnBvyI5iJ/TOdbkpSz3fzzokarG5uZoC0C6dfWo3xOg
FGtdujURgmlUdDGBMzdo+OkTAgMBAAGjUzBRMB0GA1UdDgQWBBSZR9TyL1ih76Ah
ONFiPGqtao9sSDAfBgNVHSMEGDAWgBSZR9TyL1ih76AhONFiPGqtao9sSDAPBgNV
HRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4GBALLOjTVPyjzDPST7HHq4jafs
L9l5WiTJSeEDim8nN4V1ZgK3K+znoR9Ztx8taCPi+QwqpvTEXlEEiezx+hygClzY
Mo/W0QKYPgMqRlQnJzAhZhb++KWrovtdzk5dUOZa6xfKSB4DoQHYowGr+PO8R7hS
bvrsD6YFuVn6ZtSb8qkZ
-----END CERTIFICATE-----"

for p in proxy http-proxy http_proxy HTTP_PROXY https-proxy https_proxy HTTPS_PROXY; do npm config set ${p} ${NCL_PROXY}; done
for r in registry; do npm config set ${r} ${npmRegistryURL}; done
npm config set strict-ssl false
npm config set ca "${NCL_CA}"

# loglevel = silent, warn, info, verbose, silly
# do not use maxsockets 2 or build will stall & die
npm config set loglevel warn 
npm config set maxsockets 80 
npm config set fetch-retries 10
npm config set fetch-retry-mintimeout 60000

npm config list

if [[ $includeDashboardFromSource -gt 0 ]]; then
  # workaround for lack of https support and inability to see github.com as a result
  mkdir -p /tmp/phantomjs/
  pushd /tmp/phantomjs/
    # previously mirrored from https://github.com/Medium/phantomjs/releases/download/v2.1.1/phantomjs-2.1.1-linux-x86_64.tar.bz2
    time wget -q http://download.jboss.org/jbosstools/requirements/codeready-workspaces/node/phantomjs/phantomjs-2.1.1-linux-x86_64.tar.bz2
  popd

  pushd dashboard
    time npm install phantomjs-prebuilt
    export PATH=${PATH}:`pwd`/node_modules/phantomjs-prebuilt/bin

    time npm install yarn
    PATH=${PATH}:`pwd`/node_modules/yarn/bin

    for p in proxy http-proxy http_proxy HTTP_PROXY https-proxy https_proxy HTTPS_PROXY yarn-proxy yarn_proxy; do yarn config set ${p} ${NCL_PROXY} --global; done
    for r in registry YARN_REGISTRY; do yarn config set ${r} ${YARN_REGISTRY} --global; done
    yarn config set strict-ssl false --global
    yarn config set ca "${NCL_CA}" --global

    # loglevel = silent, warn, info, verbose, silly
    # do not use maxsockets 2 or build will stall & die
    yarn config set loglevel warn --global
    yarn config set maxsockets 80 --global
    yarn config set fetch-retries 10 --global
    yarn config set fetch-retry-mintimeout 60000 --global

    yarn config list
    # yarn install --frozen-lockfile --no-lockfile --pure-lockfile --ignore-optional --non-interactive --production=false
  popd
fi

##########################################################################################
# configure maven build 
##########################################################################################

if [[ ! ${PROFILES} ]]; then PROFILES=' -Pfast,native,!docker,!docs'; fi

MVNFLAGS="${MVNFLAGS} -V -ff -B -e -Dskip-enforce -DskipTests -Dskip-validate-sources -Dfindbugs.skip -DskipIntegrationTests=true"
MVNFLAGS="${MVNFLAGS} -Dmdep.analyze.skip=true -Dmaven.javadoc.skip -Dgpg.skip -Dorg.slf4j.simpleLogger.showDateTime=true"
MVNFLAGS="${MVNFLAGS} -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss "
MVNFLAGS="${MVNFLAGS} -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
MVNFLAGS="${MVNFLAGS} -DnodeDownloadRoot=${nodeDownloadRoot} -DnpmDownloadRoot=${npmDownloadRoot}"
MVNFLAGS="${MVNFLAGS} -DnpmRegistryURL=${npmRegistryURL} -DYARN_REGISTRY=${YARN_REGISTRY}"

##########################################################################################
# run maven build 
##########################################################################################

mvn clean deploy ${PROFILES} ${MVNFLAGS}

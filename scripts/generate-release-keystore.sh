#!/usr/bin/env bash
#
# Generates the self-signed certificate the release build is signed with.
#
# The app is not distributed through Play, so there is no upload key to
# protect: a key generated here is all `assembleRelease` needs to produce an
# installable APK. CI runs this same script (see .github/workflows/release.yml)
# whenever no keystore is configured through repository secrets, which is what
# makes a release build work out of the box.
#
#   RELEASE_KEYSTORE_PASSWORD=... ./scripts/generate-release-keystore.sh release.jks
#
# The store is PKCS12, so the key and the store share one password — keytool
# does not support separate ones for that format. Gradle still accepts a
# distinct `keyPassword` for keystores that have one (see app/build.gradle.kts).
#
# To use the result locally, write an (untracked) keystore.properties at the
# repository root:
#
#   storeFile=release.jks
#   storePassword=<the password used here>
#   keyAlias=praondefoiomeudinheiro
#
# Keep the file and the keystore out of git — .gitignore already covers both.
set -euo pipefail

keystore=${1:-release.jks}
alias=${RELEASE_KEY_ALIAS:-praondefoiomeudinheiro}
password=${RELEASE_KEYSTORE_PASSWORD:-}
# ~30 years. An Android signing certificate has to outlive every update the app
# will ever ship, because a change of key means a new install, not an upgrade.
validity=${RELEASE_KEY_VALIDITY_DAYS:-10950}
dname=${RELEASE_KEY_DNAME:-"CN=Pra onde foi o meu dinheiro, OU=Development, O=hhldiniz, C=BR"}

if [ -z "$password" ]; then
    echo "error: set RELEASE_KEYSTORE_PASSWORD to the password to protect $keystore with" >&2
    exit 1
fi

if [ -e "$keystore" ]; then
    # Overwriting is never what the caller meant: the old certificate is the
    # only thing that can sign an upgrade for whoever installed the last build.
    echo "error: $keystore already exists — delete it first if you really want a new key" >&2
    exit 1
fi

keytool -genkeypair -noprompt \
    -keystore "$keystore" \
    -storetype PKCS12 \
    -alias "$alias" \
    -keyalg RSA \
    -keysize 4096 \
    -sigalg SHA256withRSA \
    -validity "$validity" \
    -dname "$dname" \
    -storepass "$password" \
    -keypass "$password"

echo "wrote $keystore — alias '$alias', valid for $validity days"
keytool -list -v -keystore "$keystore" -storepass "$password" -alias "$alias" |
    grep -E "^(Owner|Valid from|\s+SHA256):" || true

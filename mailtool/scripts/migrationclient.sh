#!/bin/bash

# You may need to set -Dmail.imap.ssl.trust=* if your IMAP server
# does not have a valid certificate.  Only do this if you REALLY
# trust the identity of the server!

SCRIPT_DIR=`dirname $0`

export APPCLASS="com.obliquity.mailtool.MigrationClient"

RUNAPP="${SCRIPT_DIR}/runapp.sh"

exec "${RUNAPP}" "$@"

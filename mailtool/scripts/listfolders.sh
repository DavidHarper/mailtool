#!/bin/bash

SCRIPT_DIR=`dirname $0`
BASE_DIR="${SCRIPT_DIR}/.."

CLASSES_DIR=${BASE_DIR}/classes
LIB_DIR=${BASE_DIR}/lib

CLASSPATH="${CLASSES_DIR}:${LIB_DIR}/*"

APP_CLASS="com.obliquity.mailtool.ListFolders"

# You may need to set -Dmail.imap.ssl.trust=* if your IMAP server
# does not have a valid certificate.  Only do this if you REALLY
# trust the identity of the server!

java ${JAVA_OPTS} -Djava.awt.headless=true -classpath "${CLASSPATH}" ${APP_CLASS} "$@"

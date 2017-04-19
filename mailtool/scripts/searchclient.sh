#!/bin/bash

SCRIPT_DIR=`dirname $0`
BASE_DIR="${SCRIPT_DIR}/.."

CLASSES_DIR=${BASE_DIR}/classes
LIB_DIR=${BASE_DIR}/lib

CLASSPATH="${CLASSES_DIR}:${LIB_DIR}/*"

APP_CLASS="com.obliquity.mailtool.SearchClient"

java ${JAVA_OPTS} -Djava.awt.headless=true -D"mail.imap.ssl.trust=*" \
    -classpath "${CLASSPATH}" ${APP_CLASS} \
    "$@"
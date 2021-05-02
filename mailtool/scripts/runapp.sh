#!/bin/bash

##
##  COPYRIGHT NOTICE
##  
##  Copyright (c) 2021 David Harper
##  
##  All rights reserved.
##

if [ -z "${APPCLASS}" ]
then
    echo "Set APPCLASS to name of application class and re-run."
    exit 1
fi

SCRIPT_DIR=`dirname $0`

BASEDIR=`readlink -f ${SCRIPT_DIR}/..`

CLASSDIR=${BASEDIR}/build/classes/java/main

if [ ! -d "${CLASSDIR}" ]
then
    echo "Class directory ${CLASSDIR} not found.  Run 'gradle build' then re-run."
    exit 1
fi

JARLIBDIR=${BASEDIR}/build/extlibs

if [ ! -d "${JARLIBDIR}" ]
then
    echo "External JAR library directory ${JARLIBDIR} not found.  Run 'gradle build' then re-run."
    exit 1
fi

java  ${MAILTOOL_JAVA_OPTS} \
  -cp ${CLASSDIR}:${JARLIBDIR}/\* \
  ${APPCLASS} "$@"

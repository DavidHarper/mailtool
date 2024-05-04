#!/bin/bash

SCRIPT_DIR=`dirname $0`

export APPCLASS="com.obliquity.mailtool.SimpleMailSender"

RUNAPP="${SCRIPT_DIR}/runapp.sh"

exec "${RUNAPP}" "$@"

#!/bin/bash

mkdir -p classes

cd src

javac -d ../classes -cp '../lib/*' com/obliquity/mailtool/*.java com/obliquity/mailtool/ssl/*.java

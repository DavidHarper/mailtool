#!/bin/bash

mkdir -p classes

cd src

javac -d ../classes -cp ../lib/javamail-1.4.7.jar com/obliquity/mailtool/*.java com/obliquity/mailtool/ssl/*.java

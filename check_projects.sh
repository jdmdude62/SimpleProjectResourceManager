#!/bin/bash
CP=$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q 2>/dev/null)
java -cp ".:$CP" CheckProjects "$@"
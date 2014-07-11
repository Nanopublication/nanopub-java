#!/bin/bash
#
# Usage:
# $ scripts/exec-class.sh [JAVA-CLASS] [ARGS...]
#
# The environment variable JAVA_OPTS can be used to set Java command line options.
#

WORKINGDIR=`pwd`

cd "$( dirname "${BASH_SOURCE[0]}" )"
cd -P ..

PROJECTDIR=`pwd`
# for Cygwin:
PROJECTDIR=${PROJECTDIR#/cygdrive/?}

if [ ! -f classpath.txt ]; then
  echo "classpath.txt not found: Run 'mvn clean package' first."
  exit 1
fi

CP=$PROJECTDIR/target/classes:$(cat classpath.txt)

cd $WORKINGDIR

CLASS=$1
shift

java -cp $CP $JAVA_OPTS $CLASS "$@"

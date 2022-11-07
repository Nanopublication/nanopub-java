#!/bin/bash
#
# This script is needed for the Maven Release Plugin to load the
# content of the Git submodules (testsuite) too.

git submodule update --init
git submodule foreach git submodule update --init

#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")"

mkdir -p build/classes

javac -d build/classes src/main/java/dev/npmvisualizer/Main.java

java -cp build/classes dev.npmvisualizer.Main "$@"

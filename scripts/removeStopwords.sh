#!/bin/bash


sed 's|^|s/\\<|; s|$|\\>//g;|' $2 > /tmp/words.sed

sed -f /tmp/words.sed $1 > $1.clean

sed -i 's/  / /g' $1.clean

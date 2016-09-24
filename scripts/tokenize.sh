#!/bin/bash


tokenize(){
# cat  $1 |  tr -d '[:punct:]' > /tmp/$1.copy

sed -e 's/#/^g/g;s/\([[:punct:]]\)//g;s/^g/#/g' $1 > /tmp/$1.copy

sed -e 's/\b\([a-z]\+\)[ ,\n]\1/\1/g' /tmp/$1.copy > /tmp/$1.copy.copy

string=`cat /tmp/$1.copy.copy`
for word in $string; do echo "$word" >> $1.tokens ; done

rm /tmp/$1.copy
rm /tmp/$1.copy.copy
}


for f in $1/*.stopwords
do
  tokenize $f
done



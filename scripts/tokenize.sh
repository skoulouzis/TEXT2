#!/bin/bash


tokenize(){
# cat  $1 |  tr -d '[:punct:]' > $1.copy

sed -e 's/#/^g/g;s/\([[:punct:]]\)//g;s/^g/#/g' $1 > $1.copy

sed -e 's/\b\([a-z]\+\)[ ,\n]\1/\1/g' $1.copy > $1.copy.copy

string=`cat $1.copy.copy`
for word in $string; do echo "$word" >> $1.tokens ; done

rm $1.copy
rm $1.copy.copy
}


for f in $1/*.stopwords
do
  tokenize $f
done



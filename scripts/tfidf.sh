#!/bin/bash

head -2 $2 > tmp
df=`tail -1 tmp`
df=${df#","}
IFS=',' read -ra dfArray <<< "$df"

    
count=0
while read p; do
  if [ "$count" -gt 1 ];
  then 
    IFS=',' read -ra tfArray <<< "$p"    
    for docf in "${dfArray[@]}"; do
    j=0
      for tf in "${tfArray[@]}"; do
	if [ "$j" -gt 1 ];
	then
	  idf=`echo 1/$docf | bc -l | awk '{printf "%f", $0}'` 
# 	  echo = 1 "/" $docf "=" $idf
	  tfidf=`echo $tf \* $idf | bc -l`
# 	  echo $tf "*" $idf "=" $tfidf
	  line=$line,$tfidf
	fi
	j=$((j+1))
      done
      echo $line >> tfidf.csv
      line=""
    done
  fi
  count=$((count+1))
  echo count=$count 
  if [ "$count" -gt 10 ];
  then
    exit
  fi
done <$1


rm tmp
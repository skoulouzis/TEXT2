#!/bin/bash



removeStopwords () {
  cat  $1  | tr '[:upper:]' '[:lower:]' > /tmp/$1.copy 
  sed 's|^|s/\\<|; s|$|\\>//g;|' $2 > /tmp/words.sed
  sed -f /tmp/words.sed /tmp/$1.copy > $1.stopwords
  sed -i 's/  / /g' $1.stopwords
  rm /tmp/$1.copy 
}



for f in $1/*.txt
do
  echo "Processing file $f"
  removeStopwords $f $2
done

rm /tmp/words.sed
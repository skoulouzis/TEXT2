#!/bin/bash



transpose(){
  echo transposing $i
  awk '
  { 
      for (i=1; i<=NF; i++)  {
	  a[NR,i] = $i
      }
  }
  NF>p { p = NF }
  END {    
      for(j=1; j<=p; j++) {
	  str=a[1,j]
	  for(i=2; i<=NR; i++){
	      str=str" "a[i,j];
	  }
	  print str
      }
  }' $1 > tmpFile 
  
  mv tmpFile $1.trans
}




transpose $1 
sed -i 's/ /,/g' $1.trans
sed -i 's/_/ /g' $1.trans
terms=`cat $1.trans`
header="fileName,"$terms
echo $header > tf.csv



for f in $2/*.tokens
do
  echo "$0 Processing file $f"
  while read p; do
    tf=`grep "$p" $f | wc -l`
    line=$line,$tf
  done <$1
  echo $f $line >> tf.csv
  line=""
done 

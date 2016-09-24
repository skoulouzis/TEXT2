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
terms=`cat $1.trans`
header="fileName,"$terms
echo $header > df.csv


while read p; do
  df=0
  for f in $2/*.tokens
  do
    grep -q "$p" $f; [ $? -eq 0 ] && df=$((df+1))
  done 
   line=$line,$df
done <$1

echo $line >> df.csv

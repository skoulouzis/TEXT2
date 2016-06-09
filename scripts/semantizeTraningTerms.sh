#!/bin/bash

JAR_PATH=$HOME/workspace/TEXT2/termSemantization/target/termSemantization-1.0-SNAPSHOT-jar-with-dependencies.jar


declare -a TRAIN_DOC_PATHS=("$HOME/Downloads/doc3/1Data_Analytics_and_Machine_Learning/" "$HOME/Downloads/doc3/2Data_Management_data_Curation/" "$HOME/Downloads/doc3/3Data_Science_Engineering/" "$HOME/Downloads/doc3/4Scientific_Research_Methods/" "$HOME/Downloads/doc3/5PersonalInter-personalCommunication" "$HOME/Downloads/doc3/2Data_Management_data_Curation/" "$HOME/Downloads/doc3/6ApplicationSubjectDomain")

for i in "${TRAIN_DOC_PATHS[@]}"
do
  for f in $i/dictionary.csv
  do
    echo $f $i
    java -jar $JAR_PATH $HOME/Downloads/textdocs/dictionaryAll.csv $f $i
  done 
done


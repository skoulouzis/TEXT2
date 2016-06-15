#!/bin/bash

JAR_PATH=$HOME/workspace/TEXT2/termXtraction/target/termXtraction-1.0-SNAPSHOT-jar-with-dependencies.jar
DICTIONARY_ALL=$HOME/Downloads/textdocs/dictionaryAll.csv

# declare -a TRAIN_DOC_PATHS=("$HOME/Downloads/doc3/1Data_Analytics_and_Machine_Learning/" "$HOME/Downloads/doc3/2Data_Management_data_Curation/" "$HOME/Downloads/doc3/3Data_Science_Engineering/" "$HOME/Downloads/doc3/4Scientific_Research_Methods/" "$HOME/Downloads/doc3/5PersonalInter-personalCommunication" "$HOME/Downloads/doc3/2Data_Management_data_Curation/" "$HOME/Downloads/doc3/6ApplicationSubjectDomain")
# declare -a TRAIN_DOC_PATHS=("$HOME/Downloads/doc3/6ApplicationSubjectDomain" "$HOME/Downloads/doc3/5PersonalInter-personalCommunication")


declare -a TRAIN_DOC_PATHS=("$HOME/Downloads/D2.2_Table_14CompetencesGroups/DataAnalytics_DSDA/" "$HOME/Downloads/D2.2_Table_14CompetencesGroups/DataManagementCuration_DSDM/" "$HOME/Downloads/D2.2_Table_14CompetencesGroups/DSDomain_DSDK/" "$HOME/Downloads/D2.2_Table_14CompetencesGroups/DSEngineering_DSENG/" "$HOME/Downloads/D2.2_Table_14CompetencesGroups/ScientificResearchMethods_DSRM")


for i in "${TRAIN_DOC_PATHS[@]}"
do
  for f in $i/*.txt
  do
    java -jar $JAR_PATH -e nl.uva.sne.extractors.LuceneExtractor $i/ $i/LuceneDictionary.csv
    java -jar $JAR_PATH -e nl.uva.sne.extractors.JtopiaExtractor $i/ $i/JtopiaExtractorDictionary.csv
  done 
  cat $i/JtopiaExtractorDictionary.csv > $i/dictionary.csv
  cat $i/LuceneDictionary.csv >> $i/dictionary.csv
  sort $i/dictionary.csv | uniq -u > $i/tmp
  mv $i/tmp $i/dictionary.csv
  
  while read p; do
    IFS=',' read -a myarray <<< "$p"
    grep -i "${myarray[0]}" $DICTIONARY_ALL >> $i/tmp
  done <$i/dictionary.csv
  
  sort $i/tmp | uniq -u > $i/dictionary.csv
  rm $i/tmp
done



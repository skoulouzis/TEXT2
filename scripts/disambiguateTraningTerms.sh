#!/bin/bash

JAR_PATH=$HOME/workspace/TEXT2/termDisambiguation/target/termDisambiguation-1.0-SNAPSHOT-jar-with-dependencies.jar
PROPS_FILE=$HOME/workspace/TEXT2/scripts/props.properties
DICTIONARY_ALL=$HOME/Downloads/textdocs/dictionaryAll.csv

# declare -a TRAIN_DOC_PATHS=("$HOME/Downloads/doc3/1Data_Analytics_and_Machine_Learning/" "$HOME/Downloads/doc3/2Data_Management_data_Curation/" "$HOME/Downloads/doc3/3Data_Science_Engineering/" "$HOME/Downloads/doc3/4Scientific_Research_Methods/" "$HOME/Downloads/doc3/5PersonalInter-personalCommunication" "$HOME/Downloads/doc3/2Data_Management_data_Curation/" "$HOME/Downloads/doc3/6ApplicationSubjectDomain")


declare -a TRAIN_DOC_PATHS=("$HOME/Downloads/D2.2_Table_14CompetencesGroups/DataAnalytics_DSDA/" "$HOME/Downloads/D2.2_Table_14CompetencesGroups/DataManagementCuration_DSDM/" "$HOME/Downloads/D2.2_Table_14CompetencesGroups/DSDomain_DSDK/" "$HOME/Downloads/D2.2_Table_14CompetencesGroups/DSEngineering_DSENG/" "$HOME/Downloads/D2.2_Table_14CompetencesGroups/ScientificResearchMethods_DSRM")


for i in "${TRAIN_DOC_PATHS[@]}"
do
  for f in $i/dictionary.csv
  do
    echo $f $i
    java -jar $JAR_PATH $DICTIONARY_ALL $f $i $PROPS_FILE
  done 
done


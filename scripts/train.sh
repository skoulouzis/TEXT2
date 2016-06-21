#!/bin/bash

JAR_PATH=$HOME/workspace/TEXT2/termClassifier/target/termClassifier-1.0-SNAPSHOT-jar-with-dependencies.jar
PROPS_FILE=$HOME/workspace/TEXT2/scripts/props.properties
CLASSIFIER=nl.uva.sne.classifiers.CosineSimilarity
DICTIONARY_ALL=$HOME/Downloads/textdocs/dictionaryAll.csv


declare -a TRAIN_DOC_PATHS=("$HOME/Downloads/D2.2_Table_14CompetencesGroups/DataAnalytics_DSDA/" "$HOME/Downloads/D2.2_Table_14CompetencesGroups/DataManagementCuration_DSDM/" "$HOME/Downloads/D2.2_Table_14CompetencesGroups/DSDomain_DSDK/" "$HOME/Downloads/D2.2_Table_14CompetencesGroups/DSEngineering_DSENG/" "$HOME/Downloads/D2.2_Table_14CompetencesGroups/ScientificResearchMethods_DSRM")


# -t nl.uva.sne.classifiers.CosineSimilarity /home/alogo/Downloads/trainData /home/alogo/Downloads/trainedModel/
# for i in "${TRAIN_DOC_PATHS[@]}"
# do
  java -jar $JAR_PATH -t $CLASSIFIER $HOME/Downloads/D2.2_Table_14CompetencesGroups/ $HOME/Downloads/D2.2_Table_14CompetencesGroups/ $PROPS_FILE
# done
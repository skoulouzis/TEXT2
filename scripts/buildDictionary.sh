#!/bin/bash


cat $1/*.tokens > all 

sort all | uniq > dictionary.dic

rm all


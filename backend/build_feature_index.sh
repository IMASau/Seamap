#!/bin/bash
OUTPUT=$($1 $2 build_feature_index_start | tail -1)
IFS=' ' read -ra ADDR <<< "$OUTPUT"
for i in "${ADDR[@]}"; do
    $1 $2 build_feature_index_layer --layer_id $i
done
$1 $2 build_feature_index_end

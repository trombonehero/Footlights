#!/bin/sh

SIZES="128 96 72 48 36 32"

filename=`echo $1 | tr '[:upper:]' '[:lower:]'`
if [ "$filename" == "" ]; then
	echo "No filename specified"
	exit 1
fi

basename=`echo $filename | awk 'BEGIN{FS=OFS="."}{$NF=""; NF--; print}'`
for i in $SIZES; do
	target=gen/$basename-$i.png
	echo "$filename => $target"

	convert -geometry ${i}x${i} $filename $target
done


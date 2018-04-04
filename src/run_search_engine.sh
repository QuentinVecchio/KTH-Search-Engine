#!/bin/sh
#java -cp classes:pdfbox -Xmx1g ir.Engine -d ../data/davisWiki_test -l ir18.jpg -p patterns.txt
java -cp classes:pdfbox -Xmx1g ir.Engine -d ../data/davisWiki -l ir18.jpg -p patterns.txt
#java -cp classes:pdfbox -Xmx1g ir.Engine -d ../data/dataset_2_3 -l ir18.jpg -p patterns.txt
#java -cp classes:pdfbox -Xmx1g ir.Engine -d ../data/guardian -l ir18.jpg -p patterns.txt

#!/bin/sh
java -cp classes:pdfbox -Xmx1g ir.Engine -d ../data/davisWiki -l ir18.jpg -p patterns.txt -ni

javac -cp . -d classes ir/KGramIndex.java && java -cp classes ir.KGramIndex -f $1 -p patterns.txt -k 2 -kg "$2"

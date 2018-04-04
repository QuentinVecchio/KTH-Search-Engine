/*
*   This file is part of the computer assignment for the
*   Information Retrieval course at KTH.
*
*   Johan Boye, 2017
*/

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID = 0;
    public double score = 0;
    public ArrayList<Integer> positions;

    public PostingsEntry(int docID, double score) {
        this.docID = docID;
        this.score = score;
        this.positions = new ArrayList<Integer>();
    }

    public PostingsEntry(String s) {
        this.positions = new ArrayList<Integer>();
        String[] elts = s.split(";");
        this.docID = Integer.parseInt(elts[0]);
        elts = elts[1].split("\\[");
        this.score = Double.parseDouble(elts[0]);
        String[] pos = elts[1].split(",");
        for(int i=0;i<pos.length;++i) {
            this.positions.add(Integer.parseInt(pos[i]));
        }
    }

    public void addPosition(int position) {
        int start = 0;
        int end = this.positions.size() - 1;
        int i;
        while(end >= start){
            i = (start + end)/2;
            if(this.positions.get(i) == position) {
                return;
            } else if(position > this.positions.get(i)) {
                start = i+1;
            } else {
                end = i-1;
            }
        }
        this.positions.add(position);
    }

    public void merge(PostingsEntry entry) {
        if(this.docID == entry.docID) {
            for(int i=0;i<entry.positions.size();++i) {
                this.addPosition(entry.positions.get(i));
            }
        }
    }

    /**
    *  PostingsEntries are compared by their score (only relevant
    *  in ranked retrieval).
    *
    *  The comparison is defined so that entries will be put in
    *  descending order.
    */
    public int compareTo(PostingsEntry other) {
        return Double.compare( other.score, score );
    }


    public boolean equals(PostingsEntry entry) {
        return entry.docID == this.docID;
    }


    public String toString() {
        String s = "";
        s += Integer.toString(this.docID) + ";";
        s += Double.toString(this.score);
        s += "[";
        for(int i=0;i<this.positions.size();++i) {
            s += Integer.toString(this.positions.get(i));
            if(i < this.positions.size()-1) {
                s += ",";
            }
        }
        s += "]";
        return s;
    }

    // tf = [# occurrences of t in d],
    // N = [# documents in the corpus],
    // df = [# documents in the corpus which contain t],
    // idf = ln(N/df)
    // len = [# words in d].
    public void computeScore(double idf, int len) {
        int tf = this.positions.size();
        // System.out.println("TF : " + Integer.toString(tf));
        // System.out.println("1+ln(TF) : " + Double.toString(1+Math.log10((double)tf)));
        double tf_idf = (double)tf * idf / (double)len;
        this.score = tf_idf;
    }
}

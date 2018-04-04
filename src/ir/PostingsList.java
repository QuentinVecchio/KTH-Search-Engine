/*
*   This file is part of the computer assignment for the
*   Information Retrieval course at KTH.
*
*   Johan Boye, 2017
*/

package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;

public class PostingsList {

    /** The postings list */
    public ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

    public PostingsList() {

    }

    public PostingsList(String s) {
        String[] docs = s.split("]");
        for(int i=0;i<docs.length;++i) {
            list.add(new PostingsEntry(docs[i]));
        }
    }

    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get(int i) {
        return list.get( i );
    }

    public PostingsEntry getEntry(int docID) {
        int start = 0;
        int end = this.list.size()-1;
        int i;
        while(end >= start) {
            i = (start + end)/2;
            if(this.list.get(i).docID == docID) {
                return this.list.get(i);
            } else if(docID > this.list.get(i).docID) {
                start = i+1;
            } else {
                end = i-1;
            }
        }
        return null;
    }

    /** Add a new entry at the good position to preserve the list sorted **/
    public PostingsEntry add( int docID, double score, int position ) {
        PostingsEntry entry = this.add(new PostingsEntry(docID, score));
        entry.addPosition(position);
        return entry;
    }

    public PostingsEntry add(PostingsEntry entry) {
        int start = 0;
        int end = this.list.size()-1;
        int i;
        while(end >= start){
            i = (start + end)/2;
            if(this.list.get(i).docID == entry.docID) {
                return this.list.get(i);
            } else if(entry.docID > this.list.get(i).docID) {
                start = i+1;
            } else {
                end = i-1;
            }
        }
        this.list.add(start, entry);
        return entry;
    }

    private PostingsEntry addAndMerge(PostingsEntry entry) {
        int start = 0;
        int end = this.list.size()-1;
        int i;
        while(end >= start) {
            i = (start + end)/2;
            if(this.list.get(i).docID == entry.docID) {
                this.list.get(i).merge(entry);
                return this.list.get(i);
            } else if(entry.docID > this.list.get(i).docID) {
                start = i+1;
            } else {
                end = i-1;
            }
        }
        this.list.add(start, entry);
        return entry;
    }

    public void merge(PostingsList listEntries) {
        for(int i=0;i<listEntries.list.size();++i) {
            this.addAndMerge(listEntries.list.get(i));
        }
    }

    public String toString() {
        String s = "";
        for(int i=0;i<this.list.size();++i) {
            s += this.list.get(i).toString();
        }
        return s;
    }

    // N = [# documents in the corpus],
    // len = [# words in d].
    public void computeScore(int N,  HashMap<Integer,Integer> docsLength) {
        double idf = Math.log10((double)N/(double)this.list.size());
        //System.out.println("N : " + Integer.toString(N));
        for(int i=0;i<this.list.size();++i) {
            this.list.get(i).computeScore(idf, docsLength.get(this.list.get(i).docID));
        }
    }

    public void sortPostingListByScore() {
        Collections.sort(this.list);
    }

    public boolean documentExists(int docID) {
        int start = 0;
        int end = this.list.size()-1;
        int i;
        while(end >= start) {
            i = (start + end)/2;
            if(this.list.get(i).docID == docID) {
                return true;
            } else if(docID > this.list.get(i).docID) {
                start = i+1;
            } else {
                end = i-1;
            }
        }
        return false;
    }
}

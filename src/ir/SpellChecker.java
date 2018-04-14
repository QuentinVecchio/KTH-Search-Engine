/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.util.List;
import java.util.*;

public class SpellChecker {
    /** The regular inverted index to be used by the spell checker */
    Index index;

    /** K-gram index to be used by the spell checker */
    KGramIndex kgIndex;

    /** The auxiliary class for containing the value of your ranking function for a token */
    class KGramStat implements Comparable {
        double score;
        String token;

        KGramStat(String token, double score) {
            this.token = token;
            this.score = score;
        }

        public String getToken() {
            return token;
        }

	public int compareTo(Object other) {
            if (this.score == ((KGramStat)other).score) return 0;
            return this.score < ((KGramStat)other).score ? -1 : 1;
        }

        public String toString() {
            return token + ";" + score;
        }
    }

    /**
     * The threshold for Jaccard coefficient; a candidate spelling
     * correction should pass the threshold in order to be accepted
     */
    private static final double JACCARD_THRESHOLD = 0.4;


    /**
      * The threshold for edit distance for a candidate spelling
      * correction to be accepted.
      */
    private static final int MAX_EDIT_DISTANCE = 2;


    public SpellChecker(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     *  Computes the Jaccard coefficient for two sets A and B, where the size of set A is
     *  <code>szA</code>, the size of set B is <code>szB</code> and the intersection
     *  of the two sets contains <code>intersection</code> elements.
     */
    private double jaccard(int szA, int szB, int intersection) {
        return (double)(intersection)/(szA + szB - intersection);
    }

    /**
     * Computing Levenshtein edit distance using dynamic programming.
     * Allowed operations are:
     *      => insert (cost 1)
     *      => delete (cost 1)
     *      => substitute (cost 2)
     */
    private int editDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for(int i=0;i<=s1.length();i++) {
            for(int j=0;j<=s2.length();j++) {
                if(i == 0) {
                    dp[i][j] = j;
                }
                else if (j == 0) {
                    dp[i][j] = i;
                }
                else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j - 1] + costOfSubstitution(s1.charAt(i - 1), s2.charAt(j - 1)),
                                                dp[i - 1][j] + 1),
                                                dp[i][j - 1] + 1);
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 2;
    }

    /**
     *  Checks spelling of all terms in <code>query</code> and returns up to
     *  <code>limit</code> ranked suggestions for spelling correction.
     */
    public String[] check(Query query, int limit) {
        HashMap<String, Integer> matches = new HashMap<String, Integer>();
        ArrayList<String> corrections = new ArrayList<String>();
        HashMap<String, Integer> distances = new HashMap<String, Integer>();

        // Do a union search in the k-gram index for the query term
        if(query.queryterm.size() > 0) {
            String term = query.queryterm.get(0).term;
            int sizeTerm = term.length()+3-kgIndex.getK();
            String token = "^" + term + "$";
            for(int i=0;i<=token.length()-kgIndex.getK();++i) {
                String kgram = token.substring(i,i+kgIndex.getK());
                List<KGramPostingsEntry> postings = kgIndex.getPostings(kgram);
                System.out.println("-> " + kgram + " - " + Integer.toString(postings.size()));
                for(int j=0;j<postings.size();++j) {
                    String tk = kgIndex.getTermByID(postings.get(j).tokenID);
                    if(matches.get(tk) == null) {
                        matches.put(tk, 1);
                    } else {
                        matches.put(tk, matches.get(tk)+1);
                    }
                }
            }
            System.out.println("Number of matches : " + Integer.toString(matches.size()));
            // Calculate the Jaccard coefficient between term and each of the resulting words
            for(String key : matches.keySet()) {
                int sizeMatch = key.length()+3-kgIndex.getK();
                if(jaccard(sizeTerm, sizeMatch, matches.get(key)) >= JACCARD_THRESHOLD) {
                    corrections.add(key);
                }
            }
            System.out.println("Number of matches after Jaccard : " + Integer.toString(corrections.size()));
            // calculate the Levenshtein distance between words and term
            for(int k=0;k<corrections.size();++k) {
                int dist = editDistance(term, corrections.get(k));
                if(dist <= MAX_EDIT_DISTANCE) {
                    PostingsList list = index.getPostings(corrections.get(k));
                    int weight = 0;
                    for(int i=0;i<list.size();++i) {
                        weight += list.list.get(i).positions.size();
                    }
                    System.out.println(corrections.get(k) + " : " + Integer.toString(weight));
                    distances.put(corrections.get(k), weight);
                }
            }
            System.out.println("Number of matches after edit distance : " + Integer.toString(distances.size()));
            // Sort the result
            int size = distances.size();
            if(size > limit) {
                size = limit;
            }
            String[] results = new String[size];
            for(int i=0;i<size;++i) {
                results[i] = null;
                Integer best = null;
                for(String key : distances.keySet()) {
                    if(best == null || best < distances.get(key)) {
                        results[i] = key;
                        best = distances.get(results[i]);
                    }
                }
                distances.put(results[i], 0);
            }
            return results;
        }
        return null;
    }

    /**
     *  Merging ranked candidate spelling corrections for all query terms available in
     *  <code>qCorrections</code> into one final merging of query phrases. Returns up
     *  to <code>limit</code> corrected phrases.
     */
    private List<KGramStat> mergeCorrections(List<List<KGramStat>> qCorrections, int limit) {
        //
        // YOUR CODE HERE
        //
        return null;
    }
}

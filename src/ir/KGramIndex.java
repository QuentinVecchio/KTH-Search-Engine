/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer,String> id2term = new HashMap<Integer,String>();

    /** Mapping from term strings to term ids */
    HashMap<String,Integer> term2id = new HashMap<String,Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String,List<KGramPostingsEntry>> index = new HashMap<String,List<KGramPostingsEntry>>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 2;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }

    /**
     *  Get intersection of two postings lists
     */
    public List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
        List<KGramPostingsEntry> intersection = new ArrayList<KGramPostingsEntry>();
        int pointer1 = 0;
        int pointer2 = 0;
        // System.out.println("P1 : " + Integer.toString(p1.size()));
        // System.out.println("P2 : " + Integer.toString(p2.size()));

        while(pointer1 < p1.size() && pointer2 < p2.size()) {
            if(p1.get(pointer1).tokenID == p2.get(pointer2).tokenID) {
                intersection.add(new KGramPostingsEntry(p1.get(pointer1)));
                pointer1++;
                pointer2++;
            } else {
                if(p1.get(pointer1).tokenID > p2.get(pointer2).tokenID) {
                    pointer2++;
                } else {
                    pointer1++;
                }
            }
        }
        // System.out.println("Intersection : " + Integer.toString(intersection.size()));
        return intersection;
    }

    public List<KGramPostingsEntry> match(String token) {
        if(token.contains("*")) {
            List<KGramPostingsEntry> before = null;
            List<KGramPostingsEntry> after = null;
            List<KGramPostingsEntry> intersection = null;
            List<KGramPostingsEntry> results = new ArrayList<KGramPostingsEntry>();
            int positionOfStar = token.indexOf("*");
            String reg = "";
            if(positionOfStar == 0) {
                // *oney
                token = token.substring(positionOfStar+1,token.length());
                reg = ".*" + token + "$";
                token += "$";
                after = this.search(token);
                // System.out.println("Search : " + token);
                // System.out.println("Regex : " + reg);
            } else if(positionOfStar == token.length()-1) {
                // mone*
                token = token.substring(0,positionOfStar);
                reg = "^" + token + ".*";
                token = "^" + token;
                // System.out.println("Search : " + token);
                // System.out.println("Regex : " + reg);
                before = this.search(token);
            } else {
                // mo*ey
                reg = "^" + token.substring(0,positionOfStar) + ".*" + token.substring(positionOfStar+1,token.length()) + "$";
                // System.out.println("Search for $" + token.substring(0,positionOfStar) + " and " + token.substring(positionOfStar+1,token.length()) + "$");
                // System.out.println("Regex : " + reg);
                before = this.search("^" + token.substring(0,positionOfStar));
                after = this.search(token.substring(positionOfStar+1,token.length()) + "$");
            }
            if(before != null && after != null) {
                intersection = intersect(before, after);
            } else if(before != null) {
                intersection = before;
            } else {
                intersection = after;
            }
            for(int i=0;i<intersection.size();++i) {
                String s = getTermByID(intersection.get(i).tokenID);
                if(Pattern.matches(reg, s)) {
                    results.add(intersection.get(i));
                }
            }
            return results;
        } else {
            return this.search("^" + token + "$");
        }
    }

    private List<KGramPostingsEntry> search(String token) {
        List<KGramPostingsEntry> tokenIntersection = null;
        for(int i=0;i<=token.length()-K;++i) {
            String kgram = token.substring(i,i+K);
            // System.out.println("-> " + kgram);
            List<KGramPostingsEntry> postings = getPostings(kgram);
            if(postings != null) {
                if(tokenIntersection == null) {
                    tokenIntersection = postings;
                } else {
                    tokenIntersection = intersect(tokenIntersection, postings);
                }
            }
        }
        return tokenIntersection;
    }

    /** Inserts all k-grams from a token into the index. */
    public void insert( String token ) {
        // Creation or retrieval of the token
        if(term2id.get(token) == null) {
            int tokenID = generateTermID();
            term2id.put(token, tokenID);
            id2term.put(tokenID, token);
            token = "^" + token + "$";
            KGramPostingsEntry entry = new KGramPostingsEntry(tokenID);
            for(int i=0;i<=token.length()-K;++i) {
                String kgram = token.substring(i,i+K);
                if(index.get(kgram) == null) {
                    index.put(kgram, new ArrayList<KGramPostingsEntry>());
                }
                index.get(kgram).add(entry);
            }
        }
    }

    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        return index.get(kgram);
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    private static HashMap<String,String> decodeArgs( String[] args ) {
        HashMap<String,String> decodedArgs = new HashMap<String,String>();
        int i=0, j=0;
        while ( i < args.length ) {
            if ( "-p".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            }
            else if ( "-f".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("file", args[i++]);
                }
            }
            else if ( "-k".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("k", args[i++]);
                }
            }
            else if ( "-kg".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("kgram", args[i++]);
                }
            }
            else {
                System.err.println( "Unknown option: " + args[i] );
                break;
            }
        }
        return decodedArgs;
    }

    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String,String> args = decodeArgs(arguments);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        KGramIndex kgIndex = new KGramIndex(k);

        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
        Tokenizer tok = new Tokenizer( reader, true, false, true, args.get("patterns_file") );
        while ( tok.hasMoreTokens() ) {
            String token = tok.nextToken();
            kgIndex.insert(token);
        }

        String[] kgrams = args.get("kgram").split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != k) {
                System.err.println("Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
    }
}

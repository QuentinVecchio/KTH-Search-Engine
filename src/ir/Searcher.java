/*
*   This file is part of the computer assignment for the
*   Information Retrieval course at KTH.
*
*   Johan Boye, 2017
*/

package ir;

import java.util.ArrayList;
import java.util.*;
import java.io.*;

/**
*  Searches an index for results of a query.
*/
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;
    HashMap<Integer, Double> ranking = new HashMap<Integer, Double>();
    HashMap<String, Integer> docNamesPageRank = new HashMap<String, Integer>();

    /** Constructor */
    public Searcher( Index index ) {
        this.index = index;
        readPageRankFile("../data/davis_page_rank.txt");
        readPageRankTitleFile("../data/davisWikiArticleTitles.txt");
    }

    private void readPageRankFile(String filename) {
        try {
            System.out.println("Reading file of page rank results... ");
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine()) != null) {
                int index = line.indexOf(";");
                Integer docID = Integer.parseInt(line.substring(0, index));
                Double value = Double.parseDouble(line.substring(index+1, line.length()));
                this.ranking.put(docID, value);
            }
        }
        catch ( FileNotFoundException e ) {
            System.out.println("File " + filename + " not found!");
        }
        catch ( IOException e ) {
            System.out.println("Error reading file " + filename);
        }
        System.out.println("Read " + this.ranking.size() + " page rank results");
    }

    private void readPageRankTitleFile(String filename) {
        try {
            System.out.println("Reading file of page rank title ... ");
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine()) != null) {
                int index = line.indexOf(";");
                Integer docID = Integer.parseInt(line.substring(0, index));
                String title = line.substring(index+1, line.length());
                this.docNamesPageRank.put(title, docID);
            }
        }
        catch ( FileNotFoundException e ) {
            System.out.println("File " + filename + " not found!");
        }
        catch ( IOException e ) {
            System.out.println("Error reading file " + filename);
        }
        System.out.println("Read " + this.docNamesPageRank.size() + " page rank titles");
    }

    /**
    *  Searches the index for postings matching the query.
    *  @return A postings list representing the result of the query.
    */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType ) {
        ArrayList<PostingsList> lists = new ArrayList<PostingsList>();
        for(int i=0;i<query.size();++i) {
            PostingsList list = index.getPostings(query.queryterm.get(i).term);
            if(list != null) {
                lists.add(list);
            }
        }

        if(queryType == QueryType.PHRASE_QUERY) {
            return this.positionalIntersect(lists);
        } else if(queryType == QueryType.RANKED_QUERY) {
            if(rankingType == RankingType.PAGERANK) {
                return this.pageRank(lists);
            } else if(rankingType == RankingType.COMBINATION) {
                return this.combination(lists);
            } else {
                return this.tfIdf(query);
            }
        } else {
            return this.intersect(lists);
        }
    }

    private PostingsList intersect(ArrayList<PostingsList> lists) {
        if(lists.size() > 0) {
            int indexBestPostingList = 0;
            int sizeBestPostingList = lists.get(0).size();
            for(int i=1;i<lists.size();++i) {
                if(lists.get(i).size() < sizeBestPostingList) {
                    sizeBestPostingList = lists.get(0).size();
                    indexBestPostingList = i;
                }
            }
            PostingsList inter = lists.get(indexBestPostingList);
            int p1 = 0;
            int p2 = 0;
            for(int i=0;i<lists.size();++i) {
                if(i != indexBestPostingList) {
                    p1 = 0;
                    p2 = 0;
                    PostingsList interTemp = new PostingsList();
                    while(p1 < inter.size() && p2 < lists.get(i).size()) {
                        if(inter.get(p1).docID == lists.get(i).get(p2).docID) {
                            interTemp.add(inter.get(p1));
                            p1++;
                            p2++;
                        } else if(inter.get(p1).docID < lists.get(i).get(p2).docID) {
                            p1++;
                        } else {
                            p2++;
                        }
                    }
                    inter = interTemp;
                }
            }
            return inter;
        } else {
            return null;
        }
    }

    private PostingsList positionalIntersect(ArrayList<PostingsList> lists) {
        if(lists.size() > 0) {
            PostingsList inter = lists.get(0);
            int k = 1;
            int p1 = 0;
            int p2 = 0;
            int pp1 = 0;
            int pp2 = 0;
            int pos1 = 0;
            int pos2 = 0;
            for(int i=1;i<lists.size();++i) {
                p1 = 0;
                p2 = 0;
                PostingsList interTemp = new PostingsList();
                while(p1 < inter.size() && p2 < lists.get(i).size()) {
                    if(inter.get(p1).docID == lists.get(i).get(p2).docID) {
                        PostingsEntry entry = new PostingsEntry(inter.get(p1).docID, 0);
                        pp1 = 0;
                        while(pp1 < inter.get(p1).positions.size()) {
                            pp2 = 0;
                            while(pp2 < lists.get(i).get(p2).positions.size()) {
                                pos1 = inter.get(p1).positions.get(pp1);
                                pos2 = lists.get(i).get(p2).positions.get(pp2);
                                if(pos1 == pos2 - k) {
                                    entry.addPosition(pos2);
                                }
                                pp2++;
                            }
                            pp1++;
                        }
                        p1++;
                        p2++;
                        if(entry.positions.size() > 0) {
                            interTemp.add(entry);
                        }
                    } else if(inter.get(p1).docID < lists.get(i).get(p2).docID) {
                        p1++;
                    } else {
                        p2++;
                    }
                }
                inter = interTemp;
            }
            return inter;
        } else {
            return null;
        }
    }

    private PostingsList tfIdf(Query query) {
        PostingsList results = new PostingsList();
        double scores[] = new double[index.docNames.size()];
        //computeWeightsQuery(query);

        for(int i=0;i<query.size();++i) {
            PostingsList list = index.getPostings(query.queryterm.get(i).term);
            if(list != null) {
                list.computeScore(index.docNames.size(), index.docLengths);
                for(int d=0;d<list.list.size();++d) {
                    scores[list.list.get(d).docID] += list.list.get(d).score * query.queryterm.get(i).weight;
                }
            }
        }

        double[] scores_copy = Arrays.copyOf(scores, scores.length);
        for(int i=0;i<scores.length;i++) {
            int best = -1;
            double max = 0;
            for(int j=0;j<scores.length;j++) {
                if(scores[j] > 0 && scores[j] > max) {
                    best = j;
                    max = scores[j];
                }
            }
            if(best == -1) {
                break;
            }
            results.add(new PostingsEntry(best,scores[best]));
            scores[best] = Double.NEGATIVE_INFINITY;
        }

        results.sortPostingListByScore();
        return results;
        // PostingsList union = lists.get(0);
        // lists.get(0).computeScore(index.docNames.size(), index.docLengths);
        // for(int i=1;i<lists.size();++i) {
        //     lists.get(i).computeScore(index.docNames.size(), index.docLengths);
        //     for(int j=0;j<lists.get(i).size();++j) {
        //         if(union.documentExists(lists.get(i).get(j).docID)) {
        //             union.getEntry(lists.get(i).get(j).docID).score += lists.get(i).get(j).score;
        //         } else {
        //             union.add(lists.get(i).get(j));
        //         }
        //     }
        // }
        // union.sortPostingListByScore();
        // return union;
    }

    private PostingsList pageRank(ArrayList<PostingsList> lists) {
        PostingsList union = lists.get(0);
        lists.get(0).computeScore(index.docNames.size(), index.docLengths);
        for(int i=1;i<lists.size();++i) {
            for(int j=0;j<lists.get(i).size();++j) {
                if(union.documentExists(lists.get(i).get(j).docID)) {
                    union.getEntry(lists.get(i).get(j).docID).score += lists.get(i).get(j).score;
                } else {
                    union.add(lists.get(i).get(j));
                }
            }
        }
        for(int i=0;i<union.list.size();++i) {
            String doc[] = index.docNames.get(union.list.get(i).docID).split("/");
            int docId = this.docNamesPageRank.get(doc[doc.length-1]);
            union.list.get(i).score = ranking.get(docId);
        }
        union.sortPostingListByScore();
        return union;
    }

    private PostingsList combination(ArrayList<PostingsList> lists) {
        // PostingsList results = new PostingsList();
        // double scores[] = new double[index.docNames.size()];
        // double weights[] = computeWeightsQuery(query);
        //
        // for(int i=0;i<query.size();++i) {
        //     PostingsList list = index.getPostings(query.queryterm.get(i).term);
        //     if(list != null) {
        //         list.computeScore(index.docNames.size(), index.docLengths);
        //         for(int d=0;d<list.list.size();++d) {
        //             scores[list.list.get(d).docID] += list.list.get(d).score * weights[i];
        //         }
        //     }
        // }
        //
        // double[] scores_copy = Arrays.copyOf(scores, scores.length);
        // for(int i=0;i<scores.length;i++) {
        //     int best = -1;
        //     double max = 0;
        //     for(int j=0;j<scores.length;j++) {
        //         if(scores[j] > 0 && scores[j] > max) {
        //             best = j;
        //             max = scores[j];
        //         }
        //     }
        //     if(best == -1) {
        //         break;
        //     }
        //     results.add(new PostingsEntry(best,scores[best]));
        //     scores[best] = Double.NEGATIVE_INFINITY;
        // }
        //
        // double coef_tfidf = 1;
        // double coef_page_rank = 2;
        //
        // for(int i=0;i<results.list.size();++i) {
        //     results.list.get(i).score = (coef_tfidf * results.list.get(i).score) * (coef_page_rank * ranking.get(results.list.get(i).docID));
        // }
        //
        // results.sortPostingListByScore();
        // return results;
        PostingsList union = lists.get(0);
        lists.get(0).computeScore(index.docNames.size(), index.docLengths);
        for(int i=1;i<lists.size();++i) {
            for(int j=0;j<lists.get(i).size();++j) {
                if(union.documentExists(lists.get(i).get(j).docID)) {
                    union.getEntry(lists.get(i).get(j).docID).score += lists.get(i).get(j).score;
                } else {
                    union.add(lists.get(i).get(j));
                }
            }
        }
        for(int i=0;i<union.list.size();++i) {
            union.list.get(i).score *= ranking.get(union.list.get(i).docID);
        }
        union.sortPostingListByScore();
        return union;
    }
}

import java.util.*;
import java.io.*;

public class PageRankMonteCarlo {

    /**
    *   Maximal number of documents. We're assuming here that we
    *   don't have more docs than we can keep in main memory.
    */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
    *   Mapping from document names to document numbers.
    */
    HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
    *   Mapping from document numbers to document names
    */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**
    *   A memory-efficient representation of the transition matrix.
    *   The outlinks are represented as a HashMap, whose keys are
    *   the numbers of the documents linked from.<p>
    *
    *   The value corresponding to key i is a HashMap whose keys are
    *   all the numbers of documents j that i links to.<p>
    *
    *   If there are no outlinks from i, then the value corresponding
    *   key i is null.
    */
    HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

    /**
    *   The number of outlinks from each node.
    */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
    *   The probability that the surfer will be bored, stop
    *   following links, and take a random jump somewhere.
    */
    final static double BORED = 0.15;

    /**
    *   Convergence criterion: Transition probabilities do not
    *   change more that EPSILON from one iteration to another.
    */
    final static double EPSILON = 0.0001;


    /* --------------------------------------------- */


    public PageRankMonteCarlo(String filename) {
        int noOfDocs = readDocs(filename);
        //iterateWithAlgorithm1(noOfDocs, 100000);
        //iterateWithAlgorithm2(noOfDocs, 10000, 100);
    }


    /* --------------------------------------------- */


    /**
    *   Reads the documents and fills the data structures.
    *
    *   @return the number of documents read.
    */
    int readDocs(String filename) {
        int fileIndex = 0;
        try {
            System.err.print("Reading file... ");
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS) {
                int index = line.indexOf(";");
                String title = line.substring(0, index);
                Integer fromdoc = docNumber.get(title);
                //  Have we seen this document before?
                if (fromdoc == null) {
                    // This is a previously unseen doc, so add it to the table.
                    fromdoc = fileIndex++;
                    docNumber.put(title, fromdoc);
                    docName[fromdoc] = title;
                }
                // Check all outlinks.
                StringTokenizer tok = new StringTokenizer(line.substring(index+1), ",");
                while (tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS) {
                    String otherTitle = tok.nextToken();
                    Integer otherDoc = docNumber.get(otherTitle);
                    if (otherDoc == null) {
                        // This is a previousy unseen doc, so add it to the table.
                        otherDoc = fileIndex++;
                        docNumber.put(otherTitle, otherDoc);
                        docName[otherDoc] = otherTitle;
                    }
                    // Set the probability to 0 for now, to indicate that there is
                    // a link from fromdoc to otherDoc.
                    if (link.get(fromdoc) == null) {
                        link.put(fromdoc, new HashMap<Integer,Boolean>());
                    }
                    if (link.get(fromdoc).get(otherDoc) == null) {
                        link.get(fromdoc).put(otherDoc, true);
                        out[fromdoc]++;
                    }
                }
            }
            if (fileIndex >= MAX_NUMBER_OF_DOCS) {
                System.err.print("stopped reading since documents table is full. ");
            }
            else {
                System.err.print("done. ");
            }
        }
        catch (FileNotFoundException e) {
            System.err.println("File " + filename + " not found!");
        }
        catch (IOException e) {
            System.err.println("Error reading file " + filename);
        }
        System.err.println("Read " + fileIndex + " number of documents");
        return fileIndex;
    }


    /* --------------------------------------------- */

    void iterateWithAlgorithm1(int numberOfDocs, int N) {
        double[] count = new double[MAX_NUMBER_OF_DOCS];
        double[] count_copy = new double[MAX_NUMBER_OF_DOCS];
        Arrays.fill(count, 0.0);
        Random r = new Random();

        for(int i = 0; i < N; i++){
            // We chose a random page
            int pos = ((new Random().nextInt()% numberOfDocs) + numberOfDocs) % numberOfDocs;
            double probaToBeBored = r.nextDouble();
            while(probaToBeBored > BORED && out[pos] > 0){ // One run
                  ArrayList<Integer> targetList = new ArrayList<Integer>(link.get(pos).keySet());
                  pos = targetList.get(r.nextInt(out[pos])); // We chose a new page to jump from the possibility
                  probaToBeBored = r.nextDouble();
            }
           count[pos] += 1;
          }

        for(int j = 0; j < numberOfDocs; j++){
            count[j] /= N;
        }

        printKBest(count, 30);
    }

    /* --------------------------------------------- */

    void iterateWithAlgorithm2(int numberOfDocs, int N, int M) {
        double[] count = new double[MAX_NUMBER_OF_DOCS];
        double[] count_copy = new double[MAX_NUMBER_OF_DOCS];
        Arrays.fill(count, 0.0);
        Random r = new Random();

        for(int d = 0;d<numberOfDocs;d++) {
            for(int i = 0; i < M; i++){
                for(int j=0; j < N; j++) {
                    int pos = d;
                    double probaToBeBored = r.nextDouble();
                    while(probaToBeBored > BORED && out[pos] > 0){ // One run
                          ArrayList<Integer> targetList = new ArrayList<Integer>(link.get(pos).keySet());
                          pos = targetList.get(r.nextInt(out[pos])); // We chose a new page to jump from the possibility
                          probaToBeBored = r.nextDouble();
                    }
                   count[pos] += 1;
                 }
             }
         }

        for(int j = 0; j < numberOfDocs; j++){
            count[j] /= N * M;
        }

        printKBest(count, 30);
    }

    /* --------------------------------------------- */

    void iterateWithAlgorithm4(int numberOfDocs, int N, int M) {
        double[] count = new double[MAX_NUMBER_OF_DOCS];
        double[] count_copy = new double[MAX_NUMBER_OF_DOCS];
        Arrays.fill(count, 0.0);
        Random r = new Random();
        double nbVisiting = 0;

        for(int i = 0; i < M; i++){
            for(int j=0; j < N; j++) {
                int pos = i;
                double probaToBeBored = r.nextDouble();
                while(probaToBeBored > BORED && out[pos] > 0){ // One run
                      ArrayList<Integer> targetList = new ArrayList<Integer>(link.get(pos).keySet());
                      pos = targetList.get(r.nextInt(out[pos])); // We chose a new page to jump from the possibility
                      probaToBeBored = r.nextDouble();
                      nbVisiting+=1;
                }
               count[pos] += 1;
             }
         }

        for(int j = 0; j < numberOfDocs; j++){
            count[j] /= nbVisiting;
        }

        printKBest(count, 30);
    }


    /* --------------------------------------------- */

    /*
    *   Chooses a probability vector a, and repeatedly computes
    *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
    */
    void iterate(int numberOfDocs, int maxIterations) {
        long begin, end, current, last;
        begin = last = System.currentTimeMillis();
        double[] xPrime = new double[numberOfDocs];
        xPrime[0] = 1.0;
        double[] x = new double[numberOfDocs];
        int[] in = new int[MAX_NUMBER_OF_DOCS];
        int iteration = -1;
        double coefBored = BORED / (double) numberOfDocs;
        double coefNumberOfDocs = 1.0 / (double) numberOfDocs;
        double coefNumberOfDocsOfI;
        double coefLinkExists;
        double coefBoredOfI;
        double coefLinkExistsOfI;
        for(Map.Entry<Integer, HashMap<Integer,Boolean>> entry : link.entrySet()) {
            for(Map.Entry<Integer, Boolean> entry2 : entry.getValue().entrySet()) {
                if(entry2.getValue()) {
                    in[entry2.getKey()]++;
                }
            }
        }
        HashMap<Integer,Boolean> row;

        do {
            x = Arrays.copyOf(xPrime, xPrime.length);
            Arrays.fill(xPrime, 0);
            for(int i=0;i<numberOfDocs;i++) {
                if(out[i] > 0) {
                    row = link.get(new Integer(i));
                    coefLinkExists = (1.0-BORED) / out[i] + coefBored;
                    coefBoredOfI = x[i] * coefBored;
                    coefLinkExistsOfI = x[i] * coefLinkExists;
                    for(int j=0;j<numberOfDocs;j++) {
                        if(in[j] > 0 && row.get(new Integer(j)) != null) {
                            xPrime[j] += coefLinkExistsOfI;
                        } else {
                            xPrime[j] += coefBoredOfI;
                        }
                    }
                } else {
                    coefNumberOfDocsOfI = x[i] * coefNumberOfDocs;
                    for(int j=0;j<numberOfDocs;j++) {
                        xPrime[j] += coefNumberOfDocsOfI;
                    }
                }
            }

            iteration++;
            System.out.println("Iteration : " + Integer.toString(iteration));
            System.out.println("Time : " + Double.toString((System.currentTimeMillis() - last) / 1000.0) + " seconds.");
            last = System.currentTimeMillis();
        } while(normOfVector(difference(x, xPrime)) > EPSILON && iteration <= maxIterations);


        printKBest(xPrime, 30);
    }

    /* --------------------------------------------- */

    public void printKBest(double[] array, int k) {
        double[] array_copy = Arrays.copyOf(array, array.length);
        ArrayList<Integer> rank = new ArrayList<Integer>();
        for(int i=0;i<k;i++) {
            int best = 0;
            double max = array[0];
            for(int j=1;j<array.length;j++) {
                if(array[j] > max) {
                    best = j;
                    max = array[j];
                }
            }
            array[best] = Double.NEGATIVE_INFINITY;
            rank.add(best);
        }

        for(int i=0;i<rank.size();i++) {
            System.out.println(docName[rank.get(i)] + " -> " + Double.toString(array_copy[rank.get(i)]));
        }
    }

    /* --------------------------------------------- */

    public double[] difference(double[] A, double[] B) {
        double[] C = new double[A.length];
        for(int i=0;i<A.length;i++) {
            C[i] = Math.abs(A[i] - B[i]);
        }
        return C;
    }

    /* --------------------------------------------- */

    public double normOfVector(double[] A) {
        double norm = 0;
        for(int i=0;i<A.length;i++) {
            norm += A[i];
        }
        return norm;
    }

    /* --------------------------------------------- */


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please give the name of the link file");
        }
        else {
            new PageRankMonteCarlo(args[0]);
        }
    }
}

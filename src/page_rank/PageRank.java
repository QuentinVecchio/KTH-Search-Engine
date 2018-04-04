import java.util.*;
import java.io.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map.Entry;

public class PageRank {
    public class Tuple<X, Y> {
      public final X x;
      public final Y y;
      public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
      }
    }

    /**
    *   Maximal number of documents. We're assuming here that we
    *   don't have more docs than we can keep in main memory;
    */
    final static int MAX_NUMBER_OF_DOCS = 1000;

    /**
    *   Mapping from document names to document numbers.
    */
    Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();

    /**
    *   Mapping from document numbers to document names
    */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**
    *   The transition matrix. p[i][j] = the probability that the
    *   random surfer clicks from page i to page j.
    */
    double[][] p = new double[MAX_NUMBER_OF_DOCS][MAX_NUMBER_OF_DOCS];

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
    *   In the initializaton phase, we use a negative number to represent
    *   that there is a direct link from a document to another.
    */
    final static double LINK = -1.0;

    /**
    *   Convergence criterion: Transition probabilities do not
    *   change more that EPSILON from one iteration to another.
    */
    final static double EPSILON = 0.0001;


    /* --------------------------------------------- */


    public PageRank(String filename) {
        int noOfDocs = readDocs(filename);
        // double[][] A = {{2,3,4}, {8,10,1}, {9,23,10}};
        // double[] v = {8,2,5};
        // double[] xPrime = multiplyVectorWithMatrices(v, A, 3);
        // for(int i=0;i<A.length;++i) {
        //     String s = "";
        //     for(int j=0;j<A[i].length;++j) {
        //         s += Double.toString(A[i][j]) + " ";
        //     }
        //     System.out.println(s);
        // }
        // System.out.println("-----");
        // String s = "";
        // for(int j=0;j<v.length;++j) {
        //     s += Double.toString(v[j]) + " ";
        // }
        // System.out.println(s);
        // System.out.println("-----");
        // s = "";
        // for(int j=0;j<xPrime.length;++j) {
        //     s += Double.toString(xPrime[j]) + " ";
        // }
        // System.out.println(s);

        initiateProbabilityMatrix(noOfDocs);
        iterate(noOfDocs, 100);
    }


    /* --------------------------------------------- */


    /**
    *   Reads the documents and fills the data structures. When this method
    *   finishes executing, <code>p[i][j] = LINK</code> if there is a direct
    *   link from i to j, and <code>p[i][j] = 0</code> otherwise.
    *   <p>
    *
    *   @return the number of documents read.
    */
    int readDocs( String filename ) {
        int fileIndex = 0;
        try {
            System.err.print( "Reading file... " );
            BufferedReader in = new BufferedReader( new FileReader( filename ));
            String line;
            while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
                int index = line.indexOf( ";" );
                String title = line.substring( 0, index );
                Integer fromdoc = docNumber.get( title );
                //  Have we seen this document before?
                if ( fromdoc == null ) {
                    // This is a previously unseen doc, so add it to the table.
                    fromdoc = fileIndex++;
                    docNumber.put( title, fromdoc );
                    docName[fromdoc] = title;
                }
                // Check all outlinks.
                StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
                while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
                    String otherTitle = tok.nextToken();
                    Integer otherDoc = docNumber.get( otherTitle );
                    if ( otherDoc == null ) {
                        // This is a previousy unseen doc, so add it to the table.
                        otherDoc = fileIndex++;
                        docNumber.put( otherTitle, otherDoc );
                        docName[otherDoc] = otherTitle;
                    }
                    // Set the probability to LINK for now, to indicate that there is
                    // a link from d to otherDoc.
                    if ( p[fromdoc][otherDoc] >= 0 ) {
                        p[fromdoc][otherDoc] = LINK;
                        out[fromdoc]++;
                    }
                }
            }
            if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
                System.err.print( "stopped reading since documents table is full. " );
            }
            else {
                System.err.print( "done. " );
            }
        }
        catch ( FileNotFoundException e ) {
            System.err.println( "File " + filename + " not found!" );
        }
        catch ( IOException e ) {
            System.err.println( "Error reading file " + filename );
        }
        System.err.println( "Read " + fileIndex + " number of documents" );
        return fileIndex;
    }




    /* --------------------------------------------- */


    /*
    *   Initiates the probability matrix.
    */
    /**
    *   The transition matrix. p[i][j] = the probability that the
    *   random surfer clicks from page i to page j.
    */
    void initiateProbabilityMatrix(int numberOfDocs) {
        for(int i=0;i<numberOfDocs;i++) {
            for(int j=0;j<numberOfDocs;j++) {
                if (out[i] != 0) {
                    if(p[i][j] == LINK) {
                        p[i][j] = (1.0-BORED) / out[i] + BORED / (double) numberOfDocs;
                    } else {
                        p[i][j] = BORED / (double) numberOfDocs;;
                    }
                } else {
                    p[i][j] = 1.0 / (double) numberOfDocs;
                }
            }
        }
    }


    /* --------------------------------------------- */


    /*
    *   Chooses a probability vector a, and repeatedly computes
    *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
    */
    void iterate(int numberOfDocs, int maxIterations) {
        double[] xPrime = new double[numberOfDocs];
        xPrime[0] = 1.0;
        double[] x = new double[numberOfDocs];
        int iteration = -1;

        do {
            x = Arrays.copyOf(xPrime, xPrime.length);
            xPrime = multiplyVectorWithMatrices(x, this.p, numberOfDocs);
            iteration++;
            System.out.println("Iteration : " + Integer.toString(iteration));
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


    public static void main( String[] args ) {
        if ( args.length != 1 ) {
            System.err.println( "Please give the name of the link file" );
        }
        else {
            new PageRank( args[0] );
        }
    }

    public double getRandomDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public int getRandomInt(int min, int max) {
        Random rnd = ThreadLocalRandom.current();
        return rnd.nextInt((max - min) + 1) + min;
    }

    public void shuffleArray(double[] ar) {
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            double a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    public void printMatrix(double[][] ar, int size) {
        for(int i=0;i<size;i++) {
            String s = "";
            for(int j=0;j<size;j++) {
                s += Double.toString(ar[i][j]) + " ";
            }
            System.out.println(s);
        }
    }

    public double[] multiplyVectorWithMatrices(double[] vector, double[][] A, int size) {
        double[] C = new double[vector.length];
        for(int j=0;j<size;j++) {
            double value = 0.0;
            for(int i=0;i<size;i++) {
                value += vector[i] * A[i][j];
            }
            C[j] = value;
        }
        return C;
    }

    public double[] difference(double[] A, double[] B) {
        double[] C = new double[A.length];
        for(int i=0;i<A.length;i++) {
            C[i] = Math.abs(A[i] - B[i]);
        }
        return C;
    }

    public boolean isSame(double[] A, double[] B) {
        for(int i=0;i<A.length;i++) {
            if(A[i] != B[i]) {
                return false;
            }
        }
        return true;
    }

    public double normOfVector(double[] A) {
        double norm = 0;
        for(int i=0;i<A.length;i++) {
            norm += A[i];
        }
        return norm;
    }
}

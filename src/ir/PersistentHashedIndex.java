/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, KTH, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;
import static java.lang.Math.*;

/*
 *   Implements an inverted index as a hashtable on disk.
 *
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks.
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The dictionary file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static long TABLESIZE = 611953L;  // 100,000th prime number

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;


    FileOutputStream fout;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();

    public final long SIZE = 200;
    public boolean firtTime = true;

    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */
    public class Entry {
        public String token = null;
        public long location = 0;
        public int size = 0;
        public String separator = "|";

        public Entry() {}

        public Entry(String s) {
            //System.out.println(s);
            String[] elts = s.split("\\" + separator);
            if(elts.length == 4) {
                this.token = elts[0];
                this.location = Long.parseLong(elts[1]);
                this.size = Integer.parseInt(elts[2]);
            }
        }

        public Entry(String token, int location, int size) {
            this.token = token;
            this.location = location;
            this.size = size;
        }

        public String toString() {
            //token;location;size
            String data = separator + Long.toString(this.location) + separator + Integer.toString(this.size) + separator;
            String s = "";
            for(int i=0;i<this.token.length()&&i<SIZE-data.length();++i) {
                s += this.token.charAt(i);
            }
            s += data;
            for(int i=s.length();i < SIZE;++i) {
                s += "_";
            }
            //System.out.println(s);
            return s;
        }
    }


    // ==================================================================


    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created.
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        try {
            readDocInfo();
        }
        catch ( FileNotFoundException e ) {
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */
    int writeData(String dataString, long ptr) {
        try {
            dataFile.seek(ptr);
            byte[] data = dataString.getBytes("UTF-8");
            dataFile.write(data);
            return data.length;
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */
    String readData(long ptr, int size) {
        try {
            dataFile.seek(ptr);
            byte[] data = new byte[size];
            dataFile.readFully(data);
            return new String(data);
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file.
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry(Entry entry, long ptr) {
        try {
            dictionaryFile.seek(ptr);
            byte[] data = entry.toString().getBytes("UTF-8");
            dictionaryFile.write(data);
            //System.out.println("Size " + data.length);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry(long ptr) {
        try{
            dictionaryFile.seek(ptr);
            byte[] data = new byte[(int)SIZE];
            dictionaryFile.readFully(data);
            Entry entry = new Entry(new String(data));
            if(entry.token != null) {
                // System.out.println("collision");
                // System.out.println(new String(data));
                return entry;
            } else {
                return null;
            }
        } catch(EOFException e){
            return null;
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    public void writeDocInfo() throws IOException {
        fout = new FileOutputStream( INDEXDIR + "/docInfo", !firtTime);
        firtTime = false;
        for (Map.Entry<Integer,String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes("UTF-8"));
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    public void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
               String[] data = line.split(";");
               docNames.put(new Integer(data[0]), data[1]);
               docLengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;

        this.free = 0;
        int i = 0;
        // Write the dictionary and the postings list
        for(String key : index.keySet()) {
            i++;
            Entry entry = new Entry();
            entry.token = key;
            //  Manage the Posting List
            String postingsListStr = index.get(key).toString();
            entry.location = this.free;
            int size = this.writeData(postingsListStr, this.free);
            entry.size = size;
            this.free += size + 1;

            // Manage the entry
            long ptr = this.hash(key);
            //System.out.println("Hash : " + ptr);
            Entry bucket = readEntry(ptr);
            while(bucket != null) {
                ptr += SIZE;
                bucket = readEntry(ptr);
                collisions++;
            }

            this.writeEntry(entry, ptr);
            if(i % 10000 == 0) {
                System.out.println(Integer.toString(i) + " tokens written.");
            }
        }

        System.out.println( index.size() + " tokens.");
        System.out.println( collisions + " collisions." );
    }


    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings(String token) {
        long h = this.hash(token);
        //System.out.println("Hash : " + h);
        Entry entry = this.readEntry(h);
        if(entry != null) {
            long ptr = 0;
            while(entry != null && !entry.token.equals(token)) {
                entry = this.readEntry(h + ptr);
                ptr += SIZE;
            }
            if(entry != null && entry.token != null) {
                return new PostingsList(this.readData(entry.location, entry.size));
            }
        }
        return null;
    }


    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        if(token != null && docID >= 0 && offset >= 0) {
            if(!index.containsKey(token)) {
                PostingsList list = new PostingsList();
                index.put(token,list);
            }
            index.get(token).add(docID, 0, offset);
        }
    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.out.println( index.keySet().size() + " unique words" );
        System.out.print( "Writing index to disk..." );
        try {
            writeDocInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }
        writeIndex();
        System.out.println( "done!" );
     }

     /**
      *  Hash a given string.
      */
     public long hash(String token) {
         long hash = 0;
         for(int i=0;i<token.length();++i) {
             hash = token.charAt(i) + (hash << 6) + (hash << 16) - hash;
             hash = ((hash<<5) - hash)+token.charAt(i);
             hash = ((hash << 5) + hash) + token.charAt(i);
             hash %= (SIZE * TABLESIZE);
         }
         long q = hash / SIZE;
         hash = q * SIZE;
         return abs(hash);
     }

}

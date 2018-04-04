package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;
import static java.lang.Math.*;
import java.io.File;
import java.lang.Thread;

public class PersistentScalableHashedIndex extends PersistentHashedIndex {
    public final int BLOCKSIZE = 50000;
    public int indexNumber = -1;
    public ArrayList<String> indexsNumbers;
    public Thread threadMerger;
    public boolean indexingFinished = false;
    public ArrayList<Thread> threads = new ArrayList<Thread>();

    public PersistentScalableHashedIndex() {
        super();
        //this.TABLESIZE = 10000000;
        indexsNumbers = new ArrayList<String>();
        threadMerger = new Thread(){
            ArrayList<Thread> threads = new ArrayList<Thread>();

            public void run() {
                while(!indexingFinished) {
                    Thread thread = null;
                    synchronized(indexsNumbers) {
                        if(indexsNumbers.size() >= 2) {
                            thread = new Thread() {
                                public void run() {
                                    String s1 = "";
                                    String s2 = "";
                                    synchronized(indexsNumbers) {
                                        if(indexsNumbers.size() >= 2) {
                                            s1 = indexsNumbers.get(0);
                                            s2 = indexsNumbers.get(1);
                                            indexsNumbers.remove(0);
                                            indexsNumbers.remove(0);
                                        } else {
                                            System.out.println("Erreur");
                                            return;
                                        }
                                    }
                                    System.out.println("Merge block " + s1 + " and " + s2 + "...");
                                    mergeIndex(s1, s2);
                                    System.out.println("Merge block " + s1 + " and " + s2 + " finished");
                                }
                            };
                        }
                    }
                    if(thread != null) {
                        threads.add(thread);
                        thread.start();
                    }
                    try {
                        Thread.sleep(500);
                    } catch(InterruptedException e) {

                    }
                }
                boolean threadRunning = true;
                while(threadRunning) {
                    threadRunning = false;
                    while(indexsNumbers.size() >= 2) {
                        Thread thread = new Thread() {
                            public void run() {
                                String s1 = "";
                                String s2 = "";
                                synchronized(indexsNumbers) {
                                    s1 = indexsNumbers.get(0);
                                    s2 = indexsNumbers.get(1);
                                    indexsNumbers.remove(0);
                                    indexsNumbers.remove(0);
                                }
                                System.out.println("Merge block " + s1 + " and " + s2 + "...");
                                mergeIndex(s1, s2);
                                System.out.println("Merge block " + s1 + " and " + s2 + " finished");
                            }
                        };
                        threads.add(thread);
                        thread.start();
                        try {
                            Thread.sleep(500);
                        } catch(InterruptedException e) {

                        }
                    }
                    while(threads.size() > 0) {
                        threadRunning = true;
                        try {
                            threads.get(0).join();
                        } catch(InterruptedException e) {

                        }
                        threads.remove(0);
                    }
                    System.out.println(indexsNumbers.size());
                }
                finish();
            }
        };
        threadMerger.start();
    }

    /**
     *  Inserts this token in the main-memory hashtable.
    */
    public void insert( String token, int docID, int offset ) {
        if(indexNumber == -1) {
            indexNumber++;
            prepareNewIndex();
        }
        if(token != null && docID >= 0 && offset >= 0) {
            if(!index.containsKey(token)) {
                PostingsList list = new PostingsList();
                this.index.put(token,list);
            }
            this.index.get(token).add(docID, 0, offset);
        }
        if(this.index.size() >= BLOCKSIZE) {
            //System.out.println("BLOCKSIZE reached.");
            writeIndex();
        }
    }

    public void writeIndex() {
        //System.out.println("Write index " + Integer.toString(this.indexNumber));
        super.writeIndex();
        synchronized(indexsNumbers) {
            indexsNumbers.add(Integer.toString(this.indexNumber));
        }
        this.indexNumber++;
        prepareNewIndex();
    }

    public void prepareNewIndex() {
        // System.out.println("Prepare new index " + Integer.toString(this.indexNumber));
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + Integer.toString(this.indexNumber), "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + Integer.toString(this.indexNumber), "rw" );
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        this.free = 0;
        this.index.clear();
        // this.docNames.clear();
        // this.docLengths.clear();
    }

    public void mergeIndex(String n1, String n2) {
        try {
            RandomAccessFile dictionaryTmp = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + n1 + "_" + n2, "rw" );
            RandomAccessFile dataTmp = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + n1 + "_" + n2, "rw" );
            RandomAccessFile dictionary1 = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + n1, "rw" );
            RandomAccessFile dictionary2 = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + n2, "rw" );
            RandomAccessFile data1 = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + n1, "rw" );
            RandomAccessFile data2 = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + n2, "rw" );
            HashMap<String,Entry> dic1 = readDictionary(dictionary1);
            //System.out.println("Dic 1 : " + dic1.size() + " elements.");
            HashMap<String,Entry> dic2 = readDictionary(dictionary2);
            //System.out.println("Dic 2 : " + dic2.size() + " elements.");
            long ptr = 0;
            // PostingsList list1;
            // PostingsList list2;

            for(Map.Entry<String, Entry> entry : dic1.entrySet()) {
                PostingsList list1 = new PostingsList(readData(data1, entry.getValue().location, entry.getValue().size));
                if(dic2.containsKey(entry.getKey())) {
                    PostingsList list2 = new PostingsList(readData(data2, dic2.get(entry.getKey()).location, dic2.get(entry.getKey()).size));
                    if(list1.list.size() >= list2.list.size()) {
                        list1.merge(list2);
                        // for(int i=0;i<list1.list.size()-1;i++) {
                        //     if(list1.get(i).docID > list1.get(i+1).docID) {
                        //         System.out.println("ERRRRRRRRRRRRRORRRRRRRR");
                        //         System.out.println(list1.toString());
                        //         System.out.println(list2.toString());
                        //         break;
                        //     }
                        // }
                        ptr += this.writeEntry(dictionaryTmp, dataTmp, entry.getKey(), list1, ptr); // We write the token and posting list here
                    } else {
                        list2.merge(list1);
                        // for(int i=0;i<list2.list.size()-1;i++) {
                        //     if(list2.get(i).docID > list2.get(i+1).docID) {
                        //         System.out.println("ERRRRRRRRRRRRRORRRRRRRR");
                        //         System.out.println(list1.toString());
                        //         System.out.println(new PostingsList(readData(data2, dic2.get(entry.getKey()).location, dic2.get(entry.getKey()).size)).toString());
                        //         System.out.println(list2.toString());
                        //     }
                        // }
                        ptr += this.writeEntry(dictionaryTmp, dataTmp, entry.getKey(), list2, ptr); // We write the token and posting list here
                    }
                    dic2.remove(entry.getKey()); // Token proceed
                } else {
                    ptr += this.writeEntry(dictionaryTmp, dataTmp, entry.getKey(), list1, ptr); // We write the token and posting list here
                }
                ptr += 1;
            }
            // System.out.println("Dic 1 finished");
            // System.out.println("Dic 2 " + dic2.size() + " keys left.");
            for(Map.Entry<String, Entry> entry : dic2.entrySet()) {
                PostingsList list1 = new PostingsList(readData(data2, entry.getValue().location, entry.getValue().size));
                ptr += this.writeEntry(dictionaryTmp, dataTmp, entry.getKey(), list1, ptr);
                ptr += 1;
            }
            //System.out.println("Dic 2 finished");

            synchronized(indexsNumbers) {
                indexsNumbers.add(n1+"_"+n2);
            }
            this.removeFile(INDEXDIR + "/" + DICTIONARY_FNAME + n1);
            this.removeFile(INDEXDIR + "/" + DICTIONARY_FNAME + n2);
            this.removeFile(INDEXDIR + "/" + DATA_FNAME + n1);
            this.removeFile(INDEXDIR + "/" + DATA_FNAME + n2);
            // HashMap<String,Entry> test = readDictionary(dictionaryTmp);
            // System.out.println("test 1 : " + dic1.size() + " elements.");
        } catch ( IOException e ) {
        }
    }

    public void cleanup() {
        if(index.size() > 0) {
            super.writeIndex();
            synchronized(indexsNumbers) {
                indexsNumbers.add(Integer.toString(this.indexNumber));
            }
        }
        indexingFinished = true;
    }

    public void finish() {
        if(indexsNumbers.size() > 0) {
            removeFile( INDEXDIR + "/" + DICTIONARY_FNAME);
            removeFile( INDEXDIR + "/" + DATA_FNAME);
            renameFile( INDEXDIR + "/" + DICTIONARY_FNAME + indexsNumbers.get(0), INDEXDIR + "/" + DICTIONARY_FNAME);
            renameFile( INDEXDIR + "/" + DATA_FNAME + indexsNumbers.get(0), INDEXDIR + "/" + DATA_FNAME);
            try {
                writeDocInfo();
                dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
                dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
                // HashMap<String,Entry> test = readDictionary(dictionaryFile);
                // System.out.println("Test : " + test.size() + " elements.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println( "done!" );
    }

    public HashMap<String,Entry> readDictionary(RandomAccessFile file) {
        long ptr = 0;
        HashMap<String,Entry> dic = new HashMap<String,Entry>();
        try {
            while(true) {
                file.seek(ptr);
                byte[] data = new byte[(int) SIZE];
                file.readFully(data);
                Entry entry = new Entry(new String(data));
                if(entry.token != null) {
                    dic.put(entry.token, entry);
                }
                ptr += SIZE;
            }
        }
        catch ( IOException e ) {
        }
        return dic;
    }

    public int writeEntry(RandomAccessFile dictionary, RandomAccessFile data, String token, PostingsList list, long ptr) {
        Entry entry = new Entry();
        entry.token = token;
        entry.location = ptr;
        int size = this.writeData(data, list.toString(), ptr);
        entry.size = size;

        long hash = this.hash(token);
        Entry bucket = readEntry(dictionary, hash);

        while(bucket != null) {
            hash += SIZE;
            bucket = readEntry(dictionary, hash);
        }

        this.writeEntry(dictionary, entry, hash);
        return size;
    }

    public int writeData(RandomAccessFile file, String dataString, long ptr) {
        try {
            file.seek(ptr);
            byte[] data = dataString.getBytes("UTF-8");
            file.write(data);
            return data.length;
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }

    public String readData(RandomAccessFile file, long ptr, int size) {
        try {
            file.seek(ptr);
            byte[] data = new byte[size];
            file.readFully(data);
            return new String(data);
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }

    public void writeEntry(RandomAccessFile file, Entry entry, long ptr) {
        try {
            file.seek(ptr);
            byte[] data = entry.toString().getBytes("UTF-8");
            file.write(data);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public Entry readEntry(RandomAccessFile file, long ptr) {
        try{
            file.seek(ptr);
            byte[] data = new byte[(int) SIZE];
            file.readFully(data);
            Entry entry = new Entry(new String(data));
            if(entry.token != null) {
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

    public void removeFile(String path) {
        try {
    		File file = new File(path);
    		file.delete();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }

    public void renameFile(String path, String newPath) {
        try {
            File file = new File(path);
            File file2 = new File(newPath);
            file.renameTo(file2);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

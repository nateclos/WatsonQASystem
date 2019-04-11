package edu.arizona.cs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

public class Indexer {
	
	private FSDirectory index;
	private StandardAnalyzer analyzer;
	
	public Indexer(String input_file) throws IOException {
		
		ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(input_file).getFile());
        Scanner s = new Scanner(file);
        int end = 100;
        while(end != 0) {
        		String curr = s.next();
        		System.out.println(curr);
        		end--;
        }
        /*this.analyzer = new StandardAnalyzer();
        this.index = FSDirectory.open(Paths.get("/Users/nateclos/Documents/cs483/WatsonQASystem/src/main/resources/index.lucene"));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);
        w.close();
        index.close();*/
	}
	
	public static void main(String[] args) throws IOException {
		
		Indexer i = new Indexer("enwiki-20140602-pages-articles.xml-0005.txt");
		
	}
}
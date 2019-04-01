package edu.arizona.cs;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class Indexer {
	
	public Indexer(String input_file) throws IOException {
		
		ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(input_file).getFile());
        
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);
        
	}
	
	public static void main(String[] args) {
		
		File f = new File(".");
		System.out.println(f.getAbsolutePath());
	}
}
package edu.arizona.cs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

public class Indexer {
	
	private FSDirectory index;
	private StandardAnalyzer analyzer;
	private String[] exclusions = new String[] {"[[File:", "[[Image:", "[[Media:"};
	
	public Indexer(String input_file) throws IOException {
		
		ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(input_file).getFile());
        Scanner s = new Scanner(file);
        
        this.analyzer = new StandardAnalyzer();
        this.index = FSDirectory.open(Paths.get("/Users/nateclos/Documents/cs483/WatsonQASystem/src/main/resources/index.lucene"));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);
        
        while(s.hasNextLine()) {
        		String curr = s.nextLine();
        		if(curr.startsWith("[[") && !isMedia(curr)) {
        			addNewDoc(w, s, curr);
        			while(true) {
        				if(curr.endsWith("]]")) {
        					curr = s.nextLine();
        					break;
        				}
        				curr = s.nextLine();
        			}
        		}
        }
        w.close();
        index.close();
	}
	
	private void addNewDoc(IndexWriter w, Scanner s, String curr) {
		System.out.println(curr);
		curr = curr.replace("[", "");
		String title = curr;
		if(!curr.endsWith("]]")) {
			while(!curr.endsWith("]]")) {
				title += curr;
				curr = s.nextLine();
			}
			curr.replace("]", "");
			title += curr;
		} else {
			title = title.replace("]]", "");
		}
		System.out.println(title);
		Document newDoc = new Document();
		
		s.nextLine();
		curr = s.nextLine();
		String document = "";
		
		if(curr.startsWith("CATEGORIES:")) {
			addCategories(newDoc, curr, s);
		} else {
			System.out.println("NO CATEGORIES");
		}
		
	}
	
	private void addCategories(Document newDoc, String categories, Scanner s) {
		
		categories = categories.replace("CATEGORIES: ", "");
		String curr = s.nextLine();
		while(!curr.isEmpty()) {
			categories += curr;
			curr = s.nextLine();
		}
		System.out.println(categories);
		//newDoc.add(new TextField("categories", categories, Field.Store.YES));
	}
		
	private boolean isMedia(String line) {
		
		for(String curr : this.exclusions) {
			if(line.startsWith(curr)) return true;
		}
		return false;
	}
	
	public static void main(String[] args) throws IOException {
		
		Indexer i = new Indexer("enwiki-20140602-pages-articles.xml-0005.txt");
		
	}
}
package edu.arizona.cs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

public class Indexer {
	
	private FSDirectory index;
	private StandardAnalyzer analyzer;
	private String[] exclusions = new String[] {"[[File:", "[[Image:", "[[Media:"};
	
	public Indexer(File currDir) throws IOException {
		
		File oldIndex = new File("/Users/nateclos/Documents/cs483/WatsonQASystem/src/main/resources/index.lucene");
		if(oldIndex.exists()) {
			this.analyzer = new StandardAnalyzer();
			this.index = FSDirectory.open(Paths.get("/Users/nateclos/Documents/cs483/WatsonQASystem/src/main/resources/index.lucene"));
			return;
		}
		
		this.analyzer = new StandardAnalyzer();
        this.index = FSDirectory.open(Paths.get("/Users/nateclos/Documents/cs483/WatsonQASystem/src/main/resources/index2.lucene"));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);
        
		for(File name : currDir.listFiles()) {
			System.out.println("Indexing " + name.getName());
			if(!name.getName().startsWith("enwiki")) continue;
			
			ClassLoader classLoader = getClass().getClassLoader();
	        File file = new File(classLoader.getResource(name.getName()).getFile());
	        Scanner s = new Scanner(file);
	        
	        while(s.hasNextLine()) {
	        		String curr = s.nextLine();
	        		if(curr.startsWith("[[") && !isMedia(curr)) {
	        			Scanner newS = s;
	        			addNewDoc(w, newS
	        					, curr);
	        		}
	        }
	        break;
	        
		}
		w.close();
	}
		
	
	private void addNewDoc(IndexWriter w, Scanner s, String curr) throws IOException {
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
		Document newDoc = new Document();
		newDoc.add(new StringField("title", title, Field.Store.YES));
		
		s.nextLine();
		curr = s.nextLine();
		if(curr.startsWith("CATEGORIES:")) {
			addCategories(newDoc, curr, s);
		} else {
			newDoc.add(new TextField("categories", "NO CATEGORIES", Field.Store.YES));
		}
		curr = s.nextLine();
		String document = "";
		while(!curr.startsWith("[[")) {
			document += curr;
			curr = s.nextLine();
		}
		newDoc.add(new TextField("document", document, Field.Store.YES));
		System.out.println("Adding document with title: " + title);
		w.addDocument(newDoc);
	}
	
	private void addCategories(Document newDoc, String categories, Scanner s) {
		
		categories = categories.replace("CATEGORIES: ", "");
		String curr = s.nextLine();
		while(!curr.isEmpty()) {
			categories += curr;
			curr = s.nextLine();
		}
		newDoc.add(new TextField("categories", categories, Field.Store.YES));
	}
		
	private boolean isMedia(String line) {
		
		for(String curr : this.exclusions) {
			if(line.startsWith(curr)) return true;
		}
		return false;
	}
	
	public void queryIndex(String query) throws ParseException, IOException {
		
		Query q = new QueryParser("categories", this.analyzer).parse(query);
		int hitNum = 10;
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(new String[] {"categories", "document"},analyzer);
		//TopDocs docs = searcher.search(queryParser.parse(query), hitNum);
		TopDocs docs = searcher.search(q, hitNum);
		ScoreDoc[] hits = docs.scoreDocs;
		
		System.out.println("Found " + hits.length + " hits.");
		for(int i=0;i<hits.length;++i) {
		    int docId = hits[i].doc;
		    float score = hits[i].score;
		    Document d = searcher.doc(docId);
		    	System.out.println((i + 1) + ". " + d.get("title") + " " + score + " " + d.get("categories"));
		}
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		
		File currDir = new File("src/main/resources");
		Indexer i = new Indexer(currDir);
		i.queryIndex("HISTORICAL QUOTES");
		i.index.close();
	}
}
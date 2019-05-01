package edu.arizona.cs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
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
        this.index = FSDirectory.open(Paths.get("/Users/nateclos/Documents/cs483/WatsonQASystem/src/main/resources/index.lucene"));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);
        
		for(File name : currDir.listFiles()) {
			System.out.println("\nIndexing " + name.getName() + "\n");
			if(!name.getName().startsWith("enwiki")) continue;
			
			ClassLoader classLoader = getClass().getClassLoader();
	        File file = new File(classLoader.getResource(name.getName()).getFile());
	        Scanner s = new Scanner(file);
	        String curr = s.nextLine();
    			if(curr.startsWith("[[") && !isMedia(curr))
    				curr = addNewDoc(w, s, curr);
	        while(s.hasNextLine()) {
	        		curr = addNewDoc(w,s,curr);
	        }
	        
		}
		w.close();
	}
		
	
	private String addNewDoc(IndexWriter w, Scanner s, String curr) throws IOException {
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
		while(!isTitle(curr) && s.hasNextLine()) {
			document += curr;
			if(!curr.isEmpty()) document += '\n';
			curr = s.nextLine();
		}
		newDoc.add(new TextField("document", document, Field.Store.YES));
		try {
			w.addDocument(newDoc);
		} catch(IllegalArgumentException e) {
			System.out.println(newDoc.get("title"));
			System.out.println(newDoc.get("categories"));
			System.out.println(newDoc.get("document"));
			System.exit(1);
		}
		
		return curr;
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
	
	private boolean isTitle(String line) {
		
		if(line.startsWith("[[") && (!line.startsWith("[[File") && !line.startsWith("[[Image:") && !line.startsWith("[[Media:"))) {
			return true;
		}
		return false;
	}
	
public float queryIndex(String query, String answer) throws ParseException, IOException {
		
		QueryParser parser = new QueryParser("document", this.analyzer);
		Query q = parser.parse(QueryParser.escape(query));
		int hitNum = 100;
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		
		//MultiFieldQueryParser queryParser = new MultiFieldQueryParser(new String[] {"categories", "document"},analyzer);
		//Query q = parser.parse(MultiFieldQueryParser.escape(query));
		//TopDocs docs = searcher.search(q, hitNum);
		searcher.setSimilarity(new BM25Similarity(0.6f, 0.60f));
		TopDocs docs = searcher.search(q, hitNum);
		ScoreDoc[] hits = docs.scoreDocs;
		float pos = measureMRR(hits, searcher, answer);
		
		return pos;
	}
	
	public void testIndex() throws ParseException, IOException {
		
		File questions = new File("/Users/nateclos/Documents/cs483/WatsonQASystem/src/main/resources/questions.txt");
		Scanner s = new Scanner(questions);
		float total = 0; 
		int overall = 0;
		while(s.hasNextLine()) {
			
			String category = s.nextLine();
			String clue = s.nextLine();
			String answer = s.nextLine();
			s.nextLine();
			System.out.println("Attempting query: " + clue);
			// The following lines are what would allow a phrase query to be added to the query 
			// in the event there are parentheses in the question.
			/*
			if(clue.contains("\"")) {
				String q = "";
				String[] clueArr = clue.split("\"");
				q += '"' + clueArr[1].trim() + '"' + "~1";
				clue += " " + q;
			}
			*/
			float result = queryIndex(category + " " + clue, answer);
			total += result;
		}
		System.out.println("MRR: " + total);
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		
		File currDir = new File("src/main/resources");
		Indexer i = new Indexer(currDir);
		i.testIndex();
		i.index.close();
		edu.stanford.nlp.simple.Document d = new edu.stanford.nlp.simple.Document("test");
	}
}
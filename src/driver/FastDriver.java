package driver;

    import java.io.File;
	import java.io.FileInputStream;
	import java.io.IOException;
	import java.io.InputStream;
	import java.io.InputStreamReader;
	import java.io.Reader;
	import java.util.ArrayList;
	import java.util.HashMap;
	import java.util.LinkedList;
	import java.util.Scanner;
    import java.util.TreeMap;
    import java.util.TreeSet;

	import javax.xml.parsers.ParserConfigurationException;
	import javax.xml.parsers.SAXParser;
	import javax.xml.parsers.SAXParserFactory;

	import org.xml.sax.InputSource;
	import org.xml.sax.SAXException;

	import documents.AbstractDocument;
import filters.CaseFoldingFilter;
	import filters.IFilter;
import filters.NumberFilter;
import filters.PorterStemmerFilter;
import filters.PunctuationFilter;
import filters.ReutersFilter;
import filters.StopWordsFilter;

	import parsers.SAXHandlerReuters;
	import spimi.SPIMInvert;
	import technical.DateUtils;
	import technical.Merge;
	import tokenizer.DocumentTokenizer;
import tokenizer.ReutArticleTokenizer;
	

	public class FastDriver {

		public static void main (String[] args) throws ParserConfigurationException {
		    try {
		    	    	
				// Get a list of files that need to be parsed
		    	String documentCollection = "/media/320/Users/Ben/School/Concordia University/Classes/COMP 479 (Information Retrieval)/code/reuters/copies";
		    	File root = new File(documentCollection);
		    	File[] all_files = root.listFiles();
		    	
		    	// Create a new SAX Parser for which we have created a custom handler
		    	// The handler contains a list of AbstractDoucments which have been parsed
		    	// out of the file specified in the parse function
		    	SAXParserFactory factory = SAXParserFactory.newInstance();
		    	SAXParser saxParser = factory.newSAXParser();
		    	SAXHandlerReuters handler = new SAXHandlerReuters();
		    	
		    	// Now that we have a list of files and a parser we can parse them
		    	// and place each article into it's own object which we can then tokenize
		    	
		    	System.out.println("Begin gathering and parsing all files \t\t"+DateUtils.now());
		    	int file_ctr = 0;
		    	
		    	for (File f : all_files) {
		    		// make sure we only parse XML files
		    		if (f.getAbsolutePath().endsWith("xml")) {
		    			//System.out.println("Processing file: "+f.getName());
		    			InputStream inputStream = new FileInputStream(f);
		    			Reader reader = new InputStreamReader(inputStream,"UTF-8");
		    			InputSource is = new InputSource(reader);
		    			is.setEncoding("UTF-8");
		    			saxParser.parse(is, handler);
		    			file_ctr++;
		    		}
		    	}
		    	System.out.println(file_ctr+" files successfully gathered\t\t\t"+DateUtils.now());
		    	System.out.println(handler.getDocuments().size()+" articles have been parsed\t\t\t"+DateUtils.now());
		    	
		    	// Now that the handler is full of articles that need parsing we
		    	// get to work on that and we add the items to our SPIMI index from within the tokenizer for speed
		    	
		    	SPIMInvert spimi = new SPIMInvert();
		    	
		    	// Create a tokenizer and pass the SPIMI object
		    	DocumentTokenizer tokenizer = new ReutArticleTokenizer(spimi);
		    	
		    	// Add some stats in
		    	tokenizer.getStats().setFiles(file_ctr);
		    	tokenizer.getStats().setDocuments(handler.getDocuments().size());
		    	
		    	System.out.println("Begin Indexing Process\t\t\t\t"+DateUtils.now());
		    	
		    	// Loop through every document, tokenize it and add it to the index
		    	for (AbstractDocument d : handler.getDocuments()) {
		    		tokenizer.setDocument(d);
		    		tokenizer.parse(); 				// this adds to the index too
		    	}
		    	
		    	spimi.flushBlock(); // just in case we didn't reach the buffer limit

		    	System.out.println("All files have been tokenized and Indexed\t"+DateUtils.now());
		    	System.out.println("Begin merging the indexes\t\t\t"+DateUtils.now());
		    	
		    	File input_files = new File(documentCollection+"/index_files");
		    	TreeMap<String, TreeSet<Long>> complete_index = spimi.mergeBlocks(input_files.list(), documentCollection+"/spimi_index/s_index.txt");
		    	tokenizer.getStats().setDistinctTerms(complete_index.size());
		    	
		    	
		    	System.out.println("Index Merged\t\t\t\t\t"+DateUtils.now());
		    	System.out.println("################ Statistics ################");
		    	System.out.println("Number of Files\t\t\t"+tokenizer.getStats().getFiles());
		    	System.out.println("Number of Documents\t\t"+tokenizer.getStats().getDocuments());
		    	System.out.println("Number of Terms\t\t\t"+tokenizer.getStats().getTerms());
		    	System.out.println("Number of NonPosPostings\t"+tokenizer.getStats().getNonPosPostings());
		    	System.out.println("Number of Distinct Terms\t"+tokenizer.getStats().getDistinctTerms());
		    	
		     	LinkedList<IFilter> filters = new LinkedList<IFilter>();
		    	filters.add(new ReutersFilter());
				filters.add(new PunctuationFilter());
				filters.add(new CaseFoldingFilter("down"));
				filters.add(new NumberFilter());
				filters.add(new StopWordsFilter(30));
				filters.add(new StopWordsFilter(174));
				filters.add(new PorterStemmerFilter());
		    	
		    	
		    	
		    	while(true) {
			    	
			    	System.out.println("\nReady to accept a query");
			    	
			    	Scanner keyboard = new Scanner (System.in);
			    	String query = keyboard.nextLine();
			    	String[] q_terms = query.split("\\s");
			    	
			    	// Run the query terms through all of the same filters we used to build the index
			    	for(int i = 0; i < q_terms.length; i++) {
			    		for (IFilter f : filters) {
			    			q_terms[i] = f.process(q_terms[i]);
			    		}
			    	}
			    	
			    	System.out.println("List of matching documents");
			    	
			    	// Perform some retrieval now
			    	if (q_terms.length == 1) {
			    		for(Long i : complete_index.get(q_terms[0])) {
			    			System.out.print(" "+i);
			    		}
			    	}
			    	else {
				    	// Create a Map to store the postings lists of the query terms
				    	HashMap<String, TreeSet<Long>> matching_docs = new HashMap<String, TreeSet<Long>>();
				    	
				    	// iterate through each query term and add it's postings list to the matching docs
				    	for(String s : q_terms) {
				    		// Make sure the query term is not empty
				    		if (!s.equals("")) {
				    			matching_docs.put(s, complete_index.get(s));
				    		}
				    	}
				    	
				    	// Time to take the union of the results
				    	TreeSet<Long> union_set = new TreeSet<Long>();
				    	
				    	// get a handle to postings lists that have been matched
				    	ArrayList<String> keys = new ArrayList<String>();
				    	for(String s : matching_docs.keySet()) {
				    		keys.add(s);
				    	}
				    	
				    	// iterate through the postings lists merging them
				    	for(int i = 0; i < keys.size() -1; i++) {
				    		if (i == 0) {
				    			union_set = Merge.PostingsList(matching_docs.get(keys.get(i)), matching_docs.get(keys.get(i+1))); 
				    		}
				    		else {
				    			union_set = Merge.PostingsList(union_set, matching_docs.get(keys.get(i+1)));
				    		}
				    	}
				    	
				    	// print the results to screen
				    	for (Long i : union_set) {
				    		// get the postings list
				    		System.out.print(" "+i);
				    	}
			    	}
		    	}
		    }
		    
		    catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

}

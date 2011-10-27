package driver;

import index.TermDictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import technical.Logger;
import filters.Filter;
import filters.PunctuationFilter;
import filters.ReutersFilter;

public class TestParsing {

	protected File f_handle = null;
	protected TermDictionary dictionary = null;
	protected ArrayList<String> tokens = null;
	protected ArrayList<Filter> filters = null;
	
	public TestParsing(String input) {
		f_handle = new File(input);
		dictionary = new TermDictionary();
		tokens = new ArrayList<String>();
		filters = new ArrayList<Filter>();
		filters.add(new PunctuationFilter());
		filters.add(new ReutersFilter());
	}
	
	  public void parse() {	
			// Get the size of the file, create a byte stream and a character array to store the input
			int file_size = (int) f_handle.length(); // note f_handle is from the super class

			try {
			  Reader input = new BufferedReader(new InputStreamReader(new FileInputStream(f_handle)));
			  char[] data = new char[file_size];

			// Create an array list to store the tokens
		    //	ArrayList<String> tokens = new ArrayList<String>();

			// Read the file into memory at once since they are not usually larger than 2MB

				while (input.read(data, 0, file_size) != -1) {

				  // Keep track of the bounds of tokens and a counter 
				  int left = 0, right = 0;//, i = 0;
				  
				  // Loop through the characters omitting any information that occurs between < >
				  //while (i < data.length) {
				  for(int i = 0; i < data.length; i++) {
					while(true) {
						if (data[i] == '<') {
					      while (i < data.length && data[i] != '>') {
					    	  i++;
						  }
					      // make certain that we haven't over-stepped our boundaries
					      if (i >= data.length) {
					    	  break;
					      }
						}
						
						if (data[i] == '>') {
							i++; // if we've gotten here we increment since the current character is >
						}
						
						if (i < data.length && data[i] != '<') {
						  // if the next character is an opening bracket we keep looping up here
							break;
						}
					}

				      // make certain that we haven't over-stepped our boundaries once again
				      if (i >= data.length) {
				    	  break;
				      }
					// Finally we are no longer inside a tag
					boolean build_token = false;

					// If we are not on whitespace
					if (!Character.isWhitespace(data[i])) {
						left = i; // take note of the LHS boundary of the term

						// Find the RHS of the term
						// if this loop is never entered we found another tag and go back to the top
						// if this loop is entered and we find whitespace or another <, we form a term
						while(i < data.length && !Character.isWhitespace(data[i]) && data[i] != '<') {
						  i++;
						  build_token = true;
					    }
						while(build_token) {
						  right = i;
						  StringBuilder term = new StringBuilder(right-left);
					      term.append(data, left, right-left);
					      String string_term = term.toString();

					      // Do some post-processing on the string to clean it up
					      for (Filter f : this.filters) {
					    	  string_term = f.process(string_term);
					      }

				          // If we're not left with an empty string we add it to the output
					      if (!string_term.isEmpty()) {
					    	String case_fold = string_term.toLowerCase();
					    	this.tokens.add(case_fold); 
					        this.dictionary.add(case_fold);
					        Logger.getUniqueInstance().writeToLog(case_fold); //*** remove me
					      }

					      i--; // decrement the counter to make sure we don't skip ahead when for loop increments next.
					      build_token = false; // break the loop 
						}
					}
				  }
				}
				input.close();	// close the file handle
				tokens.trimToSize(); // small optimization here
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//return tokens;
		  }
	
	  public static void main (String[] args) {
		  File f = new File("/home/ben/school/COMP 479 (Information Retrieval)/code/reuters/testFile.txt");
		  //File[] fs = f.listFiles();
		  //for (File g : fs) {
			  TestParsing z = new TestParsing(f.getAbsolutePath());
			  System.out.println(f.getAbsolutePath());
			  z.parse();
		  //}
		  
		  
	  }
}
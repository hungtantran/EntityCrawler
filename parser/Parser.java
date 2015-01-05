package parser;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.jsoup.nodes.Document;

public class Parser {
	protected String fullURL = null;
	protected String URL = null;
	protected Document doc = null;
	protected boolean isContentPage = false;
	
	protected ArrayList<String> authors = null;
	protected ArrayList<String> topics = null;
	protected ArrayList<String> title = null;
	protected ArrayList<String> body = null;
	protected ArrayList<String> urls = null;
	protected ArrayList<String> urlDocs = null;
	protected ArrayList<String> citedByArticle = null;
	protected ArrayList<String> relatedCitedArticle = null;

	protected final String invalidCharacters[] = { "{", "}", "(", ")", "[", "]", ".",
			";", ",", "/", "\\" };
	protected final String invalidWords[] = { "paper", "article", "this", "that",
			"with"};
	
	protected static String excuteGet(String executedUrl) {
		HttpURLConnection connection = null;
		
		try {
			// Create connection
			URL url = new URL(executedUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection
					.setRequestProperty("Accept",
							"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			connection.setRequestProperty("Accept-Encoding",
					"gzip,deflate,sdch");
			connection.setRequestProperty("Accept-Language",
					"en-US,en;q=0.8,de;q=0.6,vi;q=0.4");
			connection.setRequestProperty("Cache-Control", "max-age=0");
			connection.setRequestProperty("Connection", "keep-alive");
			connection
					.setRequestProperty(
							"User-Agent",
							"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36");
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");

			// connection.setUseCaches (false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.flush();
			wr.close();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			// System.out.println(response.toString());
			return response.toString();

		} catch (Exception e) {

			e.printStackTrace();
			return null;

		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	protected String[] getBody() {
		if (this.body == null)
			return null;

		String[] result = new String[this.body.size()];

		for (int i = 0; i < this.body.size(); i++) {
			result[i] = this.body.get(i);
		}

		return result;
	}

	protected String[] getTitle() {
		if (this.title == null)
			return null;

		String[] result = new String[this.title.size()];

		for (int i = 0; i < this.title.size(); i++) {
			result[i] = this.title.get(i);
		}

		return result;
	}

	protected String[] getAuthors() {
		if (this.authors == null)
			return null;
		
		Set<String> authorSet = new HashSet<String>();
		authorSet.addAll(this.authors);
		
		String[] result = new String[authorSet.size()];
		
		int index = 0;
		for (String author : authorSet) {
			result[index] = author;
			index++;
		}

		return result;
	}

	protected String[] getTopics() {
		if (this.topics == null)
			return null;
		
		Set<String> topicSet = new HashSet<String>();
		topicSet.addAll(this.topics);
		
		String[] result = new String[topicSet.size()];
		
		int index = 0;
		for (String topic : topicSet) {
			result[index] = topic;
			index++;
		}

		return result;
	}

	protected String[] getUrl() {
		if (this.urls == null)
			return null;

		String[] result = new String[this.urls.size()];

		for (int i = 0; i < this.urls.size(); i++) {
			result[i] = this.urls.get(i);
		}

		return result;
	}

	protected String[] getUrlCitedByArticles() {
		if (this.citedByArticle == null)
			return null;

		String[] result = new String[this.citedByArticle.size()];

		for (int i = 0; i < this.citedByArticle.size(); i++) {
			result[i] = this.citedByArticle.get(i);
		}

		return result;
	}

	protected String[] getUrlRelatedCitedArticles() {
		if (this.relatedCitedArticle == null)
			return null;

		String[] result = new String[this.relatedCitedArticle.size()];

		for (int i = 0; i < this.relatedCitedArticle.size(); i++) {
			result[i] = this.relatedCitedArticle.get(i);
		}

		return result;
	}

	protected String[] getUrlDoc() {
		if (this.urlDocs == null)
			return null;

		String[] result = new String[this.urlDocs.size()];

		for (int i = 0; i < this.urlDocs.size(); i++) {
			result[i] = this.urlDocs.get(i);
		}

		return result;
	}
	
	protected static boolean dedupNoun(String noun1, String noun2) {
		if (Math.abs(noun1.length() - noun2.length()) < 3) {
			String firstForm = noun1;
			String secondForm = noun2;
			if (noun1.length() > noun2.length()) {
				firstForm = noun2;
				secondForm = noun1;
			}
			
			int compareLength = firstForm.length()-2;
			
			if (firstForm.length() < 5)
				compareLength += 1;
			
			for (int i = 0; i < compareLength; i++) {
				if (firstForm.charAt(i) != secondForm.charAt(i)) return false;
			}
			
			return true;
		}
			
		return false;
	}
	
	public static int numOccurance(String body, String term) {
		if (body == null || term == null || body.length() < 1 || term.length() < 1)
			return -1;
		
		int numOccurance = 0;
		String copyBody = body.toLowerCase();
		String copyTerm = term.toLowerCase();
		
		while (true) {
			int index = copyBody.indexOf(copyTerm);
			
			if (index < 0) {
				break;
			}
			
			numOccurance++;
			copyBody = copyBody.substring(index+copyTerm.length());
		}
		
		return numOccurance;
	}
	
	protected class ValueComparator implements Comparator<String> {

	    Map<String, Integer> base;
	    public ValueComparator(Map<String, Integer> base) {
	        this.base = base;
	    }

	    // Note: this comparator imposes orderings that are inconsistent with equals.    
	    public int compare(String a, String b) {
	        if (base.get(a) >= base.get(b)) {
	            return 1;
	        } else {
	            return -1;
	        } // returning 0 would merge keys
	    }
	}
}

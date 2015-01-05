package parser;

import java.io.*;
import java.util.*;

import org.jsoup.*;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class JSTORParser extends Parser {
	private String removeParameter(String fullUrl) {
		if (fullUrl.indexOf("stable/info") != -1) {
			if (fullUrl.indexOf("?") != -1) 
				return fullUrl.substring(0, fullUrl.indexOf("?"));
		}
		
		return fullUrl;
	}
	
	// Parse the document title
	private void parseTitle() {
		Elements titleElems = doc.select("div[class~=hd]");
		if (titleElems.size() > 0) {
			for (int i = 0; i < titleElems.size(); i++) {
				if (titleElems.get(i).attr("class").toString().equals("hd title langMatch")) {
					new String(titleElems.get(i).text());
				}
			}
		}
	}
	
	// Parse the document body
	private void parseBody() {
		doc.outerHtml();
	}
	
	// Parse the document author name
	private void parseAuthors() {
		Elements authorElems = doc.select("div[class~=author]");
		if (authorElems.size() > 0) {
			String authorText = authorElems.get(0).text();
			
			String[] authorParts = authorText.split(" and ");
			
			for (int i = 0; i < authorParts.length; i++) {
				String[] author = authorParts[i].split(", ");
				
				for (int j = 0; j < author.length; j++) {
					this.authors.add(author[j]);
				}
			}
		} else {
			System.out.println("Empty Author");
		}
	}

	public JSTORParser() {
	}
	
	public JSTORParser(String URL) {
		this.setURL(URL);
		this.authors = new ArrayList<String>();
		this.topics = new ArrayList<String>();
	}

	public String getURL() {
		return URL;
	}
	
	public void setDocument(Document doc) {
		this.doc = new Document(doc.outerHtml());
	}

	public void setURL(String URL) {
		this.fullURL = URL;
		this.URL = new String(this.removeParameter(this.fullURL));
		System.out.println("Truncate ULR = "+this.URL);
		
		try {
			Response response = Jsoup.connect(URL)
					.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					.timeout(10000) 
					.followRedirects(true)
					.execute();
			this.doc = response.parse();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isContentPage() {
		return this.isContentPage;
	}
	
	public void parseDoc() {
		if (doc == null)
			return;
		
		int pdfLinkSize = doc.select("a[class=pdflink]").size();
		this.isContentPage = (pdfLinkSize > 0);
		
		// If not content page, don't do anything
		if (!this.isContentPage)
			return;
		
		// Parse the document title
		this.parseTitle();
		
		// Parse the body of the document
		this.parseBody();
		
		// Parse the author name
		this.parseAuthors();
	}
	
	public static void main(String[] args) {
		/*Parser parser = new Parser("http://www.jstor.org/stable/info/20447896?&Search=yes&searchText=artworld&searchUri=%2Faction%2FdoBasicSearch%3FQuery%3Dartworld%26amp%3Bprq%3Dart%2Bforum%26amp%3Bhp%3D25%26amp%3Bacc%3Don%26amp%3Bwc%3Don%26amp%3Bfc%3Doff%26amp%3Bso%3Drel%26amp%3Bracc%3Doff");
		parser.parseDoc();
		
		if (parser.isContentPage()) {
			System.out.println("Title = " + parser.getTitle());
			System.out.println("Authors = " + parser.getAuthors());
			System.out.println("Topic = " + parser.getTopics());
			System.out.println("Body = " + parser.getBody());
		} else {
			System.out.println("Link is not a JSTOR Document page");
		}*/
	}
}

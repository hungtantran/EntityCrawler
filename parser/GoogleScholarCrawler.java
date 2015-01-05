package parser;

import java.io.*;
import java.net.*;
import java.util.*;

import org.jsoup.*;
import org.jsoup.Connection.*;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

public class GoogleScholarCrawler extends Crawler {
	private String startURL = null;
	private final String domain = "http://scholar.google.com";
	private Set<String> urlsCrawled;
	private Set<String> urlsDocument;
	private Queue<String> urlsQueue;

	public GoogleScholarCrawler(String startURL) {
		this.startURL = new String(startURL);
		// this.analyzer = new DocAnalyzer();

		this.urlsQueue = new LinkedList<String>();
		this.urlsCrawled = new HashSet<String>();
		this.urlsDocument = new HashSet<String>();

		urlsQueue.add(this.startURL);
		urlsCrawled.add(this.startURL);
	}

	@Override
	public void startCrawl(boolean timeOut, long duration) {
		int count = 0;
		while (true) {
			if (this.urlsQueue.isEmpty())
				break;

			String curUrl = this.urlsQueue.remove();
			System.out.println("Process url " + curUrl);
			this.processUrl(curUrl);

			try {
				int waitTime = 10000 + (int) (Math.random() * ((15000 - 10000) + 1));
				System.out.println("Wait for " + waitTime);
				Thread.currentThread();
				Thread.sleep(waitTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			count++;
			if (count >= 20)
				break;
		}
	}

	private void processUrl(String curUrl) {
		Document doc = null;

		try {
			Response response = Jsoup
					.connect(curUrl)
					.userAgent(
							"Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					.timeout(10000).followRedirects(true).execute();
			doc = response.parse();

		} catch (IOException e) {
			e.printStackTrace();
		}

		if (doc != null) {
			this.addLinksFromDocument(doc);
			this.checkDocumentUrl(curUrl, doc);
		}
	}

	private void checkDocumentUrl(String url, Document doc) {
		GoogleScholarParser parser = new GoogleScholarParser(url);
		parser.parseDoc();

		if (parser.isContentPage()) {
			String[] titles = parser.getTitle();
			String[] authors = parser.getAuthors();
			String[] topics = parser.getTopics();
			String[] bodies = parser.getBody();
			String[] urls = parser.getUrl();
			String[] urlDocs = parser.getUrlDoc();

			if (titles.length != authors.length
					|| authors.length != topics.length
					|| topics.length != bodies.length
					|| bodies.length != urls.length
					|| urls.length != urlDocs.length)
				return;

			for (int i = 0; i < titles.length; i++) {
				this.urlsDocument.add(urls[i]);
				System.out.println("Url = " + urls[i]);
				System.out.println("Title = " + titles[i]);
				System.out.println("Authors = " + authors[i]);
				System.out.println("Topic = " + topics[i]);
				System.out.println("Body Length = " + bodies[i].length());
				System.out.println("UrlDoc = " + urlDocs[i]);
				System.out.println();

				if (bodies[i].length() < 100)
					bodies[i] = titles[i];

				// Analyze the doc to get entity tags for document with
				// sufficient length
				/*
				 * if (bodies[i].length() > 1000) {
				 * this.analyzer.setDoc(bodies[i]); this.analyzer.analyzeDoc();
				 * HashSet<String>personTag = analyzer.getPersonTags();
				 * HashSet<String>locationTag = analyzer.getLocationTags();
				 * HashSet<String>organizationTag =
				 * analyzer.getOrganizationTags(); }
				 */

				// Create document node
				this.createDocumentNode(titles[i], urls[i], bodies[i]);

				String[] authorList = authors[i].split(";");
				String[] topicList = topics[i].split(";");

				for (int j = 0; j < authorList.length; j++) {
					authorList[j] = authorList[j].trim();
					if (authorList[j].length() < 1)
						continue;
					// Create author node
					this.createAuthorNode(authorList[j]);
					// Create author - url relationship
					this.createAuthorDocumentRelation(authorList[j], urls[i]);
				}

				for (int j = 0; j < topicList.length; j++) {
					topicList[j] = topicList[j].trim();
					if (topicList[j].length() < 1)
						continue;
					// Create entity node
					this.createEntityNode(topicList[j]);
					// Create entity - url relationship
					this.createEntityDocumentRelation(topicList[j], urls[i]);
				}
			}
		} else {
			System.out.println("Link is not a Google Scholar page");
		}
	}

	private void addLinksFromDocument(Document doc) {
		Elements hrefElems = doc.select("td[align=left]");
		String hrefStartString = "<a href=\"";

		if (hrefElems.size() > 0) {
			for (int i = 0; i < hrefElems.size(); i++) {
				String absHref = hrefElems.get(i).html();
				absHref = absHref.substring(absHref.indexOf(hrefStartString)
						+ hrefStartString.length(), absHref.indexOf("\">"));
				absHref = "http://scholar.google.com" + absHref;
				absHref = absHref.replaceAll("&amp;", "&");
				System.out.println("Add link " + absHref);

				// If the link is of the same domain and hasn't been crawled
				// before
				if (absHref.indexOf(this.domain) != -1
						&& !urlsCrawled.contains(absHref)) {
					this.urlsCrawled.add(absHref);
					this.urlsQueue.add(absHref);
				}
			}
		}
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: Parser {query string}");
			return;
		}

		String queryString = "";
		for (int i = 0; i < args.length; i++) {
			queryString += args[i].toLowerCase() + " ";
		}

		queryString = URLEncoder.encode(queryString.trim());
		// System.out.println("Query string = "+queryString);
		GoogleScholarCrawler crawler = new GoogleScholarCrawler(
				"http://scholar.google.com/scholar?q=" + queryString);
		crawler.startCrawl(false, 0);
	}
}

package parser;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import org.jsoup.*;
import org.jsoup.Connection.*;
import org.jsoup.nodes.*;

public class GenericCrawler extends Crawler {
	private String startURL = null;
	private String domain = null;
	private Set<String> urlsCrawled;
	private Queue<String> urlsQueue;
	private DocAnalyzer analyzer = null;
	private Set<String> termsCrawled;

	public GenericCrawler(String startURL) {
		System.out.println("Start url = "+startURL);
		URI urlObject;
		try {
			urlObject = new URI(startURL);
			this.domain = urlObject.getHost();
			this.domain = "http://" + this.domain;
			System.out.println("Domain = " + this.domain);
		} catch (URISyntaxException e) {
			System.out.println("Cannot process given link");
			return;
		}

		this.startURL = new String(startURL);
		this.analyzer = new DocAnalyzer();

		this.urlsQueue = new LinkedList<String>();
		this.urlsCrawled = new HashSet<String>();
		this.termsCrawled = new HashSet<String>();
		new HashSet<String>();

		urlsQueue.add(this.startURL);
		urlsCrawled.add(this.startURL);
	}

	@Override
	public void startCrawl(boolean timeOut, long duration) {
		if (this.startURL == null)
			return;

		while (true) {
			if (this.urlsQueue.isEmpty())
				break;

			String curUrl = this.urlsQueue.remove();
			System.out.println("Process url " + curUrl);
			this.processUrl(curUrl);

			waitSec(0, 1);
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
			this.checkDocumentUrl(curUrl, doc);
		}
	}

	private void checkDocumentUrl(String url, Document doc) {
		this.urlsCrawled.add(url);
		
		GenericParser parser = new GenericParser(url, this.analyzer);
		try {
			parser.parseDoc();
		} catch (URISyntaxException e) {
			System.out.println("Cannot process link");
			return;
		}
		
		if (parser.isContentPage()) {
			String[] titles = parser.getTitle();
			String[] topics = parser.getTopics();
			String[] bodies = parser.getBody();
			String[] urls = parser.getUrl();

			if (titles == null || titles.length < 1)
				titles = new String[] { "No Title" };
			if (bodies == null || bodies.length < 1 || bodies[0].length() < 100)
				bodies = new String[] { titles[0] };

			for (int i = 0; i < titles.length; i++)
				System.out.println("Title = " + titles[i]);

			for (int i = 0; i < topics.length; i++)
				System.out.println("Topic = " + topics[i]);

			for (int i = 0; i < bodies.length; i++)
				System.out.println("Body Length = " + bodies[i].length());

			for (int i = 0; i < urls.length; i++)
				System.out.println("Url = " + urls[i]);

			System.out.println();

			// Create document node
			this.createDocumentNode(titles[0], url, bodies[0]);
			
			if (topics != null) {
				int count = 0;
				for (int j = 0; j < topics.length; j++) {
					topics[j] = topics[j].trim();
					if (topics[j].length() < 1 || topics[j].length() > 50)
						continue;
					// Create entity node
					this.createEntityNode(topics[j]);
					// Create entity - url relationship
					this.createEntityDocumentRelation(topics[j], url);
					
					// Add topics into the term set
					this.termsCrawled.add(topics[j]);
					
					// Try to dedup term
					for (String existingTopic : this.termsCrawled) {
						if (topics[j].contains(existingTopic)
								&& !topics[j].equals(existingTopic)) {
							System.out.println("Dedup term " + topics[j]
									+ " into term " + existingTopic
									+ " for url " + urls[0] + "***\n\n");
							// Create entity node
							this.createEntityNode(existingTopic);
							// Create entity - url relationship
							this.createEntityDocumentRelation(existingTopic,
									urls[0]);
						}
					}
					
					count++;
					if (count >= 10) break;
				}
			}
			
			// Add more urls to the queue
			if (urls != null) {
				for (int j = 0; j < urls.length; j++) {
					urls[j] = urls[j].trim();
					if (urls[j].length() < 1)
						continue;
					
					if (!this.urlsCrawled.contains(urls[j]) && urls[j].contains(this.domain))
						this.urlsQueue.add(urls[j]);
				}
			}
		} else {
			System.out.println("Link is not a valid page");
		}
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: Parser {url}");
			return;
		}

		String startUrl = args[0].toLowerCase();
		GenericCrawler crawler = new GenericCrawler(startUrl);
		crawler.startCrawl(false, 0);
	}
}

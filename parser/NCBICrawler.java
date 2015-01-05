package parser;

import java.io.*;
import java.net.*;
import java.util.*;

import org.jsoup.*;
import org.jsoup.Connection.*;
import org.jsoup.nodes.*;

public class NCBICrawler extends Crawler {
	private String queryTerm = null;
	private Set<String> urlsCrawled;
	private Set<String> termsCrawled;
	private Stack<String> urlsQueue;
	private HashMap<String, Set<String>> citeByMap;

	public NCBICrawler(String queryTerm) {
		this.queryTerm = new String(queryTerm);
		this.urlsQueue = new Stack<String>();//LinkedList<String>();
		this.urlsCrawled = new HashSet<String>();
		this.termsCrawled = new HashSet<String>();
		this.citeByMap = new HashMap<String, Set<String>>();

		// Try to deserialize the saved term on disk into memory
		try (InputStream file = new FileInputStream("termsCrawled.ser");
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream(buffer);) {
			this.termsCrawled = (Set<String>) input.readObject();
			// display its data
			for (String term : this.termsCrawled) {
				System.out.println("Existing term : " + term);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void startCrawl(boolean timeOut, long duration) {
		// Get the first 20 search results
		int curPage = 1;
		NCBIParser parser = new NCBIParser();
		String[] docLinks = parser.parseDocLinksSearchPage(this.queryTerm,
				curPage);
		if (docLinks == null)
			return;

		for (int i = 0; i < docLinks.length; i++) {
			System.out.println("Add doc links " + docLinks[i]);
			this.urlsQueue.add(docLinks[i]);
		}

		while (true) {
			waitSec(3, 5);
			// If there is no urls left to crawl from the queue, go to the next
			// search page
			boolean finishedSearch = false;
			while (this.urlsQueue.isEmpty()) {
				curPage++;
				docLinks = parser.parseDocLinksSearchPage(this.queryTerm,
						curPage);

				// If there is no next page, break out and finish
				if (docLinks == null) {
					finishedSearch = true;
					break;
				}

				for (int i = 0; i < docLinks.length; i++) {
					System.out.println("Add doc links " + docLinks[i]);
					if (!this.urlsCrawled.contains(docLinks[i]))
						this.urlsQueue.add(docLinks[i]);
				}
			}

			if (finishedSearch)
				break;

			String curUrl = this.urlsQueue.pop();
			System.out.println("Process url " + curUrl);
			this.processUrl(curUrl);
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
			// this.addLinksFromDocument(doc);
			this.checkDocumentUrl(curUrl, doc);
		}
	}

	private void checkDocumentUrl(String url, Document doc) {
		NCBIParser parser = new NCBIParser(url);
		parser.parseDoc();

		if (parser.isContentPage()) {
			this.urlsCrawled.add(url);

			String[] titles = parser.getTitle();
			String[] authors = parser.getAuthors();
			String[] topics = parser.getTopics();
			String[] bodies = parser.getBody();
			String[] urls = parser.getUrl();
			String[] citedByArticleUrls = parser.getUrlCitedByArticles();
			// String[] relatedCitedArticleUrls =
			// parser.getUrlRelatedCitedArticles();

			if (titles == null || titles.length == 0 || authors == null
					|| authors.length == 0 || urls == null || urls.length == 0)
				return;

			System.out.println("Title = " + titles[0]);
			System.out.println("Authors = " + authors[0]);
			if (topics != null && topics.length > 0)
				System.out.println("Topic = " + topics[0]);
			if (bodies != null && bodies.length > 0)
				System.out.println("Abstract = " + bodies[0]);
			System.out.println("Url = " + urls[0]);
			System.out.println();

			if (bodies == null || bodies.length == 0) {
				bodies = new String[1];
				bodies[0] = "";
			}

			if (bodies[0].length() < 100)
				bodies[0] = titles[0];

			// Create document node
			this.createDocumentNode(titles[0], urls[0], bodies[0]);

			// Create author node and author-article relationship
			String[] authorList = authors[0].split(",");
			for (int j = 0; j < authorList.length; j++) {
				authorList[j] = authorList[j].trim();
				if (authorList[j].length() < 1 || authorList[j].length() > 40)
					continue;
				// Create author node
				this.createAuthorNode(authorList[j]);
				// Create author - url relationship
				this.createAuthorDocumentRelation(authorList[j], urls[0]);
			}

			// Try to create terms for the documents (This is most used for
			// article without keywords). This is entirely heuristic
			for (String existingTopic : this.termsCrawled) {
				if (existingTopic.length() < 4) continue;
				boolean relevantTopic = false;

				// If title contains the keyword, then the keyword is relevant
				if (titles[0].contains(existingTopic)) {
					relevantTopic = true;
				} else {
					int numOccuranceBody = Parser.numOccurance(bodies[0],
							existingTopic);
					// If the keyword is sufficiently long and appear
					// sufficiently many times in the body, it is relevant
					if (numOccuranceBody > 2)
						relevantTopic = true;
					else if (existingTopic.length() >= 5
							&& numOccuranceBody > 0)
						relevantTopic = true;
				}

				if (relevantTopic) {
					System.out.println("Find new term " + existingTopic
							+ " for url " + urls[0] + "***\n\n");
					// Create entity node
					this.createEntityNode(existingTopic);
					// Create entity - url relationship
					this.createEntityDocumentRelation(existingTopic, urls[0]);
				}
			}

			// Create topic node and topic-article relationship
			if (topics != null && topics.length > 0) {
				String[] topicList = topics[0].split(",");
				for (int j = 0; j < topicList.length; j++) {
					topicList[j] = topicList[j].trim().toLowerCase();
					if (topicList[j].length() < 4)
						continue;
					
					String curTopic = topicList[j];
					for (String existingTopic : this.termsCrawled) {
						if (!existingTopic.equals(curTopic) && Parser.dedupNoun(curTopic, existingTopic)) {
							System.out.println("Dedup noun from "+curTopic+" to "+existingTopic);
							curTopic = existingTopic;
							break;
						}
					}
					
					// Add topics into the term set
					this.termsCrawled.add(curTopic);
					// Create entity node
					this.createEntityNode(curTopic);
					// Create entity - url relationship
					this.createEntityDocumentRelation(curTopic, urls[0]);

					// Try to dedup term
					for (String existingTopic : this.termsCrawled) {
						if (curTopic.contains(existingTopic)
								&& !curTopic.equals(existingTopic)) {
							System.out.println("Dedup term " + curTopic
									+ " into term " + existingTopic
									+ " for url " + urls[0] + "***\n\n");
							// Create entity node
							this.createEntityNode(existingTopic);
							// Create entity - url relationship
							this.createEntityDocumentRelation(existingTopic,
									urls[0]);
						}
					}
				}
			}

			// Add in related citation article to url queues
			/*
			 * if (relatedCitedArticleUrls != null) for (int i = 0; i <
			 * relatedCitedArticleUrls.length; i++) if
			 * (!urlsCrawled.contains(relatedCitedArticleUrls[i]))
			 * this.urlsQueue.add(relatedCitedArticleUrls[i]);
			 */

			// Cited by relationship
			if (citedByArticleUrls != null) {
				for (int i = 0; i < citedByArticleUrls.length; i++) {
					System.out.println("Cited By = " + citedByArticleUrls[i]);

					// An article shouldn't cite itself (Medline has this case)
					if (citedByArticleUrls[i].equals(url))
						continue;

					// Put the cite by relationship into the map
					if (this.citeByMap.containsKey(citedByArticleUrls[i])) {
						Set<String> citedBy = this.citeByMap
								.get(citedByArticleUrls[i]);
						citedBy.add(url);
						this.citeByMap.put(citedByArticleUrls[i], citedBy);
					} else {
						Set<String> citedBy = new HashSet<String>();
						citedBy.add(url);
						this.citeByMap.put(citedByArticleUrls[i], citedBy);
					}

					// If the link is of the same domain and hasn't been crawled
					// before
					if (!this.urlsCrawled.contains(citedByArticleUrls[i])) {
						this.urlsQueue.push(citedByArticleUrls[i]);
					} else {
						// Create the relationship if the cited by node has
						// already been crawled
						this.createCitedByRelation(citedByArticleUrls[i], url);
					}
				}
			}
			
			// Create the cited node relationship
			if (this.citeByMap.containsKey(url)) {
				Set<String> citesList = this.citeByMap.get(url);
				for (String citeUrl : citesList) {
					if (this.urlsCrawled.contains(citeUrl)) {
						this.createCitedByRelation(url, citeUrl);
					}
				}
			}

			if (this.termsCrawled.size() > 0) {
				try (OutputStream file = new FileOutputStream(
						"termsCrawled.ser");
						OutputStream buffer = new BufferedOutputStream(file);
						ObjectOutput output = new ObjectOutputStream(buffer);) {
					output.writeObject(this.termsCrawled);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} else {
			System.out.println("Link is not a Medline page");
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
		NCBICrawler crawler = new NCBICrawler(queryString);
		crawler.startCrawl(false, 0);
	}
}

package parser;

import java.io.IOException;
import java.net.*;
import java.util.*;

import org.jsoup.*;
import org.jsoup.Connection.*;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

public class JSTORCrawler extends Crawler {
	private String startURL = null;
	private String domain = null;
	private Set<String> urlsCrawled;
	private Set<String> urlsDocument;
	private Queue<String> urlsQueue;

	public JSTORCrawler(String startURL) {
		this.startURL = new String(startURL);

		try {
			this.domain = this.getDomainName(this.startURL);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.urlsQueue = new LinkedList<String>();
		this.urlsCrawled = new HashSet<String>();
		this.urlsDocument = new HashSet<String>();

		urlsQueue.add(this.startURL);
		urlsCrawled.add(this.startURL);
	}

	private String getDomainName(String url) throws URISyntaxException {
		URI uri = new URI(url);
		String domain = uri.getHost();
		return domain.startsWith("www.") ? domain.substring(4) : domain;
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
				Thread.currentThread();
				Thread.sleep(20000);
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
		JSTORParser parser = new JSTORParser(url);
		parser.parseDoc();

		if (parser.isContentPage()) {
			this.urlsDocument.add(url);
			System.out.println("Title = " + parser.getTitle()[0]);
			System.out.println("Authors = " + parser.getAuthors()[0]);
			System.out.println("Topic = " + parser.getTopics()[0]);
		} else {
			System.out.println("Link is not a JSTOR Document page");
		}
	}

	private String truncateLink(String url) {
		int index = url.indexOf("#");

		if (index != -1) {
			return url.substring(0, index);
		}

		return url;
	}

	private void addLinksFromDocument(Document doc) {
		Elements hrefElems = doc.select("a[href]");

		if (hrefElems.size() > 0) {
			for (int i = 0; i < hrefElems.size(); i++) {
				String absHref = hrefElems.get(i).attr("abs:href");
				absHref = new String(this.truncateLink(absHref));

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
		JSTORCrawler crawler = new JSTORCrawler(
				"http://www.jstor.org/stable/info/3331810");
		crawler.startCrawl(false, 0);
	}
}

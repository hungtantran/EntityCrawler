package parser;

import java.io.*;
import java.net.*;
import java.util.*;

import org.jsoup.*;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import edu.stanford.nlp.ling.TaggedWord;

/*import edu.stanford.nlp.process.Tokenizer;
 import edu.stanford.nlp.process.TokenizerFactory;
 import edu.stanford.nlp.process.CoreLabelTokenFactory;
 import edu.stanford.nlp.process.DocumentPreprocessor;
 import edu.stanford.nlp.process.PTBTokenizer;
 import edu.stanford.nlp.ling.CoreLabel;
 import edu.stanford.nlp.ling.HasWord;
 import edu.stanford.nlp.ling.Sentence;
 import edu.stanford.nlp.trees.*;
 import edu.stanford.nlp.parser.lexparser.LexicalizedParser;*/

public class GenericParser extends Parser {
	private String domain = null;
	private String bodyText = null;
	private DocAnalyzer analyzer = null;

	private String removeParameter(String fullUrl) {
		return fullUrl;
	}

	// Parse the document title
	private void parseTitle() {
		Elements titleElems = doc.select("title");
		if (titleElems.size() > 0) {
			String titleText = new String(titleElems.get(0).text());
			titleText = titleText.trim();
			this.title.add(titleText);
			return;
		}
		
		titleElems = doc.select("h1");
		if (titleElems.size() > 0) {
			String titleText = new String(titleElems.get(0).text());
			titleText = titleText.trim();
			this.title.add(titleText);
			return;
		}
	}

	// Parse the document body/abstract
	private void parseBody() {
		Elements bodyElems = doc.select("body");
		if (bodyElems.size() > 0) {
			String bodyText = new String(bodyElems.get(0).html());
			this.bodyText = bodyElems.get(0).text();
			bodyText = bodyText.trim();
			this.body.add(bodyText);
		}
	}

	// Parse the document topic
	private void parseTopics(int lowerboundNameEntity,
			int lowerboundGenericEntity, String text) {

		Map<String, Integer> map = new HashMap<String, Integer>();
		ValueComparator bvc = new ValueComparator(map);
		Map<String, Integer> sortedMap = new TreeMap<String, Integer>(bvc);

		// People topic
		Set<String> peopleTopics = this.analyzer.getPersonTags();

		for (String person : peopleTopics) {
			person = person.trim();
			if (person.length() > 3 && person.length() <= 30) {
				// Sometimes, the abbreviated name appears more than the full
				// name so try to find the full name
				String name = person;
				for (String fullname : peopleTopics)
					if (!fullname.equals(name) && fullname.contains(name)
							&& fullname.length() <= 30)
						name = fullname;
				map.put(name, 0 - Parser.numOccurance(this.bodyText, person));
			}
		}

		// Only get the top 5 occurance
		int count = 0;
		// System.out.println("Person map = "+map);
		sortedMap.putAll(map);
		// System.out.println("Sorted Person map = "+sortedMap);
		for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
			if (entry.getValue() > lowerboundNameEntity)
				break;
			System.out.println("Add person topic = " + entry.getKey());
			this.topics.add(entry.getKey().toLowerCase());
			count++;
			if (count >= 3)
				break;
		}

		Set<String> locationTopics = this.analyzer.getLocationTags();
		map = new HashMap<String, Integer>();
		bvc = new ValueComparator(map);
		sortedMap = new TreeMap<String, Integer>(bvc);
		for (String location : locationTopics) {
			location = location.trim();
			if (location.length() > 3 && location.length() <= 30) {
				// Sometimes, the abbreviated name appears more than the full
				// name so try to find the full name
				String name = location;
				for (String fullname : peopleTopics)
					if (!fullname.equals(name) && fullname.contains(name)
							&& fullname.length() <= 30)
						name = fullname;
				map.put(name, 0 - Parser.numOccurance(this.bodyText, location));
			}
		}

		// Only get the top 5 occurance
		count = 0;
		// System.out.println("Location map = "+map);
		sortedMap.putAll(map);
		// System.out.println("Sorted Location map = "+sortedMap);
		for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
			if (entry.getValue() > lowerboundNameEntity)
				break;
			System.out.println("Add location topic = " + entry.getKey());
			this.topics.add(entry.getKey().toLowerCase());
			count++;
			if (count >= 3)
				break;
		}

		Set<String> organizationTopics = this.analyzer.getOrganizationTags();
		map = new HashMap<String, Integer>();
		bvc = new ValueComparator(map);
		sortedMap = new TreeMap<String, Integer>(bvc);
		for (String organization : organizationTopics) {
			organization = organization.trim();
			if (organization.length() > 3 && organization.length() <= 30) {
				// Sometimes, the abbreviated name appears more than the full
				// name so try to find the full name
				String name = organization;
				for (String fullname : peopleTopics)
					if (!fullname.equals(name) && fullname.contains(name)
							&& fullname.length() <= 30)
						name = fullname;
				map.put(name,
						0 - Parser.numOccurance(this.bodyText, organization));
			}
		}

		// Only get the top 5 occurance
		count = 0;
		// System.out.println("Organiztion map = "+map);
		sortedMap.putAll(map);
		// System.out.println("Sorted Organiztion map = "+sortedMap);
		for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
			if (entry.getValue() > lowerboundNameEntity)
				break;
			System.out.println("Add organization topic = " + entry.getKey());
			this.topics.add(entry.getKey().toLowerCase());
			count++;
			if (count >= 3)
				break;
		}

		Set<String> genericTopics = new HashSet<String>();
		String[] sentences = text.split("(?<=[.!?;,)({}:/])\\s* ");
		for (String sentence : sentences) {
			System.out.println("Sentence = " + sentence);
			List<TaggedWord> listWords = this.analyzer.parseSentence(sentence);
			String topic = new String("");
			int numWords = 0;
			for (TaggedWord taggedWord : listWords) {
				if (taggedWord.tag().contains("NN")) {
					topic += taggedWord.word() + " ";
					numWords++;
				} else {
					topic = topic.trim().toLowerCase();
					if (topic.length() > 5 && numWords >= 2
							&& topic.length() <= 30) {
						System.out.println("Found topic " + topic);
						genericTopics.add(topic);
					}
					numWords = 0;
					topic = new String("");
				}
			}
		}

		map = new HashMap<String, Integer>();
		bvc = new ValueComparator(map);
		sortedMap = new TreeMap<String, Integer>(bvc);
		for (String genericTopic : genericTopics) {
			genericTopic = genericTopic.trim();
			if (genericTopic.length() > 5 && genericTopic.length() <= 30) {
				int numOccurances = Parser.numOccurance(this.bodyText,
						genericTopic);
				map.put(genericTopic, 0 - numOccurances);
			}
		}

		// Only get the bottom 5 occurance
		count = 0;
		// System.out.println("Generic map = "+map);
		sortedMap.putAll(map);
		// System.out.println("Sorted generic map = "+sortedMap);
		for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
			if (entry.getValue() > lowerboundGenericEntity)
				break;
			System.out.println("Add in generic topic " + entry.getKey());
			this.topics.add(entry.getKey().toLowerCase());
		}
	}

	// Parse the document urls
	private void parseUrls() throws URISyntaxException {
		Elements urlElems = doc.select("a[href]");
		// System.out.println("Doc = "+doc.toString());
		for (int i = 0; i < urlElems.size(); i++) {
			String urlText = new String(urlElems.get(i).attr("href"));
			// System.out.println("Found = "+urlText);
			urlText = urlText.trim();

			URL urlObject = null;
			try {
				urlObject = new URL(this.URL);
				urlObject = new URL(urlObject, urlText);
			} catch (MalformedURLException e) {
				continue;
			}

			urlText = urlObject.toString();
			// Ignore url not from the same domain or has position anchor
			if (urlText.indexOf(this.domain) == -1
					|| urlText.indexOf("#") != -1)
				continue;

			if (this.urls.contains(urlText))
				continue;

			this.urls.add(urlText);
		}
	}

	public GenericParser() {
	}

	public GenericParser(String url, DocAnalyzer analyzer) {
		this.setURL(url);
		this.authors = new ArrayList<String>();
		this.topics = new ArrayList<String>();
		this.title = new ArrayList<String>();
		this.body = new ArrayList<String>();
		this.urls = new ArrayList<String>();
		this.analyzer = analyzer;
		this.isContentPage = false;
	}

	public String getURL() {
		return URL;
	}

	public void setDocument(Document doc) {
		this.doc = new Document(doc.outerHtml());
		new NCBIParser("");
	}

	public void setURL(String URL) {
		URI urlObject;
		try {
			urlObject = new URI(URL);
			this.domain = urlObject.getHost();
			this.domain = "http://" + this.domain;
			System.out.println("Domain = " + this.domain);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		this.fullURL = URL;
		this.URL = new String(this.removeParameter(this.fullURL));
		System.out.println("Truncate ULR = " + this.URL);

		try {
			Response response = Jsoup
					.connect(URL)
					.userAgent(
							"Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					.timeout(10000).followRedirects(true).execute();
			this.doc = response.parse();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isContentPage() {
		return this.isContentPage;
	}

	public void parseDoc() throws URISyntaxException {
		if (doc == null || this.domain == null)
			return;

		this.isContentPage = (this.URL.indexOf(this.domain) != -1);

		// If not content page, don't do anything
		if (!this.isContentPage)
			return;

		// Parse the body of the document
		this.parseBody();

		// Parse the document title
		this.parseTitle();
		
		// Parse topic from title of the document
		if (this.title != null && this.title.size() > 0) {
			System.out.println("Analyzing title "+this.title);
			
			String[] titleWords = this.title.get(0).split(" ");
			if (titleWords.length < 3) { 
				this.topics.add(this.title.get(0).toString());
			} else {	
				this.analyzer.setDoc(this.title.get(0));
				this.analyzer.analyzeDoc();
				this.parseTopics(0, 0, this.title.get(0));
			}
		}
		
		// Parse topic from body
		if (this.topics.size() < 5) {	
			if (this.bodyText != null && this.bodyText.length() > 0) {
				System.out.println("Analyzing body text");
				this.analyzer.setDoc(this.bodyText);
				this.analyzer.analyzeDoc();
				this.parseTopics(-2, -3, this.bodyText);
			}
		}
		
		// Try to find predefined keyword in meta tag html
		Elements keywordElems = doc.select("meta[name=keywords]");
		if (keywordElems.size() > 0) {
			String keywordText = new String(keywordElems.get(0).html());
			String[] keywords = keywordText.split(",");
			for (String keyword : keywords)
				this.topics.add(keyword.toLowerCase());
		}

		// Parse all urls in the document
		this.parseUrls();
	}

	public static void main(String[] args) throws URISyntaxException {
		/*
		 * String noun1 = "lily"; String noun2 = "lilies"; if
		 * (Parser.dedupNoun(noun1, noun2)) { System.out.println(noun1
		 * +" is dup of "+noun2); } else System.out.println(noun1
		 * +" is not dup of "+noun2);
		 */
		/*
		 * GenericParser parser = new GenericParser("http://www.nytimes.com/");
		 * parser.parseDoc();
		 * 
		 * if (parser.isContentPage()) { String[] titles = parser.getTitle();
		 * String[] topics = parser.getTopics(); String[] bodies =
		 * parser.getBody(); String[] urls = parser.getUrl();
		 * 
		 * for (int i = 0; i < titles.length; i++) System.out.println("Title = "
		 * + titles[i]);
		 * 
		 * for (int i = 0; i < topics.length; i++) System.out.println("Topic = "
		 * + topics[i]);
		 * 
		 * for (int i = 0; i < bodies.length; i++)
		 * System.out.println("Body Length = " + bodies[i].length());
		 * 
		 * for (int i = 0; i < urls.length; i++) System.out.println("Url = " +
		 * urls[i]);
		 * 
		 * System.out.println(); } else {
		 * System.out.println("Link is not a valid page"); }
		 */
	}
}

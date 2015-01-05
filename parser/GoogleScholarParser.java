package parser;

import java.io.*;
import java.util.*;
import java.net.*;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.util.*;
import org.jsoup.*;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

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

public class GoogleScholarParser extends Parser {
	private ArrayList<ArrayList<String>> authors;
	private ArrayList<ArrayList<String>> topics;

	private String removeParameter(String fullUrl) {
		return fullUrl;
	}

	// Parse the document title
	private void parseTitle() {
		Elements titleElems = doc.select("h3[class=gs_rt]");
		if (titleElems.size() > 0) {
			for (int i = 0; i < titleElems.size(); i++) {
				String titleText = new String(titleElems.get(i).text());

				int lastTitleTagIndex = titleText.lastIndexOf(']');

				if (lastTitleTagIndex > 0) {
					titleText = titleText.substring(lastTitleTagIndex + 1);
				}

				titleText = titleText.trim();
				// System.out.println("Title = "+titleText);
				this.title.add(titleText);
			}
		}
	}

	// Parse the document body
	private void parseBody() {
		for (int i = 0; i < this.urlDocs.size(); i++) {
			String pdfLinkText = this.urlDocs.get(i);
			String pdfText = "";

			if (pdfLinkText.length() < 1) {
				this.body.add(pdfText);
				continue;
			}

			try {
				URL pdfLink = new URL(pdfLinkText);
				PDDocument pd;
				pd = PDDocument.load(pdfLink);
				PDFTextStripper stripper = new PDFTextStripper();
				pdfText = stripper.getText(pd);
				pd.close();
			} catch (Exception e) {
				System.out.println("Link " + pdfLinkText + " does not work");
				// e.printStackTrace();
			}

			this.body.add(pdfText);
		}
	}

	// Parse the document author name
	private void parseAuthors() {
		Elements authorElems = doc.select("div[class=gs_a]");
		if (authorElems.size() > 0) {
			for (int i = 0; i < authorElems.size(); i++) {
				String authorText = new String(authorElems.get(i).text());
				authorText = authorText.split("-")[0];
				ArrayList<String> authors = new ArrayList<String>();
				String[] authorList = authorText.split(",");

				for (int j = 0; j < authorList.length; j++) {
					authors.add(authorList[j]);
					// System.out.println("Author = "+authorList[j]);
				}

				this.authors.add(authors);
			}
		}
	}

	// Check whether the word can or cannot be a word in a possible entity
	private boolean validCharacter(String word) {
		// TODO better sanitization
		if (word == null || word.length() <= 3)
			return false;

		for (int i = 0; i < this.invalidCharacters.length; i++) {
			if (word.contains(this.invalidCharacters[i]))
				return false;
		}

		for (int i = 0; i < this.invalidWords.length; i++) {
			if (word.contains(this.invalidWords[i]))
				return false;
		}

		return true;
	}

	// Parse the document topic
	private void parseTopics() {
		Elements paperElems = doc.select("div[class=gs_ri]");
		String allAbstractText = "";

		for (int k = 0; k < paperElems.size(); k++) {
			Elements topicElems = paperElems.get(k).select("div[class=gs_rs]");

			if (topicElems.size() > 0) {
				allAbstractText += topicElems.get(0).html();
			}
		}

		Document doc = Jsoup.parse(allAbstractText);
		allAbstractText = doc.text();
		System.out.println("All abstract = " + allAbstractText);

		// Perform some sanitization over the abstract text
		// TODO better sanitization using Stanford NLP Parser
		Map<String, Integer> nGrams = new HashMap<String, Integer>();
		allAbstractText = allAbstractText.toLowerCase();

		// Map between 2 gram and its number of appearances
		String[] wordList = allAbstractText.split(" ");

		for (int i = 0; i < wordList.length - 1; i++) {
			if (this.validCharacter(wordList[i])
					&& this.validCharacter(wordList[i + 1])) {
				String twoGram = wordList[i] + " " + wordList[i + 1];
				if (nGrams.containsKey(twoGram)) {
					int numAppearance = nGrams.get(twoGram);
					nGrams.put(twoGram, numAppearance + 1);
				} else {
					nGrams.put(twoGram, 1);
				}
			}
		}

		// Try to combine some 2-gram into 3 gram
		Set<String> removeTwoGram = new HashSet<String>();
		Map<String, Integer> threeGrams = new HashMap<String, Integer>();

		for (String twoGram1 : nGrams.keySet()) {
			for (String twoGram2 : nGrams.keySet()) {
				int numAppearance1 = nGrams.get(twoGram1);
				int numAppearance2 = nGrams.get(twoGram2);

				if (numAppearance1 > 1 && numAppearance1 == numAppearance2) {
					String[] wordList1 = twoGram1.split(" ");
					String[] wordList2 = twoGram2.split(" ");

					if (wordList1[1].equals(wordList2[0])) {
						String threeGram = wordList1[0] + " " + wordList1[1]
								+ " " + wordList2[1];
						removeTwoGram.add(twoGram1);
						removeTwoGram.add(twoGram2);
						threeGrams.put(threeGram, numAppearance1);
					} else if (wordList1[0].equals(wordList2[1])) {
						String threeGram = wordList2[0] + " " + wordList2[1]
								+ " " + wordList1[1];
						removeTwoGram.add(twoGram1);
						removeTwoGram.add(twoGram2);
						threeGrams.put(threeGram, numAppearance1);
					}
				}
			}
		}

		// Remove the 2-gram used to combine into 3-gram
		for (String twoGram : removeTwoGram) {
			nGrams.remove(twoGram);
		}

		// Add the 3-gram into n-gram
		for (String threeGram : threeGrams.keySet()) {
			nGrams.put(threeGram, threeGrams.get(threeGram));
		}

		for (String twoGram : nGrams.keySet()) {
			if (nGrams.get(twoGram) > 1) {
				System.out.println("Entities = " + twoGram + " - "
						+ nGrams.get(twoGram));
			}
		}

		for (int k = 0; k < paperElems.size(); k++) {
			Elements topicElems = paperElems.get(k).select("div[class=gs_rs]");

			if (topicElems.size() > 0) {
				ArrayList<String> curPaperTopicList = new ArrayList<String>();
				String topicText = topicElems.get(0).html();
				doc = Jsoup.parse(topicText);
				topicText = doc.text();
				topicText = topicText.toLowerCase();
				topicText = topicText.replace(".", "");
				topicText = topicText.replace(",", "");
				topicText = topicText.replace(";", "");
				wordList = topicText.split(" ");
				// System.out.println(topicText);
				for (int j = 0; j < wordList.length - 1; j++) {
					String twoGram = wordList[j] + " " + wordList[j + 1];
					if (nGrams.containsKey(twoGram) && nGrams.get(twoGram) > 1) {
						curPaperTopicList.add(twoGram);
					}
				}
				// System.out.println();
				this.topics.add(curPaperTopicList);
			} else {
				ArrayList<String> curPaperTopicList = new ArrayList<String>();
				this.topics.add(curPaperTopicList);
			}
		}
	}

	// Parse the document url
	private void parseUrl() {
		Elements urlElems = doc.select("h3[class=gs_rt]");
		if (urlElems.size() > 0) {
			for (int i = 0; i < urlElems.size(); i++) {
				String urlText = urlElems.get(i).html();
				String hrefString = "<a href=\"";
				urlText = urlText.substring(urlText.indexOf(hrefString)
						+ hrefString.length());
				urlText = urlText.substring(0, urlText.indexOf("\""));

				this.urls.add(urlText);
			}
		}
	}

	// Parse the document url
	private void parseUrlDocs() {
		Elements articleElems = doc.select("div[class=gs_r]");
		if (articleElems.size() > 0) {
			for (int i = 0; i < articleElems.size(); i++) {
				Elements urlElems = articleElems.get(i).select(
						"div[class=gs_ggs gs_fl]");

				String urlText = "";

				if (urlElems.size() == 1) {
					urlText = urlElems.get(0).html();
					String hrefString = "<a href=\"";
					urlText = urlText.substring(urlText.indexOf(hrefString)
							+ hrefString.length());
					urlText = urlText.substring(0, urlText.indexOf("\""));
				}

				this.urlDocs.add(urlText.trim());
			}
		}
	}

	public GoogleScholarParser() {
	}

	public GoogleScholarParser(String URL) {
		this.setURL(URL);
		this.authors = new ArrayList<ArrayList<String>>();
		this.topics = new ArrayList<ArrayList<String>>();
		this.title = new ArrayList<String>();
		this.body = new ArrayList<String>();
		this.urls = new ArrayList<String>();
		this.urlDocs = new ArrayList<String>();
	}

	public String getURL() {
		return URL;
	}

	public void setDocument(Document doc) {
		this.doc = new Document(doc.outerHtml());
		this.authors = new ArrayList<ArrayList<String>>();
		this.topics = new ArrayList<ArrayList<String>>();
		this.title = new ArrayList<String>();
		this.body = new ArrayList<String>();
		this.urls = new ArrayList<String>();
		this.urlDocs = new ArrayList<String>();
	}

	public void setURL(String URL) {
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

	public void parseDoc() {
		if (doc == null)
			return;

		this.isContentPage = (this.URL
				.indexOf("http://scholar.google.com/scholar") != -1);

		// If not content page, don't do anything
		if (!this.isContentPage)
			return;

		// Parse the document title
		this.parseTitle();

		// Parse the author name
		this.parseAuthors();

		// Parse the topics
		this.parseTopics();

		// Parse the urls
		this.parseUrl();

		// Parse the urls to the PDF file
		this.parseUrlDocs();

		// Parse the body of the document
		this.parseBody();
	}

	@Override
	public String[] getAuthors() {
		if (this.authors == null)
			return null;

		String[] result = new String[this.authors.size()];

		for (int i = 0; i < this.authors.size(); i++) {
			String author = "";
			ArrayList<String> curAuthor = this.authors.get(i);

			for (int j = 0; j < curAuthor.size(); j++) {
				author += curAuthor.get(j) + ";";
			}

			result[i] = author;
		}

		return result;
	}

	@Override
	public String[] getTopics() {
		if (this.topics == null)
			return null;

		String[] result = new String[this.topics.size()];

		for (int i = 0; i < this.topics.size(); i++) {
			String topic = "";
			ArrayList<String> curTopic = this.topics.get(i);

			for (int j = 0; j < curTopic.size(); j++) {
				topic += curTopic.get(j) + ";";
			}

			result[i] = topic;
		}

		return result;
	}

	public static void main(String[] args) {
		/*
		 * LexicalizedParser lp = LexicalizedParser.loadModel(
		 * "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		 * 
		 * String[] sent = { "This", "is", "an", "easy", "sentence", "." };
		 * List<CoreLabel> rawWords = Sentence.toCoreLabelList(sent); Tree parse
		 * = lp.apply(rawWords); parse.pennPrint(); System.out.println();
		 */

		/*
		 * GoogleScholarParser parser = new GoogleScholarParser(
		 * "http://scholar.google.com/scholar?start=30&q=distributed+system&hl=en&as_sdt=0,5"
		 * ); parser.parseDoc();
		 * 
		 * if (parser.isContentPage()) { String[] titles = parser.getTitle();
		 * String[] authors = parser.getAuthors(); String[] topics =
		 * parser.getTopics(); String[] bodies = parser.getBody(); String[] urls
		 * = parser.getUrl(); String[] urlDocs = parser.getUrlDoc();
		 * 
		 * for (int i = 0; i < titles.length; i++) {
		 * System.out.println("Title = " + titles[i]);
		 * System.out.println("Authors = " + authors[i]);
		 * System.out.println("Topic = " + topics[i]);
		 * System.out.println("Body Length = " + bodies[i].length());
		 * System.out.println("Url = " + urls[i]);
		 * System.out.println("UrlDoc = " + urlDocs[i]); System.out.println(); }
		 * } else { System.out.println("Link is not a Google Scholar page"); }
		 */
		/*
		 * PDDocument pd; BufferedWriter wr; try { File input = new
		 * File("test.pdf"); File output = new File("SampleText.txt"); pd =
		 * PDDocument.load(input); PDFTextStripper stripper = new
		 * PDFTextStripper(); wr = new BufferedWriter(new OutputStreamWriter(new
		 * FileOutputStream(output))); stripper.writeText(pd, wr); if (pd !=
		 * null) pd.close(); wr.close(); } catch (Exception e){
		 * e.printStackTrace(); }
		 */
	}
}

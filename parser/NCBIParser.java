package parser;

import java.io.*;
import java.net.*;
import java.util.*;

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

public class NCBIParser extends Parser {
	private Map<String, String> cookies = null;
	private static String domain = "http://www.ncbi.nlm.nih.gov";

	private String removeParameter(String fullUrl) {
		return fullUrl;
	}

	// Parse the document title
	private void parseTitle() {
		Elements titleElems = doc.select("title");
		if (titleElems.size() == 1) {
			String titleText = new String(titleElems.get(0).text());
			titleText = titleText.trim();
			// System.out.println("Title = "+titleText);
			this.title.add(titleText);
		}
	}

	// Parse the document body/abstract
	private void parseBody() {
		Elements abstractElems = doc.select("div[id~=__abstractid]");
		if (abstractElems.size() == 1) {
			String abstractText = new String(abstractElems.get(0).html());
			abstractText = abstractText.trim();
			// System.out.println("Title = "+titleText);
			this.body.add(abstractText);
		}
	}

	// Parse the document author name
	private void parseAuthors() {
		Elements authorElems = doc.select("meta[name=citation_authors]");
		if (authorElems.size() == 1) {
			String abstractText = new String(authorElems.get(0).attr("content")
					.toString());
			abstractText = abstractText.trim();
			// System.out.println("Title = "+titleText);
			this.authors.add(abstractText);
		}
	}

	// Parse the document topic
	private void parseTopics() {
		Elements topicElems = doc.select("span[class=kwd-text]");
		if (topicElems.size() == 1) {
			String topicText = new String(topicElems.get(0).text());
			topicText = topicText.trim();
			// System.out.println("Title = "+titleText);
			this.topics.add(topicText);
		}
	}

	// Parse the document url
	private void parseUrl() {
		Elements urlElems = doc.select("link[rel=canonical]");
		if (urlElems.size() == 1) {
			String urlText = new String(urlElems.get(0).attr("href").toString());
			urlText = NCBIParser.domain + urlText;
			urlText = urlText.trim();
			// System.out.println("Title = "+titleText);
			this.urls.add(urlText);
		}
	}

	// Parse the cited by articles
	public void parseCitedByArticles() {
		Elements sidebarElems = doc.select("div[class=portlet]");
		// Go through all the side bar elems to find the one with cited by
		// articles
		for (int i = 0; i < sidebarElems.size(); i++) {
			Elements spanElems = sidebarElems.get(i).select("span");
			boolean citedBySideBar = false;

			// Check all the span elements for the correct title
			for (int j = 0; j < spanElems.size(); j++) {
				String spanText = spanElems.get(j).text().trim();

				if (spanText.equals("Cited by other articles in PMC")) {
					citedBySideBar = true;
					break;
				}
			}

			// If the current side bar elem is the cited by one
			if (citedBySideBar) {
				Elements citedByUrlElems = sidebarElems.get(i).select(
						"a[class=brieflinkpopperctrl]");
				for (int j = 0; j < citedByUrlElems.size(); j++) {
					String urlText = new String(citedByUrlElems.get(j)
							.attr("href").toString());
					urlText = NCBIParser.domain + urlText;
					urlText = urlText.trim();
					// System.out.println("Title = "+titleText);
					this.citedByArticle.add(urlText);
				}

				break;
			}
		}
	}

	// Parse the related citation articles
	public void parseRelatedCitedArticles() {
		Elements sidebarElems = doc.select("div[class=portlet]");
		// Go through all the side bar elems to find the one with cited by
		// articles
		for (int i = 0; i < sidebarElems.size(); i++) {
			Elements spanElems = sidebarElems.get(i).select("span");
			boolean citedBySideBar = false;

			// Check all the span elements for the correct title
			for (int j = 0; j < spanElems.size(); j++) {
				String spanText = spanElems.get(j).text().trim();

				if (spanText.equals("Related citations in PubMed")) {
					citedBySideBar = true;
					break;
				}
			}

			// If the current side bar elem is the cited by one
			if (citedBySideBar) {
				Elements relatedCitedUrlElems = sidebarElems.get(i).select(
						"a[class=brieflinkpopperctrl]");
				for (int j = 0; j < relatedCitedUrlElems.size(); j++) {
					String urlText = new String(relatedCitedUrlElems.get(j)
							.attr("href").toString());
					urlText = NCBIParser.domain + urlText;
					urlText = urlText.trim();
					// System.out.println("Title = "+titleText);
					this.relatedCitedArticle.add(urlText);
				}

				break;
			}
		}
	}

	// Parse the document url
	private void parseUrlDocs() {
	}

	public NCBIParser() {
	}

	public NCBIParser(String url) {
		this.setURL(url);
		this.authors = new ArrayList<String>();
		this.topics = new ArrayList<String>();
		this.title = new ArrayList<String>();
		this.body = new ArrayList<String>();
		this.urls = new ArrayList<String>();
		this.urlDocs = new ArrayList<String>();
		this.citedByArticle = new ArrayList<String>();
		this.relatedCitedArticle = new ArrayList<String>();
	}

	public String getURL() {
		return URL;
	}

	public void setDocument(Document doc) {
		this.doc = new Document(doc.outerHtml());
		new NCBIParser("");
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

	// Parse out the document links from search page
	public String[] parseDocLinksSearchPage(String term, int page) {
		// Try to get the content of search page
		System.out.println("Try to get doc links for term " + term
				+ " in page " + page);

		Document searchDoc = null;
		String queryString = URLEncoder.encode(term.trim());
		if (page == 1) {
			try {

				Response response = Jsoup
						.connect(
								"http://www.ncbi.nlm.nih.gov/pmc/?term="
										+ queryString)
						.userAgent(
								"Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
						.timeout(40000).followRedirects(true).execute();
				System.out.println("Start cookie");
				this.cookies = response.cookies();
				for (String cookie : cookies.keySet()) {
					System.out.println(cookie + " = " + cookies.get(cookie));
				}
				System.out.println("End cookie");
				searchDoc = response.parse();

			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else if (page > 1) {
			searchDoc = Jsoup.parse(this.excutePost(queryString, page));
		} else {
			return null;
		}

		ArrayList<String> docLinks = new ArrayList<String>();

		// Go through all the search result in the search page to get out the
		// document links
		Elements searchDocElems = searchDoc.select("div[class=rprt]");
		for (int i = 0; i < searchDocElems.size(); i++) {
			Elements linkElems = searchDocElems.get(i).select("a");

			if (linkElems.size() > 0) {
				String singleDocLink = new String(linkElems.get(0).attr("href")
						.toString());
				singleDocLink = NCBIParser.domain + singleDocLink;
				singleDocLink = singleDocLink.trim();
				// System.out.println("Title = "+titleText);
				docLinks.add(singleDocLink);
			}
		}

		String[] resultDocLinks = new String[docLinks.size()];
		for (int i = 0; i < docLinks.size(); i++)
			resultDocLinks[i] = docLinks.get(i);

		return resultDocLinks;
	}

	public void parseDoc() {
		if (doc == null)
			return;

		this.isContentPage = (this.URL
				.indexOf("http://www.ncbi.nlm.nih.gov/pmc/articles/") != -1);

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

		// Parse the cited by articles
		this.parseCitedByArticles();

		// Parse the related citation articles
		this.parseRelatedCitedArticles();
	}

	public String excutePost(String term, int page) {
		URL url;
		HttpURLConnection connection = null;
		String urlParameters = "term="
				+ term
				+ "&EntrezSystem2.PEntrez.PMC.Pmc_PageController.PreviousPageName=results&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailReport=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailFormat=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailCount=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailStart=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailSort=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.Email=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailSubject=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailText=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailQueryKey=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.QueryDescription=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.sPresentation=DocSum&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.sPageSize=20&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.sSort=none&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.FFormat=DocSum&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.FSort=&email_format=DocSum&email_sort=&email_count=20&email_start="
				+ ((page - 2) * 20 + 1)
				+ "&email_address=&email_add_text=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.FileFormat=docsum&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.LastPresentation=docsum&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.Presentation=docsum&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.PageSize=20&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.LastPageSize=20&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.Sort=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.LastSort=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.FileSort=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.Format=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.LastFormat=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Entrez_Pager.cPage="
				+ (page - 1)
				+ "&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Entrez_Pager.CurrPage="
				+ page
				+ "&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_ResultsSearchController.ResultCount=1760286&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_ResultsSearchController.RunLastQuery=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Entrez_Pager.cPage="
				+ (page - 1)
				+ "&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.sPresentation2=DocSum&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.sPageSize2=20&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.sSort2=none&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.FFormat2=DocSum&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.FSort2=&email_format2=DocSum&email_sort2=&email_count2=20&email_start2="
				+ ((page - 2) * 20 + 1)
				+ "&email_address2=&email_add_text2=&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_FilterTab.CurrFilter=all&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_FilterTab.LastFilter=all&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_MultiItemSupl.Pmc_RelatedDataLinks.rdDatabase=rddbto&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_MultiItemSupl.Pmc_RelatedDataLinks.DbName=pmc&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Discovery_SearchDetails.SearchDetailsTerm=medical%5BAll+Fields%5D&EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.HistoryDisplay.Cmd=PageChanged&EntrezSystem2.PEntrez.DbConnector.Db=pmc&EntrezSystem2.PEntrez.DbConnector.LastDb=pmc&EntrezSystem2.PEntrez.DbConnector.Term=medical&EntrezSystem2.PEntrez.DbConnector.LastTabCmd=&EntrezSystem2.PEntrez.DbConnector.LastQueryKey=1&EntrezSystem2.PEntrez.DbConnector.IdsFromResult=&EntrezSystem2.PEntrez.DbConnector.LastIdsFromResult=&EntrezSystem2.PEntrez.DbConnector.LinkName=&EntrezSystem2.PEntrez.DbConnector.LinkReadableName=&EntrezSystem2.PEntrez.DbConnector.LinkSrcDb=&EntrezSystem2.PEntrez.DbConnector.Cmd=PageChanged&EntrezSystem2.PEntrez.DbConnector.TabCmd=&EntrezSystem2.PEntrez.DbConnector.QueryKey=&p%24a=EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Entrez_Pager.Page&p%24l=EntrezSystem2&p%24st=pmc";
		try {
			// Create connection
			url = new URL("http://www.ncbi.nlm.nih.gov/pmc");
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
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
			connection.setRequestProperty("Origin",
					"http://www.ncbi.nlm.nih.gov");
			connection.setRequestProperty("Host", "www.ncbi.nlm.nih.gov");
			connection.setRequestProperty("Referer",
					"http://www.ncbi.nlm.nih.gov/pmc");
			// connection.setRequestProperty("Cookie",
			// "WT_FPC=id=2d34ad47d68c16ff7ce1400795473914:lv=1400795477787:ss=1400795473914; pmc.article.report=classic; prevsearch=; hovernext=; WebEnv=1jMDpfyAHfrILmemz1WiilzmVfzR80tUOHTryNs15F7J6_8eM2klg1ZQ6vig_69-F8j_9pLeo5eWIXHkMtkmtPE6q7nIkVQLaYmAi%408A1A221437E55051_0035SID; ncbi_sid=8A1A221437E55051_0035SID; clicknext=link_id%3DEntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Entrez_Pager.Page%26link_name%3DEntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Entrez_Pager.Page%26link_href%3Dhttp%253A%252F%252Fwww.ncbi.nlm.nih.gov%252Fpmc%252F%253Fterm%253Dmedical%2523%26link_text%3DNext%2520%253E%26link_class%3Dactive%252Cpage_link%252Cnext%26browserwidth%3D1920%26browserheight%3D203%26evt_coor_x%3D1163%26evt_coor_y%3D2650%26jseventms%3D5tw6gd%26iscontextmenu%3Dfalse%26jsevent%3Dclicknext%26ancestorId%3Dmaincontent%2CEntrezForm%26ancestorClassName%3Dpagination%2Ctitle_and_pager%2Cbottom%2Ccontent%2Ccol%2Cnine_col%26maxScroll_x%3D0%26maxScroll_y%3D2932%26currScroll_x%3D0%26currScroll_y%3D2632%26hasScrolled%3Dtrue%26ncbi_phid%3DF4FB4872386F5EC1000000000031B223%26sgSource%3Dnative; ncbi_prevPHID=F4FB4872386F5EC1000000000031B223; unloadnext=jsevent%3Dunloadnext%26ncbi_pingaction%3Dunload%26ncbi_timeonpage%3D6643%26ncbi_onloadTime%3D903%26jsperf_dns%3D0%26jsperf_connect%3D76%26jsperf_ttfb%3D750%26jsperf_basePage%3D90%26jsperf_frontEnd%3D936%26jsperf_navType%3D1%26jsperf_redirectCount%3D0%26maxScroll_x%3D0%26maxScroll_y%3D2932%26currScroll_x%3D0%26currScroll_y%3D2632%26hasScrolled%3Dtrue%26ncbi_phid%3DF4FB4872386F5EC1000000000031B223%26sgSource%3Dnative");
			String cookieString = "";
			for (String cookie : this.cookies.keySet()) {
				cookieString += cookie + "=" + this.cookies.get(cookie) + "; ";
			}
			System.out.println("Cookie String = " + cookieString);
			connection.setRequestProperty("Cookie", cookieString);

			connection.setRequestProperty("Content-Length",
					"" + Integer.toString(urlParameters.getBytes().length));

			// connection.setUseCaches (false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			// Get Response
			InputStream is = new java.util.zip.GZIPInputStream(
					connection.getInputStream());
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
		 * String[] docLinks = NCBIParser.parseDocLinksSearchPage(
		 * "http://www.ncbi.nlm.nih.gov/pmc/?term=medical"); for (int i = 0; i <
		 * docLinks.length; i++) { System.out.println("Doc link " + i + " = " +
		 * docLinks[i]); }
		 */

		/* NCBIParser.excutePost("medical", 2); */

		/*
		 * NCBIParser parser = new
		 * NCBIParser("http://www.ncbi.nlm.nih.gov/pmc/articles/PMC3487491/");
		 * parser.parseDoc();
		 * 
		 * if (parser.isContentPage()) { String[] titles = parser.getTitle();
		 * String[] authors = parser.getAuthors(); String[] topics =
		 * parser.getTopics(); String[] bodies = parser.getBody(); String[] urls
		 * = parser.getUrl(); String[] citedByArticleUrls =
		 * parser.getUrlCitedByArticles(); String[] urlDocs =
		 * parser.getUrlDoc();
		 * 
		 * if (titles != null && titles.length > 0)
		 * System.out.println("Title = " + titles[0]); if (authors != null &&
		 * authors.length > 0) System.out.println("Authors = " + authors[0]); if
		 * (topics != null && topics.length > 0) System.out.println("Topic = " +
		 * topics[0]); if (bodies != null && bodies.length > 0)
		 * System.out.println("Abstract = " + bodies[0]); if (urls != null &&
		 * urls.length > 0) System.out.println("Url = " + urls[0]); if
		 * (citedByArticleUrls != null) { for (int i = 0; i <
		 * citedByArticleUrls.length; i++) { System.out.println("Cited By = " +
		 * citedByArticleUrls[i]); } }
		 * 
		 * if (urlDocs != null && urlDocs.length > 0)
		 * System.out.println("UrlDoc = " + urlDocs[0]); System.out.println(); }
		 * else { System.out.println("Link is not a Medline page"); }
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

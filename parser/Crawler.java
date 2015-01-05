package parser;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class Crawler {
	protected String dbDomain = "http://127.0.0.1:5000";

	protected void startCrawl(boolean timeOut, long duration) {
	};

	// Send an http request given url and the parameters
	protected void sendHttpRequest(String requestUrl, String urlParameters) {
		URL dbUrl;
		HttpURLConnection connection = null;

		try {
			System.out.println("Request URL = " + requestUrl);
			dbUrl = new URL(requestUrl);
			connection = (HttpURLConnection) dbUrl.openConnection();
			connection.setRequestMethod("PUT");
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");

			connection.setRequestProperty("Content-Length",
					"" + Integer.toString(urlParameters.getBytes().length));
			connection.setRequestProperty("Content-Language", "en-US");

			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.writeBytes(urlParameters);
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
		} catch (Exception e) {
			// e.printStackTrace();
			System.out.println("Request failed");
			System.out.println();
		}
	}
	
	protected void waitSec(int lowerBound, int upperBound) {
		try {
			int waitTime = lowerBound
					* 1000
					+ (int) (Math.random() * ((upperBound * 1000 - lowerBound * 1000) + 1));
			System.out.println("Wait for " + waitTime);
			Thread.currentThread();
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// Hash a plain string text
	protected String hash(String plainText) {
		MessageDigest messageDigest = null;

		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}

		return (new HexBinaryAdapter()).marshal(messageDigest.digest(plainText
				.getBytes()));
	}

	// Create an author node for the graph db
	protected void createAuthorNode(String authorString) {
		String targetURL = dbDomain + "/authors/";

		// Split the authorsString into each author name
		if (authorString.length() < 1)
			return;

		System.out.println("Author = " + authorString);
		String urlParameters = "name=" + authorString;
		String requestUrl = targetURL + this.hash(authorString);

		// Send the create node request
		this.sendHttpRequest(requestUrl, urlParameters);

		System.out.println();
	}

	// Create a relationship between document and author nodes
	protected void createAuthorDocumentRelation(String authorString, String url) {
		String requestUrl = dbDomain + "/documents/" + this.hash(url)
				+ "/authors/" + this.hash(authorString);
		String urlParameters = "";
		this.sendHttpRequest(requestUrl, urlParameters);

		System.out.println("Relationship between " + authorString + " and url "
				+ url);

		// Send the create relationship request
		this.sendHttpRequest(requestUrl, urlParameters);
	}

	// Create a document node for the graph db
	protected void createDocumentNode(String title, String url, String bodyText) {
		String targetURL = dbDomain + "/documents/";

		title = title.trim();
		url = url.trim();
		if (title.length() < 1 || url.length() < 1)
			return;

		System.out.println("Title = " + title);
		System.out.println("Url = " + url);
		String urlParameters = "title=" + title + "&url=" + url + "&abstract="
				+ bodyText;
		String requestUrl = targetURL + this.hash(url);

		// Send the create node request
		this.sendHttpRequest(requestUrl, urlParameters);

		System.out.println();
	}

	// Create a relationship between entity and document nodes
	protected void createEntityDocumentRelation(String entityString, String url) {
		String requestUrl = dbDomain + "/documents/" + this.hash(url)
				+ "/entities/" + this.hash(entityString);
		String urlParameters = "";
		this.sendHttpRequest(requestUrl, urlParameters);

		System.out.println("Relationship between " + entityString + " and url "
				+ url);

		// Send the create relationship request
		this.sendHttpRequest(requestUrl, urlParameters);
	}

	// Create an entity node for the graph db
	protected void createEntityNode(String entityString) {
		String targetURL = dbDomain + "/entities/";

		// Split the authorsString into each author name
		if (entityString.length() < 1)
			return;

		System.out.println("Entity = " + entityString);
		String urlParameters = "name=" + entityString;
		String requestUrl = targetURL + this.hash(entityString);

		// Send the create node request
		this.sendHttpRequest(requestUrl, urlParameters);

		System.out.println();
	}

	// Create a relationship between document and cited by document nodes
	protected void createCitedByRelation(String url, String citedByUrl) {
		String requestUrl = dbDomain + "/documents/" + this.hash(url)
				+ "/cites/" + this.hash(citedByUrl);
		String urlParameters = "";
		this.sendHttpRequest(requestUrl, urlParameters);

		System.out.println(url + "cited by " + citedByUrl);

		// Send the create relationship request
		this.sendHttpRequest(requestUrl, urlParameters);
	}
}

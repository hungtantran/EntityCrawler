package parser;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import edu.stanford.nlp.ie.*;
import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.Tokenizer;

public class DocAnalyzer {
	private LexicalizedParser lp;
	private TreebankLanguagePack tlp;
	
	// FEFF because this is the Unicode char represented by the UTF-8 byte order
	// mark (EF BB BF).
	public static final String UTF8_BOM = "\uFEFF";
	private final String serializedClassifier = "classifiers/english.all.3class.distsim.crf.ser.gz";
	private final String locationTag = "LOCATION";
	private final String timeTag = "TIME";
	private final String personTag = "PERSON";
	private final String organizationTag = "ORGANIZATION";
	private final String moneyTag = "MONEY";
	private final String percentTag = "PERCENT";
	private final String dateTag = "DATE";

	private String doc = null;
	private String fileName = null;
	private String fullXmlString = null;

	// Potential tags
	private HashSet<String> location;
	private HashSet<String> time;
	private HashSet<String> person;
	private HashSet<String> organization;
	private HashSet<String> money;
	private HashSet<String> percent;
	private HashSet<String> date;
	private AbstractSequenceClassifier<CoreLabel> classifier;

	public DocAnalyzer() {
		this.location = new HashSet<String>();
		this.time = new HashSet<String>();
		this.person = new HashSet<String>();
		this.organization = new HashSet<String>();
		this.money = new HashSet<String>();
		this.percent = new HashSet<String>();
		this.date = new HashSet<String>();
		this.classifier = CRFClassifier
				.getClassifierNoExceptions(this.serializedClassifier);

		this.lp = LexicalizedParser
				.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		this.tlp = this.lp.getOp().langpack();
	}
	
	// Parse the grammatical structure of a sentence
	public List<TaggedWord> parseSentence(String sentence) {
		// Use the default tokenizer for this TreebankLanguagePack
		Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory()
				.getTokenizer(new StringReader(sentence));
		List<? extends HasWord> sentence2 = toke.tokenize();
		Tree parse = this.lp.parse(sentence2);
		
		List<TaggedWord> list = parse.taggedYield();
		System.out.println(list);
		/*for (TaggedWord label : list) {
			System.out.println(label.tag() + " = " + label.word());
		}*/
		
		return list;
	}
	
	public void setDoc(String doc) {
		this.doc = doc;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;

		// If it is a file, read its content into doc private variable
		if (this.fileName != null) {
			try {
				this.doc = IOUtils.slurpFile(this.fileName);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void findTag(String tag, HashSet<String> tagSet,
			String xmlString) {
		Pattern locationRegex = Pattern.compile("<" + tag + ">(.*?)</" + tag
				+ ">", Pattern.DOTALL);
		Matcher matcher = locationRegex.matcher(xmlString);
		// System.out.print(tag + ": ");
		while (matcher.find()) {
			String dataElements = matcher.group(1).trim();
			// Remove new line character
			dataElements = dataElements.replaceAll("(\\r|\\n)", "");
			// System.out.print(dataElements + ", ");
			tagSet.add(dataElements);
		}
		// System.out.println("");
		// System.out.println("Size = " + tagSet.size());
	}

	public void analyzeDoc() {
		if (this.doc == null)
			return;

		this.fullXmlString = this.classifier.classifyWithInlineXML(this.doc);
		// System.out.println(this.fullXmlString);

		DocAnalyzer
				.findTag(this.locationTag, this.location, this.fullXmlString);
		DocAnalyzer.findTag(this.timeTag, this.time, this.fullXmlString);
		DocAnalyzer.findTag(this.personTag, this.person, this.fullXmlString);
		DocAnalyzer.findTag(this.organizationTag, this.organization,
				this.fullXmlString);
		DocAnalyzer.findTag(this.moneyTag, this.money, this.fullXmlString);
		DocAnalyzer.findTag(this.percentTag, this.percent, this.fullXmlString);
		DocAnalyzer.findTag(this.dateTag, this.date, this.fullXmlString);
	}

	public HashSet<String> getLocationTags() {
		return this.location;
	}

	public HashSet<String> getTimeTags() {
		return this.time;
	}

	public HashSet<String> getPersonTags() {
		System.out.println(this.person.toString());
		return this.person;
	}

	public HashSet<String> getOrganizationTags() {
		return this.organization;
	}

	public HashSet<String> getMoneyTags() {
		return this.money;
	}

	public HashSet<String> getPercentTags() {
		return this.percent;
	}

	public HashSet<String> getDateTags() {
		return this.date;
	}

	public static void main(String[] args) throws IOException {

		// DocAnalyzer analyzer = new DocAnalyzer();
		// analyzer.parseSentence("Today is the anniversary of the 1989 military attack on pro-reform protesters in Beijing. As much as censors can manage, the day will go undebated in China.");
		// analyzer.setDoc(doc);
		// analyzer.setFileName("test.txt"); analyzer.analyzeDoc();
		// analyzer.getPersonTags();

	}
}


package org.geometerplus.fbreader.formats.daisy3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geometerplus.fbreader.bookmodel.BookReader;
import org.geometerplus.fbreader.bookmodel.FBTextKind;
import org.geometerplus.fbreader.formats.util.MiscUtil;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.xml.ZLStringMap;
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter;

public class Daisy3XMLReader extends ZLXMLReaderAdapter {
	private static final HashMap<String,Daisy3XMLTagAction> ourTagActions = new HashMap<String,Daisy3XMLTagAction>();

	public static Daisy3XMLTagAction addAction(String tag, Daisy3XMLTagAction action) {
		Daisy3XMLTagAction old = (Daisy3XMLTagAction)ourTagActions.get(tag);
		ourTagActions.put(tag, action);
		return old;
	}

	public static void fillTagTable() {
		if (!ourTagActions.isEmpty()) {
			return;
		}

		//addAction("html", new XHTMLTagAction());
		//addAction("body", new XHTMLTagBodyAction());
		//addAction("title", new XHTMLTagAction());
		//addAction("meta", new XHTMLTagAction());
		//addAction("script", new XHTMLTagAction());

		//addAction("font", new XHTMLTagAction());
		//addAction("style", new XHTMLTagAction());

		addAction("p", new Daisy3XMLTagParagraphAction());
		addAction("h1", new Daisy3XMLTagParagraphWithControlAction(FBTextKind.H1));
		addAction("h2", new Daisy3XMLTagParagraphWithControlAction(FBTextKind.H2));
		addAction("h3", new Daisy3XMLTagParagraphWithControlAction(FBTextKind.H3));
		addAction("h4", new Daisy3XMLTagParagraphWithControlAction(FBTextKind.H4));
		addAction("h5", new Daisy3XMLTagParagraphWithControlAction(FBTextKind.H5));
		addAction("h6", new Daisy3XMLTagParagraphWithControlAction(FBTextKind.H6));

		//addAction("ol", new XHTMLTagAction());
		//addAction("ul", new XHTMLTagAction());
		//addAction("dl", new XHTMLTagAction());
//		addAction("li", new XHTMLTagItemAction());
//
//		addAction("strong", new XHTMLTagControlAction(FBTextKind.STRONG));
//		addAction("b", new XHTMLTagControlAction(FBTextKind.BOLD));
//		addAction("em", new XHTMLTagControlAction(FBTextKind.EMPHASIS));
//		addAction("i", new XHTMLTagControlAction(FBTextKind.ITALIC));
//		final XHTMLTagAction codeControlAction = new XHTMLTagControlAction(FBTextKind.CODE);
//		addAction("code", codeControlAction);
//		addAction("tt", codeControlAction);
//		addAction("kbd", codeControlAction);
//		addAction("var", codeControlAction);
//		addAction("samp", codeControlAction);
//		addAction("cite", new XHTMLTagControlAction(FBTextKind.CITE));
//		addAction("sub", new XHTMLTagControlAction(FBTextKind.SUB));
//		addAction("sup", new XHTMLTagControlAction(FBTextKind.SUP));
//		addAction("dd", new XHTMLTagControlAction(FBTextKind.DEFINITION_DESCRIPTION));
//		addAction("dfn", new XHTMLTagControlAction(FBTextKind.DEFINITION));
//		addAction("strike", new XHTMLTagControlAction(FBTextKind.STRIKETHROUGH));
//
//		addAction("a", new XHTMLTagHyperlinkAction());
//
//		addAction("img", new XHTMLTagImageAction("src"));
//		addAction("object", new XHTMLTagImageAction("data"));

		//addAction("area", new XHTMLTagAction());
		//addAction("map", new XHTMLTagAction());

		//addAction("base", new XHTMLTagAction());
		//addAction("blockquote", new XHTMLTagAction());
		//addAction("br", new XHTMLTagRestartParagraphAction());
		//addAction("center", new XHTMLTagAction());
		//addAction("div", new XHTMLTagParagraphAction());
		//addAction("dt", new XHTMLTagParagraphAction());
		//addAction("head", new XHTMLTagAction());
		//addAction("hr", new XHTMLTagAction());
		//addAction("link", new XHTMLTagAction());
		//addAction("param", new XHTMLTagAction());
		//addAction("q", new XHTMLTagAction());
		//addAction("s", new XHTMLTagAction());

		//addAction("pre", new XHTMLTagPreAction());
		//addAction("big", new XHTMLTagAction());
		//addAction("small", new XHTMLTagAction());
		//addAction("u", new XHTMLTagAction());

		//addAction("table", new XHTMLTagAction());
		//addAction("td", new XHTMLTagParagraphAction());
		//addAction("th", new XHTMLTagParagraphAction());
		//addAction("tr", new XHTMLTagAction());
		//addAction("caption", new XHTMLTagAction());
		//addAction("span", new Daisy3XMLTagAction());
	}

	private final BookReader myModelReader;
	String myPathPrefix;
	String myLocalPathPrefix;
	String myReferencePrefix;
	boolean myPreformatted;
	boolean myInsideBody;
	private final Map<String,Integer> myFileNumbers;

	public Daisy3XMLReader(BookReader modelReader, Map<String,Integer> fileNumbers) {
		myModelReader = modelReader;
		myFileNumbers = fileNumbers;
	}

	final BookReader getModelReader() {
		return myModelReader;
	}

	public final String getFileAlias(String fileName) {
		fileName = MiscUtil.decodeHtmlReference(fileName);
		Integer num = myFileNumbers.get(fileName);
		if (num == null) {
			num = myFileNumbers.size();
			myFileNumbers.put(fileName, num);
		}
		return num.toString();
	}

	public boolean readFile(ZLFile file, String referencePrefix) {
		fillTagTable();

		myReferencePrefix = referencePrefix;

		myPathPrefix = MiscUtil.htmlDirectoryPrefix(file);
		myLocalPathPrefix = MiscUtil.archiveEntryName(myPathPrefix);

		myPreformatted = false;
		myInsideBody = false;

		return read(file);
	}

	public boolean startElementHandler(String tag, ZLStringMap attributes) {
		String id = attributes.getValue("id");
		if (id != null) {
			myModelReader.addHyperlinkLabel(myReferencePrefix + id);
		}

		Daisy3XMLTagAction action = (Daisy3XMLTagAction)ourTagActions.get(tag.toLowerCase());
		if (action != null) {
			action.doAtStart(this, attributes);
		}
		return false;
	}

	public boolean endElementHandler(String tag) {
		Daisy3XMLTagAction action = (Daisy3XMLTagAction)ourTagActions.get(tag.toLowerCase());
		if (action != null) {
			action.doAtEnd(this);
		}
		return false;
	}

	public void characterDataHandler(char[] data, int start, int len) {
		if (myPreformatted) {
			final char first = data[start]; 
			if ((first == '\r') || (first == '\n')) {
				myModelReader.addControl(FBTextKind.CODE, false);
				myModelReader.endParagraph();
				myModelReader.beginParagraph();
				myModelReader.addControl(FBTextKind.CODE, true);
			}
			int spaceCounter = 0;
cycle:
			while (spaceCounter < len) {
				switch (data[start + spaceCounter]) {
					case 0x08:
					case 0x09:
					case 0x0A:
					case 0x0B:
					case 0x0C:
					case 0x0D:
					case ' ':
						break;
					default:
						break cycle;
				}
				++spaceCounter;
			}
			myModelReader.addFixedHSpace((short)spaceCounter);
			start += spaceCounter;
			len -= spaceCounter;
		}
		if (len > 0) {
			if (myInsideBody && !myModelReader.paragraphIsOpen()) {
				myModelReader.beginParagraph();
			}
			myModelReader.addData(data, start, len, false);
		}
	}

	private static ArrayList<String> ourExternalDTDs = new ArrayList<String>();

	public static List<String> xhtmlDTDs() {
		if (ourExternalDTDs.isEmpty()) {
			ourExternalDTDs.add("data/formats/xhtml/xhtml-lat1.ent");
			ourExternalDTDs.add("data/formats/xhtml/xhtml-special.ent");
			ourExternalDTDs.add("data/formats/xhtml/xhtml-symbol.ent");
		}
		return ourExternalDTDs;
	}

	public List<String> externalDTDs() {
		return xhtmlDTDs();
	}

	public boolean dontCacheAttributeValues() {
		return true;
	}

	public boolean processNamespaces() {
		return true;
	}

	public void namespaceMapChangedHandler(HashMap<String,String> namespaceMap) {
	}
}

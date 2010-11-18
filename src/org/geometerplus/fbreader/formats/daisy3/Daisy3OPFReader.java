package org.geometerplus.fbreader.formats.daisy3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.BookReader;
import org.geometerplus.fbreader.bookmodel.FBTextKind;
import org.geometerplus.fbreader.constants.XMLNamespace;
import org.geometerplus.fbreader.formats.util.MiscUtil;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLFileImage;
import org.geometerplus.zlibrary.core.xml.ZLStringMap;
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter;

class Reference {
	public final String Title;
	public final String HRef;

	public Reference(String title, String href) {
		Title = title;
		HRef = href;
	}
}

class Daisy3OPFReader extends ZLXMLReaderAdapter implements XMLNamespace {
	private static final char[] Dots = new char[] {'.', '.', '.'};

	private final BookReader myModelReader;
	private final HashMap<String,String> myIdToHref = new HashMap<String,String>();
//	private final ArrayList<String> myHtmlFileNames = new ArrayList<String>();
	private String daisy3XMLFileName;
	private final ArrayList<Reference> myTourTOC = new ArrayList<Reference>();
	private final ArrayList<Reference> myGuideTOC = new ArrayList<Reference>();

	private String myOPFSchemePrefix;
	private String myFilePrefix;
	private String myNCXTOCFileName;

	Daisy3OPFReader(BookModel model) {
		myModelReader = new BookReader(model);
	}

	private TreeMap<String,Integer> myFileNumbers = new TreeMap<String,Integer>();
	private TreeMap<String,Integer> myTOCLabels = new TreeMap<String,Integer>();

	boolean readBook(ZLFile file) {
		myFilePrefix = MiscUtil.htmlDirectoryPrefix(file);

		myIdToHref.clear();
		myNCXTOCFileName = null;
		myTourTOC.clear();
		myGuideTOC.clear();
//		myState = READ_NONE;

		if (!read(file)) {
			return false;
		}

		myModelReader.setMainTextModel();
		myModelReader.pushKind(FBTextKind.REGULAR);

		// Get the xml file to be read
		final String extension = file.getExtension().intern();
		if(extension == "opf"){
			ZLFile parentDirectory = file.getParent();
			List<ZLFile> children =  parentDirectory.children();
			for(ZLFile daisy3content : children){
				if(daisy3content.getExtension() == "ncx"){
					myNCXTOCFileName = daisy3content.getName(false);
				}
				if(daisy3content.getExtension() == "xml"){
					daisy3XMLFileName = daisy3content.getName(false);
					System.out.println("*** Daisy3OpfReader, daisy3XMLFilePath = "+daisy3XMLFileName);
					System.out.println(myFilePrefix+daisy3XMLFileName);

					final ZLFile daisy3XmlFile = ZLFile.createFileByPath(myFilePrefix + daisy3XMLFileName);
					
					final Daisy3XMLReader reader = new Daisy3XMLReader(myModelReader, myFileNumbers);
					final String referenceName = reader.getFileAlias(MiscUtil.archiveEntryName(daisy3XmlFile.getPath()));

//					myModelReader.addHyperlinkLabel(referenceName);
//					myTOCLabels.put(referenceName, myModelReader.Model.BookTextModel.getParagraphsNumber());
					reader.readFile(daisy3XmlFile, referenceName + '#');
					myModelReader.insertEndOfSectionParagraph();

					generateTOC();

				}
			}
		}


		return true;
	}

	private BookModel.Label getTOCLabel(String id) {
		final int index = id.indexOf('#');
		
		System.out.println("**** getTOCLabel: id = "+id);
		final String path = (index >= 0) ? id.substring(index+1, id.length()) : id;
		
		Integer num = myFileNumbers.get(id);
		
		System.out.println("**** getTOCLabel: num = "+num);
		
		if (num == null) {
			return null;
		}
		if (index == -1) {
			System.out.println("**** getTOCLabel: index = -1");
			final Integer para = myTOCLabels.get(num.toString());
			if (para == null) {
				return null;
			}
			return new BookModel.Label(null, para);
		}
		//return myModelReader.Model.getLabel(num + id.substring(index));
		return myModelReader.Model.getLabel(num.toString());
	}
	
	Map<Integer,NCXReader.NavPoint> navigationMap;

	private void generateTOC() {
		if (myNCXTOCFileName != null) {
			final NCXReader ncxReader = new NCXReader(myModelReader);
			if (ncxReader.readFile(myFilePrefix + myNCXTOCFileName)) {
				navigationMap = ncxReader.navigationMap();
				if (!navigationMap.isEmpty()) {
					int level = 0;
					for (NCXReader.NavPoint point : navigationMap.values()) {
						
						myFileNumbers.put(point.ContentHRef, myFileNumbers.size());
						myModelReader.addHyperlinkLabel(point.ContentHRef);
						myTOCLabels.put(point.ContentHRef, myModelReader.Model.BookTextModel.getParagraphsNumber());

						final BookModel.Label label = getTOCLabel(point.ContentHRef);
						
						System.out.println("**** generateTOC: label="+label);
						
						int index = (label != null) ? label.ParagraphIndex : -1;
						while (level > point.Level) {
							myModelReader.endContentsParagraph();
							--level;
						}
						while (++level <= point.Level) {
							myModelReader.beginContentsParagraph(-2);
							myModelReader.addContentsData(Dots);
						}
						myModelReader.beginContentsParagraph(index);
						myModelReader.addContentsData(point.Text.toCharArray());
					}
					while (level > 0) {
						myModelReader.endContentsParagraph();
						--level;
					}
					return;
				}
			}
		}

		/* Not needed for daisy3 NCX file
		for (Reference ref : myTourTOC.isEmpty() ? myGuideTOC : myTourTOC) {
			final BookModel.Label label = getTOCLabel(ref.HRef);
			if (label != null) {
				final int index = label.ParagraphIndex;
				if (index != -1) {
					myModelReader.beginContentsParagraph(index);
					myModelReader.addContentsData(ref.Title.toCharArray());
					myModelReader.endContentsParagraph();
				}
			}
		}
		 */
	}

	private static final String MANIFEST = "manifest";
	private static final String SPINE = "spine";
	private static final String GUIDE = "guide";
	private static final String TOUR = "tour";
	private static final String SITE = "site";
	private static final String REFERENCE = "reference";
	private static final String ITEMREF = "itemref";
	private static final String ITEM = "item";

	private static final String COVER_IMAGE = "other.ms-coverimage-standard";

	private static final int READ_NONE = 0;
	private static final int READ_MANIFEST = 1;
	private static final int READ_SPINE = 2;
	private static final int READ_GUIDE = 3;
	private static final int READ_TOUR = 4;
	
	private int myState;

	public boolean startElementHandler(String tag, ZLStringMap xmlattributes) {
		tag = tag.toLowerCase();
		if ((myOPFSchemePrefix != null) && tag.startsWith(myOPFSchemePrefix)) {
			tag = tag.substring(myOPFSchemePrefix.length());
		}
		tag = tag.intern();
		if (MANIFEST == tag) {
			myState = READ_MANIFEST;
		} else if (SPINE == tag) {
			myNCXTOCFileName = myIdToHref.get(xmlattributes.getValue("toc"));
			myState = READ_SPINE;
		} else if (GUIDE == tag) {
			myState = READ_GUIDE;
		} else if (TOUR == tag) {
			myState = READ_TOUR;
		} else if ((myState == READ_MANIFEST) && (ITEM == tag)) {
			final String id = xmlattributes.getValue("id");
			String href = xmlattributes.getValue("href");
			if ((id != null) && (href != null)) {
				href = MiscUtil.decodeHtmlReference(href);
				myIdToHref.put(id, href);
			}
		} else if ((myState == READ_SPINE) && (ITEMREF == tag)) {
			final String id = xmlattributes.getValue("idref");
			if (id != null) {
				final String fileName = myIdToHref.get(id);
				if (fileName != null) {
//					myHtmlFileNames.add(fileName);
				}
			}
		} else if ((myState == READ_GUIDE) && (REFERENCE == tag)) {
			final String type = xmlattributes.getValue("type");
			final String title = xmlattributes.getValue("title");
			String href = xmlattributes.getValue("href");
			if (href != null) {
				href = MiscUtil.decodeHtmlReference(href);
				if (title != null) {
					myGuideTOC.add(new Reference(title, href));
				}
				if ((type != null) && (COVER_IMAGE.equals(type))) {
					myModelReader.setMainTextModel();
					final ZLFile imageFile = ZLFile.createFileByPath(myFilePrefix + href);
					final String imageName = imageFile.getName(false);
					myModelReader.addImageReference(imageName, (short)0);
					myModelReader.addImage(imageName, new ZLFileImage("image/auto", imageFile));
				}
			}
		} else if ((myState == READ_TOUR) && (SITE == tag)) {
			final String title = xmlattributes.getValue("title");
			String href = xmlattributes.getValue("href");
			if ((title != null) && (href != null)) {
				href = MiscUtil.decodeHtmlReference(href);
				myTourTOC.add(new Reference(title, href));
			}
		}
		return false;
	}

	public boolean endElementHandler(String tag) {
		tag = tag.toLowerCase();
		if ((myOPFSchemePrefix != null) && tag.startsWith(myOPFSchemePrefix)) {
			tag = tag.substring(myOPFSchemePrefix.length());
		}
		tag = tag.intern();
		if ((MANIFEST == tag) || (SPINE == tag) || (GUIDE == tag) || (TOUR == tag)) {
			myState = READ_NONE;
		}
		return false;
	}

	public boolean processNamespaces() {
		return true;
	}

	public void namespaceMapChangedHandler(HashMap<String,String> namespaceMap) {
		myOPFSchemePrefix = null;
		for (Map.Entry<String,String> entry : namespaceMap.entrySet()) {
			if (OpenPackagingFormat.equals(entry.getValue())) {
				myOPFSchemePrefix = entry.getKey() + ":";
				break;
			}
		}
	}

	public boolean dontCacheAttributeValues() {
		return true;
	}
	
}

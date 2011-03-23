package org.geometerplus.fbreader.formats.daisy3;


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
import org.geometerplus.zlibrary.core.xml.ZLStringMap;
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter;

/**
 * Serves as entry point for reading the Daisy3 file.
 * Locates the XML and NCX files.
 */
class Daisy3OPFReader extends ZLXMLReaderAdapter implements XMLNamespace {
	private static final char[] Dots = new char[] {'.', '.', '.'};

	private final BookReader myModelReader;
	private final HashMap<String,String> myIdToHref = new HashMap<String,String>();
	private String daisy3XMLFileName;
	private String myFilePrefix;
	private String myNCXTOCFileName;
	Map<Integer,NCXReader.NavPoint> navigationMap;


	Daisy3OPFReader(BookModel model) {
		myModelReader = new BookReader(model);
	}

	private TreeMap<String,Integer> myFileNumbers = new TreeMap<String,Integer>();
	private Daisy3XMLReader reader;
	boolean readBook(ZLFile file) {
		myFilePrefix = MiscUtil.htmlDirectoryPrefix(file);
		myIdToHref.clear();
		myNCXTOCFileName = null;
		if (!read(file)) {
			return false;
		}

		myModelReader.setMainTextModel();
		myModelReader.pushKind(FBTextKind.REGULAR);

		// Get the xml file to be read
		final String extension = file.getExtension().intern();
		String name = file.getName(true);
		if(extension == "opf" && !name.startsWith("._")){
			ZLFile parentDirectory = file.getParent();
			List<ZLFile> children =  parentDirectory.children();
			for(ZLFile daisy3content : children){
				String str = daisy3content.getName(true);
				
				// Get the NCX file name
				if(daisy3content.getExtension() == "ncx"){
					if(!str.startsWith("._")){
						myNCXTOCFileName = daisy3content.getName(false);
					}
				}
				
				//Get the XML file name. This file contains the Daisy3 content
				if(daisy3content.getExtension() == "xml"){
					if(!str.startsWith("._"))
					{
						daisy3XMLFileName = daisy3content.getName(false);
						final ZLFile daisy3XmlFile = ZLFile.createFileByPath(myFilePrefix + daisy3XMLFileName);
						reader = new Daisy3XMLReader(myModelReader, myFileNumbers);
						final String referenceName = "daisy3";
						reader.readFile(daisy3XmlFile, referenceName + '#');
					}
				}
			}
			generateTOC();
		}
		return true;
	}


	/**
	 * Generate the Table of contents.
	 * For each entry in the TOC, the corresponding paragraph number is fetched from the
	 * HashMap stored in Daisy3XMLTagLevelControlAction class.
	 */
	private void generateTOC() {
		HashMap<String, Integer> toc_para_map = Daisy3XMLTagLevelControlAction.getToc_paragraph_map();
		if (myNCXTOCFileName != null && toc_para_map != null) {
			final NCXReader ncxReader = new NCXReader(myModelReader);
			if (ncxReader.readFile(myFilePrefix + myNCXTOCFileName)) {
				navigationMap = ncxReader.navigationMap();
				if (!navigationMap.isEmpty()) {
					int level = 0;
					for (NCXReader.NavPoint point : navigationMap.values()) {
						
						String id = point.id;
						int para;
						if(id.trim() != ""){
							
							// If the retrieved value is null, then set the para value to 0.
							// This will take the link in TOC to the start of the book.
							if(toc_para_map.get(point.id) != null){
								para = toc_para_map.get(point.id).intValue();
							}
							else{
								para = 0;
							}
						}
						else{
							para = 0;
						}

						while (level > point.Level) {
							myModelReader.endContentsParagraph();
							--level;
						}
						while (++level <= point.Level) {
							myModelReader.beginContentsParagraph(-2);
							myModelReader.addContentsData(Dots);
						}
						
						// The paragraph number gets associated with the TOC entry in this method
						myModelReader.beginContentsParagraph(para);
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
	}
	public boolean startElementHandler(String tag, ZLStringMap xmlattributes) {
		//We are not parsing the opf file
		return true;
	}

	public boolean endElementHandler(String tag) {
		//We are not parsing the opf file
		return true;
	}

	public boolean processNamespaces() {
		return true;
	}

	public void namespaceMapChangedHandler(HashMap<String,String> namespaceMap) {
	}

	public boolean dontCacheAttributeValues() {
		return true;
	}
}

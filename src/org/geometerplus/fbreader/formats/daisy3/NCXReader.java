
package org.geometerplus.fbreader.formats.daisy3;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.geometerplus.fbreader.bookmodel.BookReader;
import org.geometerplus.fbreader.formats.util.MiscUtil;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.xml.ZLStringMap;
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter;


/**
 * NCX file reader for the daisy3 book
 * All the "navPoint" elements are stored in form
 * of a navigation map. This map is used or displaying 
 * table of contents.
 */
class NCXReader extends ZLXMLReaderAdapter {
	static class NavPoint {
		final int Order;
		final int Level;
		String Text = "";
		String id = "";
		int para;

		NavPoint(int order, int level) {
			Order = order;
			Level = level;			
		}
	}

	private final TreeMap<Integer,NavPoint> myNavigationMap = new TreeMap<Integer,NavPoint>();
	private final ArrayList<NavPoint> myPointStack = new ArrayList<NavPoint>();
	private BookReader myModelReader;
	private static final int READ_NONE = 0;
	private static final int READ_MAP = 1;
	private static final int READ_POINT = 2;
	private static final int READ_LABEL = 3;
	private static final int READ_TEXT = 4;

	int myReadState = READ_NONE;
	int myPlayIndex = -65535;
	private String myLocalPathPrefix;

	NCXReader(BookReader modelReader) {
		myModelReader = modelReader;
	}

	boolean readFile(String filePath) {
		final ZLFile file = ZLFile.createFileByPath(filePath);
		myLocalPathPrefix = MiscUtil.archiveEntryName(MiscUtil.htmlDirectoryPrefix(file));
		return read(file);
	}

	Map<Integer,NavPoint> navigationMap() {
		return myNavigationMap;
	}

	private static final String TAG_NAVMAP = "navmap";
	private static final String TAG_NAVPOINT = "navpoint";
	private static final String TAG_NAVLABEL = "navlabel";
	private static final String TAG_CONTENT = "content";
	private static final String TAG_TEXT = "text";

	private static final String ATTRIBUTE_ID = "id";
	private static final String ATTRIBUTE_PLAYORDER = "playOrder";
	

	private int atoi(String number) {
		try {
			return Integer.parseInt(number);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Override
	public boolean startElementHandler(String tag, ZLStringMap attributes) {
		tag = tag.toLowerCase().intern();
		switch (myReadState) {
			case READ_NONE:
				if (tag == TAG_NAVMAP) {
					myReadState = READ_MAP;
				}
				break;
			case READ_MAP:
				if (tag == TAG_NAVPOINT) {
					final String order = attributes.getValue(ATTRIBUTE_PLAYORDER);
					final int index = (order != null) ? atoi(order) : myPlayIndex++;
					myPointStack.add(new NavPoint(index, myPointStack.size()));
					myReadState = READ_POINT;
				}
				break;
			case READ_POINT:
				if (tag == TAG_NAVPOINT) {
					final String order = attributes.getValue(ATTRIBUTE_PLAYORDER);
					final String id = attributes.getValue(ATTRIBUTE_ID);
					System.out.println("*** attributes.getValue(ATTRIBUTE_ID) = "+id);
					final int index = (order != null) ? atoi(order) : myPlayIndex++;
					NavPoint navpoint = new NavPoint(index, myPointStack.size());
					navpoint.id = id;
					myPointStack.add(navpoint);
				} else if (tag == TAG_NAVLABEL) {
					myReadState = READ_LABEL;
				} else if (tag == TAG_CONTENT) {
					final int size = myPointStack.size();
					if (size > 0) {
					//	myPointStack.get(size - 1).id = myLocalPathPrefix + attributes.getValue("id");
					}
				}
				break;
			case READ_LABEL:
				if (TAG_TEXT == tag) {
					myReadState = READ_TEXT;
				}
				break;
			case READ_TEXT:
				break;
		}
		return false;
	}
	
	@Override
	public boolean endElementHandler(String tag) {
		tag = tag.toLowerCase().intern();
		switch (myReadState) {
			case READ_NONE:
				break;
			case READ_MAP:
				if (TAG_NAVMAP == tag) {
					myReadState = READ_NONE;
				}
				break;
			case READ_POINT:
				if (TAG_NAVPOINT == tag) {
					NavPoint last = myPointStack.get(myPointStack.size() - 1);
					if (last.Text.length() == 0) {
						last.Text = "...";
					}
					System.out.println("*** last.id = "+last.id);
					myNavigationMap.put(last.Order, last);
					myPointStack.remove(myPointStack.size() - 1);
					myReadState = (myPointStack.isEmpty()) ? READ_MAP : READ_POINT;
				}
			case READ_LABEL:
				if (TAG_NAVLABEL == tag) {
					myReadState = READ_POINT;
				}
				break;
			case READ_TEXT:
				if (TAG_TEXT == tag) {
					myReadState = READ_LABEL;
				}
				break;
		}
		return false;
	}
	
	@Override
	public void characterDataHandler(char[] ch, int start, int length) {
		if (myReadState == READ_TEXT) {
			final ArrayList<NavPoint> stack = myPointStack;
			final NavPoint last = stack.get(stack.size() - 1);
			last.Text += new String(ch, start, length);
		}
	}

	@Override
	public boolean dontCacheAttributeValues() {
		return true;
	}
}

package org.geometerplus.fbreader.formats.daisy3;

import org.geometerplus.fbreader.bookmodel.BookReader;
import org.geometerplus.fbreader.bookmodel.FBTextKind;
import org.geometerplus.zlibrary.core.xml.ZLStringMap;

/**
 * Handle <a> tags.
 * @author meghan larson
 *
 */
public class Daisy3XMLTagHyperlinkAction extends Daisy3XMLTagAction {
	
	
	/**
	 * Default constructor.
	 */
	public Daisy3XMLTagHyperlinkAction() {
		//empty.
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doAtStart(Daisy3XMLReader reader, ZLStringMap xmlattributes) {
		final BookReader modelReader = reader.getModelReader();
		final String href = xmlattributes.getValue("href");
		
		if ((href != null) && (href.length() > 0)) {
			String link = href;
			final byte hyperlinkType;
			if (isReference(link)) {
				hyperlinkType = FBTextKind.EXTERNAL_HYPERLINK;
			} else {
				hyperlinkType = FBTextKind.INTERNAL_HYPERLINK;
				final int index = href.indexOf('#');
				if (index == 0) {
					link = reader.myReferencePrefix + href.substring(1);
				} else if (index > 0) {
					link = reader.getFileAlias(reader.myLocalPathPrefix + href.substring(0, index)) + href.substring(index);
				} else {
					link = reader.getFileAlias(reader.myLocalPathPrefix + href);
				}
			}
			modelReader.pushKind(hyperlinkType);
			modelReader.addHyperlinkControl(hyperlinkType, link);
		} else {
			modelReader.pushKind(FBTextKind.REGULAR);
		}
		final String name = xmlattributes.getValue("name");
		if (name != null) {
			modelReader.addHyperlinkLabel(reader.myReferencePrefix + name);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doAtEnd(Daisy3XMLReader reader) {
		reader.getModelReader().popKind();
	}
	
	/**
	 * @param text not null
	 * @return true if ref.
	 */
	private static boolean isReference(String text) {
		return
			text.startsWith("fbreader-action://") ||
			text.startsWith("http://") ||
			text.startsWith("https://") ||
			text.startsWith("mailto:") ||
			text.startsWith("ftp://");
	}

}

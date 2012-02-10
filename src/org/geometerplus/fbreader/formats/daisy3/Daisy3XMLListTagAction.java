package org.geometerplus.fbreader.formats.daisy3;

import org.geometerplus.zlibrary.core.xml.ZLStringMap;

/**
 * Handle lists.
 * @author meghan larson
 *
 */
public class Daisy3XMLListTagAction extends Daisy3XMLTagAction {
	
	/**
	 * Default constructor.
	 */
	public Daisy3XMLListTagAction() {
		//empty
	}

	@Override
	protected void doAtStart(Daisy3XMLReader reader, ZLStringMap xmlattributes) {
		//For now, just start a new paragraph. 
		reader.getModelReader().beginParagraph();
		
	}

	@Override
	protected void doAtEnd(Daisy3XMLReader reader) {
		reader.getModelReader().endParagraph();
	}

}

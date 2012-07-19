package org.geometerplus.android.fbreader.subscription;

public class DbPeriodicalEntity {

	private int id;
	private String title;
	private String latestEdition;	//latest edition of the periodical we currently have
	private int latestRevision; //latest revision
	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getLatestEdition() {
		return latestEdition;
	}
	public void setLatestEdition(String latestEdition) {
		this.latestEdition = latestEdition;
	}
	public int getLatestRevision() {
		return latestRevision;
	}
	public void setLatestRevision(int latestRevision) {
		this.latestRevision = latestRevision;
	}
	
	
}

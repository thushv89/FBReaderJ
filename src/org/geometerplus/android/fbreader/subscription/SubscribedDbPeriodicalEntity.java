package org.geometerplus.android.fbreader.subscription;

public class SubscribedDbPeriodicalEntity extends PeriodicalEntity{

	private String latestEdition;	//latest edition of the periodical we currently have
	private int latestRevision; //latest revision
	
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

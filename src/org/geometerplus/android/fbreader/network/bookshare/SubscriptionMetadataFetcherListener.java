package org.geometerplus.android.fbreader.network.bookshare;

import java.util.ArrayList;

public interface SubscriptionMetadataFetcherListener {
	public void taskCompleted(ArrayList<Bookshare_Edition_Metadata_Bean> mBeans);
}

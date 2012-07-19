package org.geometerplus.android.fbreader.network.bookshare;

import java.util.ArrayList;

import org.geometerplus.android.fbreader.subscription.DbPeriodicalEntity;

public interface IDownloadAPI{

	public ArrayList<DbPeriodicalEntity> isNewDownloadsAvailable(DbPeriodicalEntity entity);
	public Bookshare_Periodical_Edition_Bean downloadPeriodical(DbPeriodicalEntity entity);
}

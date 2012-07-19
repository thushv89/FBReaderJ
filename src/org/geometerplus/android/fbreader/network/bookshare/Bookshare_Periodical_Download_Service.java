package org.geometerplus.android.fbreader.network.bookshare;

import java.util.ArrayList;

import org.geometerplus.android.fbreader.subscription.DbPeriodicalEntity;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class Bookshare_Periodical_Download_Service extends Service{

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public class DownloadAPIBinder extends Binder implements IDownloadAPI{

		@Override
		public ArrayList<DbPeriodicalEntity> isNewDownloadsAvailable(
				DbPeriodicalEntity entity) {
			String latestEdition = entity.getLatestEdition();
			int latestRevision = entity.getLatestRevision();
			return null;
		}

		@Override
		public Bookshare_Periodical_Edition_Bean downloadPeriodical(
				DbPeriodicalEntity entity) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}

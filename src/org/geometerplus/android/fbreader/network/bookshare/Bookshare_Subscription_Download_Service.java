package org.geometerplus.android.fbreader.network.bookshare;

import org.geometerplus.android.fbreader.subscription.BooksharePeriodicalDataSource;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class Bookshare_Subscription_Download_Service extends Service{

	//SubscriptionSQLiteHelper dbHelper;
	BooksharePeriodicalDataSource dataSrc;
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		//dbHelper=new SubscriptionSQLiteHelper(getApplicationContext());
		dataSrc=new BooksharePeriodicalDataSource(getApplicationContext());
		
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
	}

	
}

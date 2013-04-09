package org.geometerplus.android.fbreader.network.bookshare.subscription;

import java.util.ArrayList;
import java.util.Vector;

import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.android.fbreader.network.bookshare.BookshareDeveloperKey;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Edition_Metadata_Bean;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Periodical_Edition_Bean;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Webservice_Login;
import org.geometerplus.fbreader.fbreader.FBReaderApp.AutomaticDownloadType;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class TestIntentService extends IntentService implements PeriodicalEditionListener,PeriodicalMetadataListener{

	private String username;
	private String password;

	private SQLiteDatabase periodicalDb;
	BooksharePeriodicalDataSource dataSource;
	PeriodicalsSQLiteHelper dbHelper;
	
	private String omDownloadPassword;
	private boolean isFree = false;
	private boolean isOM;
	private String developerKey = BookshareDeveloperKey.DEVELOPER_KEY;

	private String subscribedIDStr; 
	private int subscribedIDCount=0;
	private AutomaticDownloadType downType = AutomaticDownloadType.downloadMostRecent;
	
	public static String DOWNLOAD_PERIODICAL_ID_KEY = "download_periodical_id";
	
	private String usernameKey = "username";
	private String passwordKey = "password";
	
	public TestIntentService() {
		super("TestIntentService");
		
	}

	
	@Override
	protected void onHandleIntent(Intent intent) {
		SharedPreferences logingPrefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		username = logingPrefs.getString(usernameKey, "");
		password = logingPrefs.getString(passwordKey, "");

		dataSource = BooksharePeriodicalDataSource
				.getInstance(getApplicationContext());
		dbHelper = new PeriodicalsSQLiteHelper(getApplicationContext());

		// instantiate periodical db only if its null or is not opened currently
		if (periodicalDb == null) {
			periodicalDb = dbHelper.getWritableDatabase();
		} else {
			if (!periodicalDb.isOpen()) {
				periodicalDb = dbHelper.getWritableDatabase();
			}
		}

		if (username == null || password == null || TextUtils.isEmpty(username)) {
			isFree = true;
		}

		subscribedIDStr = (String) intent
				.getStringExtra(DOWNLOAD_PERIODICAL_ID_KEY);

		String downTypeStr = intent
				.getStringExtra(FBReader.AUTOMATIC_DOWNLOAD_TYPE_KEY);

		// Determine the download type user has set
		if (AutomaticDownloadType.downloadAll.toString().equals(downTypeStr)) {
			downType = AutomaticDownloadType.downloadAll;
		} else {
			downType = AutomaticDownloadType.downloadMostRecent;
		}

		getUpdates(downType, subscribedIDStr);
		
	}
	
	@Override
	public void onDestroy() {
		periodicalDb.close();
		super.onDestroy();
	}

	private synchronized void getUpdates(AutomaticDownloadType downType, String id) {

		final String serviceURI = Bookshare_Webservice_Login.BOOKSHARE_API_PROTOCOL
				+ Bookshare_Webservice_Login.BOOKSHARE_API_HOST
				+ "/periodical/id/"
				+ id
				+ "/for/"
				+ username
				+ "?api_key="
				+ developerKey;
		Log.i(getClass().getName(),
				"Fetching Periodical List for periodical with id: " + id
				+ " for: " + username);


		PeriodicalEditionListFetcher editionFetcher = new PeriodicalEditionListFetcher();
		//editionFetcher.getListing(serviceURI, password, this);
		
	}

	
	@Override
	public void onPeriodicalEditionListResponse(
			Vector<Bookshare_Periodical_Edition_Bean> results) {
		if (results == null) {
			Log.e(getClass().getName(),
					"Couldn't fetch any periodical Editions");
		} else {
			//SystemClock.sleep(1000);
			Log.i(getClass().getName(),
					"Found and Fetched " + results.size() + " periodicals");
			ArrayList<AllDbPeriodicalEntity> entities = new ArrayList<AllDbPeriodicalEntity>();
			for (Bookshare_Periodical_Edition_Bean bean : results) {
				Log.i(getClass().getName(),
						"Found and fetched periodical: Title "
								+ bean.getTitle() + " Edition: "
								+ bean.getEdition());
				//bean exists 
				if (bean.getId() != null && bean.getEdition() != null
						&& TextUtils.isDigitsOnly(bean.getRevision())) {

					entities.add(new AllDbPeriodicalEntity(bean.getId(),
							bean.getTitle(), bean.getEdition(), Integer
							.parseInt(bean.getRevision()), null,
							null));
				}
			}
			fetchMetadataUpdates(entities);
		}
	}

	private void fetchMetadataUpdates(
			ArrayList<AllDbPeriodicalEntity> entities) {
		PeriodicalEditionMetadataFetcher metadataFetcher;
		// URL to request metadata of particular edition of a periodical
		String serviceURI;

		


			// if user has set settings to download only the most recent
			if (downType == AutomaticDownloadType.downloadMostRecent) {

				final AllDbPeriodicalEntity maxEntity = PeriodicalDBUtils
						.getMostRecentEdition(entities);

				// download the periodical only if it's not been downloaded
				// before
				if (!dataSource.doesExist(periodicalDb,
						PeriodicalsSQLiteHelper.TABLE_ALL_PERIODICALS,
						maxEntity)) {
					serviceURI = getEditionRequestURL(maxEntity);

					metadataFetcher = new PeriodicalEditionMetadataFetcher(
							maxEntity.getId(), maxEntity.getTitle());

					metadataFetcher.getListing(serviceURI, password, this);						    	
				}else{
					subscribedIDCount++;
				}

			}
			// If user has set settings to download all periodicals
			else if (downType == AutomaticDownloadType.downloadAll) {

				for (AllDbPeriodicalEntity entity : entities) {
					// download the periodical only if it's not been
					// downloaded before
					// TODO: Download periodicals which is higer than the
					// highest in alldbperiodicals
					if (!dataSource.doesExist(periodicalDb,
							PeriodicalsSQLiteHelper.TABLE_ALL_PERIODICALS,
							entity)) {
						serviceURI = getEditionRequestURL(entity);

						metadataFetcher = new PeriodicalEditionMetadataFetcher(
								entity.getId(), entity.getTitle());
						metadataFetcher.getListing(serviceURI, password,
								this);

						// This is to reduce number of queries per second							
						SystemClock.sleep(1000);

					}
				}
			}		
	}


	private String getEditionRequestURL(AllDbPeriodicalEntity entity) {
		String serviceURI = Bookshare_Webservice_Login.BOOKSHARE_API_PROTOCOL
				+ Bookshare_Webservice_Login.BOOKSHARE_API_HOST
				+ "/periodical/id/"
				+ entity.getId()
				+ "/edition/"
				+ entity.getEdition()
				+ "/revision/"
				+ entity.getRevision()
				+ "/for/" + username + "?api_key=" + developerKey;
		return serviceURI;
	}


	@Override
	public void onPeriodicalMetadataResponse(
			Bookshare_Edition_Metadata_Bean result) {
		if (result == null) {
			Log.e(getClass().getName(), "Couldn't obtain edition details");
		} else {
			addToDownloadList(result);
			Log.i(getClass().getName(),
					"Metadata Responce Fetched Periodical: " + result.getPeriodicalId() + " "
							+ result.getTitle() + " " + result.getEdition());
		}
	}

	public boolean addToDownloadList(Bookshare_Edition_Metadata_Bean bean) {
		
			//mBeansToDownload.add(bean);	
			Log.i(getClass().getName(), "Sent extras to Download Queue: "+bean.getTitle());		

			subscribedIDCount++;	
			
		
		//if(subscribedIDCount == subscribedIDStr.size()){
			//runDownloadQueue();
		//}
		return false;
	}

	/*
	private void runDownloadQueue() {
		Intent downloadService = new Intent(
				Bookshare_Subscription_Download_Service.this,
				SubscriptionDownloadService.class);
		downloadService.putExtra(usernameKey, username);
		downloadService.putExtra(passwordKey, password);
		downloadService.putExtra("metadata_beans_to_download",(ArrayList<Bookshare_Edition_Metadata_Bean>) mBeansToDownload);
		startService(downloadService);
	}*/


}

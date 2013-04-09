package org.geometerplus.android.fbreader.network.bookshare.subscription;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.android.fbreader.network.bookshare.BookshareDeveloperKey;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Edition_Metadata_Bean;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Periodical_Edition_Bean;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Webservice_Login;
import org.geometerplus.android.fbreader.network.bookshare.SubscriptionMetadataFetcherListener;
import org.geometerplus.fbreader.fbreader.FBReaderApp.AutomaticDownloadType;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

/**
 * Main automatic dowload service
 * 
 * @author thushan
 * 
 */
public class Bookshare_Subscription_Download_Service extends Service implements
SubscriptionMetadataFetcherListener,PeriodicalEditionListener,PeriodicalMetadataListener{

	// SubscriptionSQLiteHelper dbHelper;
	BooksharePeriodicalDataSource dataSource;
	PeriodicalsSQLiteHelper dbHelper;
	private String username;
	private String password;

	private String omDownloadPassword;
	private boolean isFree = false;
	private boolean isOM;
	private String developerKey = BookshareDeveloperKey.DEVELOPER_KEY;

	//private Bookshare_Edition_Metadata_Bean metadata_bean;
	private Set<Integer> myOngoingNotifications = new HashSet<Integer>();
	private SQLiteDatabase periodicalDb;
	//private PeriodicalEditionListFetcher editionFetcher;

	private ArrayList<String> subscribedIDStr; 
	private int subscribedIDCount=0;

	private ArrayList<Bookshare_Edition_Metadata_Bean> mBeansToDownload = new ArrayList<Bookshare_Edition_Metadata_Bean>();
	private String usernameKey = "username";
	private String passwordKey = "password";

	private Object lock = new Object();
	private boolean flag = false;
	
	private AutomaticDownloadType downType = AutomaticDownloadType.downloadMostRecent;

	@Override
	public IBinder onBind(Intent arg0) { 
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		periodicalDb.close();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(getClass().getName(),
				"**** Service Started by Alarm Manager ****");

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

		subscribedIDStr = (ArrayList<String>) intent
				.getStringArrayListExtra(FBReader.SUBSCRIBED_PERIODICAL_IDS_KEY);

		String downTypeStr = intent
				.getStringExtra(FBReader.AUTOMATIC_DOWNLOAD_TYPE_KEY);

		Log.i(getClass().getName(), "Extras passed: id array size" + subscribedIDStr.size()
				+ " , " + downTypeStr);
		// Determine the download type user has set
		if (AutomaticDownloadType.downloadAll.toString().equals(downTypeStr)) {
			downType = AutomaticDownloadType.downloadAll;
		} else {
			downType = AutomaticDownloadType.downloadMostRecent;
		}

		
		
		//runGetupdateForNextSubscribedPeriodical();
		for(final String id : subscribedIDStr){
			if(subscribedIDStr != null && subscribedIDStr.size()>0){
				Intent testService = new Intent(
						Bookshare_Subscription_Download_Service.this,
						TestIntentService.class);
				testService.putExtra(usernameKey, username);
				testService.putExtra(passwordKey, password);
				testService.putExtra(TestIntentService.DOWNLOAD_PERIODICAL_ID_KEY, id);
				testService.putExtra(FBReader.AUTOMATIC_DOWNLOAD_TYPE_KEY, downTypeStr);
				//testService.putExtra("metadata_beans_to_download",(ArrayList<Bookshare_Edition_Metadata_Bean>) mBeansToDownload);
				startService(testService);

			}
			else{
				break;
			}
		}

		return super.onStartCommand(intent, flags, startId);
	}
	
	/*
	private class MetadataFetchTask extends AsyncTask<String, Void, Void> 
	implements PeriodicalEditionListener,PeriodicalMetadataListener{

		

		@Override
		protected Void doInBackground(String... ids) {
			getUpdates(downType, ids[0]);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			Log.i(getClass().getName(), "Async Task finished - Metadata");
		};
		
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

	}*/

	private synchronized void getUpdates(AutomaticDownloadType downType, String id) {
		flag = true;
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
		
			mBeansToDownload.add(bean);	
			Log.i(getClass().getName(), "Sent extras to Download Queue: "+bean.getTitle());		

			subscribedIDCount++;	
			
		
		if(subscribedIDCount == subscribedIDStr.size()){
			runDownloadQueue();
		}
		return false;
	}

	private void runDownloadQueue() {
		Intent downloadService = new Intent(
				Bookshare_Subscription_Download_Service.this,
				SubscriptionDownloadService.class);
		downloadService.putExtra(usernameKey, username);
		downloadService.putExtra(passwordKey, password);
		downloadService.putExtra("metadata_beans_to_download",(ArrayList<Bookshare_Edition_Metadata_Bean>) mBeansToDownload);
		startService(downloadService);
	}

	@Override
	public void taskCompleted(ArrayList<Bookshare_Edition_Metadata_Bean> mBeans) {
		// TODO Auto-generated method stub
		
	}



}

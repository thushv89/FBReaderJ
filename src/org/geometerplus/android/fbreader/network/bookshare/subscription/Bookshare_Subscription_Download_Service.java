package org.geometerplus.android.fbreader.network.bookshare.subscription;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.benetech.android.R;
import org.bookshare.net.BookshareWebservice;
import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.android.fbreader.network.BookDownloaderService;
import org.geometerplus.android.fbreader.network.bookshare.BookshareDeveloperKey;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Edition_Metadata_Bean;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Error_Bean;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Periodical_Edition_Bean;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Webservice_Login;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.fbreader.FBReaderApp.AutomaticDownloadType;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Main automatic dowload service
 * 
 * @author thushan
 * 
 */
public class Bookshare_Subscription_Download_Service extends Service {

	// SubscriptionSQLiteHelper dbHelper;
	BooksharePeriodicalDataSource dataSource;
	PeriodicalsSQLiteHelper dbHelper;
	private String username;
	private String password;
	private String downloadedBookDir;
	private String omDownloadPassword;
	private boolean isFree = false;
	private boolean isOM;
	private String developerKey = BookshareDeveloperKey.DEVELOPER_KEY;

	private Bookshare_Edition_Metadata_Bean metadata_bean;
	private Set<Integer> myOngoingNotifications = new HashSet<Integer>();
	private SQLiteDatabase periodicalDb;
	private PeriodicalEditionListFetcher editionFetcher;
	private PeriodicalEditionMetadataFetcher metadataFetcher;
	private ServiceBinder serviceBinder = new ServiceBinder();

	
	
	private String usernameKey = "username";
	private String passwordKey = "password";

	@Override
	public IBinder onBind(Intent arg0) {

		
		
		SharedPreferences logingPrefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		username = logingPrefs.getString(usernameKey, "");
		password = logingPrefs.getString(passwordKey, "");

		// TODO: Get first name, last name, member id
		dataSource = BooksharePeriodicalDataSource
				.getInstance(getApplicationContext());
		dbHelper = new PeriodicalsSQLiteHelper(getApplicationContext());
		periodicalDb = dbHelper.getWritableDatabase();

		editionFetcher = new PeriodicalEditionListFetcher();

		if (username == null || password == null || TextUtils.isEmpty(username)) {
			isFree = true;
		}

		return serviceBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		stopSelf();
		periodicalDb.close();
		return super.onUnbind(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// dbHelper=new SubscriptionSQLiteHelper(getApplicationContext());
		// dataSrc=new BooksharePeriodicalDataSource(getApplicationContext());

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

		if (serviceBinder == null) {
			serviceBinder = new ServiceBinder();
		}

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

		ArrayList<String> ids = (ArrayList<String>) intent
				.getStringArrayListExtra(FBReader.SUBSCRIBED_PERIODICAL_IDS_KEY);

		String downTypeStr = intent
				.getStringExtra(FBReader.AUTOMATIC_DOWNLOAD_TYPE_KEY);
		AutomaticDownloadType downType;
		Log.i(getClass().getName(), "Extras passed: id array size" + ids.size()
				+ " , " + downTypeStr);
		// Determine the download type user has set
		if (AutomaticDownloadType.downloadAll.toString().equals(downTypeStr)) {
			downType = AutomaticDownloadType.downloadAll;
		} else {
			downType = AutomaticDownloadType.downloadMostRecent;
		}

		if (ids != null && ids.size() > 0) {
			for (String id : ids) {
				Log.i(getClass().getName(),
						"Periodical search started by alarm: " + id);
				serviceBinder.getUpdates(downType, id);

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					Log.e(getClass().getName(),
							e.getCause() + " ," + e.getMessage());
				}
			}
		} else {
			Log.e(getClass().getName(),
					"Couldn't find any subscribed Periodicals");
		}
		return super.onStartCommand(intent, flags, startId);

	}

	public class ServiceBinder extends Binder implements
			IPeriodicalDownloadAPI, PeriodicalEditionListener,
			PeriodicalMetadataListener {
		AutomaticDownloadType downType;

		@Override
		public boolean downloadPeriodical(Bookshare_Edition_Metadata_Bean bean) {
			metadata_bean = bean;

			Intent downloadService = new Intent(
					Bookshare_Subscription_Download_Service.this,
					SubscriptionDownloadService.class);
			downloadService.putExtra(usernameKey, username);
			downloadService.putExtra(passwordKey, password);
			downloadService.putExtra("metadata_bean", metadata_bean);
			startService(downloadService);
			// new DownloadFilesTask().execute();
			return false;
		}

		@Override
		public void getUpdates(AutomaticDownloadType downType, String id) {

			this.downType = downType;
			String serviceURI = Bookshare_Webservice_Login.BOOKSHARE_API_PROTOCOL
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

			editionFetcher = new PeriodicalEditionListFetcher();
			editionFetcher.getListing(serviceURI, password, this);

		}

		@Override
		public void onPeriodicalEditionListResponse(
				Vector<Bookshare_Periodical_Edition_Bean> results) {
			if (results == null) {
				Log.e(getClass().getName(),
						"Couldn't fetch any periodical Editions");
			} else {
				Log.i(getClass().getName(),
						"Found and Fetched " + results.size() + " periodicals");
				ArrayList<AllDbPeriodicalEntity> entities = new ArrayList<AllDbPeriodicalEntity>();
				for (Bookshare_Periodical_Edition_Bean bean : results) {
					Log.i(getClass().getName(),
							"Found and fetched periodical: Title "
									+ bean.getTitle() + " Edition: "
									+ bean.getEdition());
					if (bean.getId() != null && bean.getEdition() != null
							&& TextUtils.isDigitsOnly(bean.getRevision())) {
						entities.add(new AllDbPeriodicalEntity(bean.getId(),
								bean.getTitle(), bean.getEdition(), Integer
										.parseInt(bean.getRevision()), null,
								null));
					}
				}

				// URL to request metadata of particular edition of a periodical
				String serviceURI;
				// if user has set settings to download only the most recent
				if (downType == AutomaticDownloadType.downloadMostRecent) {
					AllDbPeriodicalEntity maxEntity = PeriodicalDBUtils
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
							try {
								Thread.sleep(1000);
							} catch (InterruptedException ex) {
								Log.e(getClass().getName(), ex.getCause()
										+ " ," + ex.getMessage());
							}
						}
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
		public Bookshare_Edition_Metadata_Bean getDetails(
				Bookshare_Periodical_Edition_Bean bean) {
			if (bean == null) {
				Log.e(getClass().getName(), "Couldn't obtain edition details");
			} else {
				Log.i(getClass().getName(),
						"Fetched Periodical: " + bean.getId() + " "
								+ bean.getTitle() + " " + bean.getEdition());
			}
			return null;
		}

		@Override
		public void onPeriodicalMetadataResponse(
				Bookshare_Edition_Metadata_Bean result) {
			if (result == null) {
				Log.e(getClass().getName(), "Couldn't obtain edition details");
			} else {
				downloadPeriodical(result);
				Log.i(getClass().getName(),
						"Fetched Periodical: " + result.getPeriodicalId() + " "
								+ result.getTitle() + " " + result.getEdition());
			}

		}

	}


	private Intent getFBReaderIntent(final File file) {
		final Intent intent = new Intent(getApplicationContext(),
				FBReader.class);
		if (file != null) {
			intent.setAction(Intent.ACTION_VIEW).setData(Uri.fromFile(file));
		}
		return intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_NEW_TASK);
	}

	

}

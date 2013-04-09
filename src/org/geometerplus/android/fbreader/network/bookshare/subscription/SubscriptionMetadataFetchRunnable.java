package org.geometerplus.android.fbreader.network.bookshare.subscription;

import java.util.ArrayList;
import java.util.Vector;

import org.geometerplus.android.fbreader.network.bookshare.BookshareDeveloperKey;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Edition_Metadata_Bean;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Periodical_Edition_Bean;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Webservice_Login;
import org.geometerplus.android.fbreader.network.bookshare.SubscriptionMetadataFetcherListener;
import org.geometerplus.fbreader.fbreader.FBReaderApp.AutomaticDownloadType;

import android.database.sqlite.SQLiteDatabase;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

public class SubscriptionMetadataFetchRunnable implements Runnable,PeriodicalEditionListener,PeriodicalMetadataListener, Callback {

	private String username;
	private String password;
	private String developerKey=BookshareDeveloperKey.DEVELOPER_KEY;
	private String id;
	private AutomaticDownloadType downType;
	private SQLiteDatabase periodicalDb;
	private BooksharePeriodicalDataSource dataSource;
	private SubscriptionMetadataFetcherListener listener;
	private ArrayList<Bookshare_Edition_Metadata_Bean> mBeanResults = new ArrayList<Bookshare_Edition_Metadata_Bean>();
	private int numOfItemsToDownload =0;
	
	
	public SubscriptionMetadataFetchRunnable(String id, AutomaticDownloadType type, SubscriptionMetadataFetcherListener listener){
		this.id = id;
		this.downType = type;
		this.listener = listener;
	}
	
	@Override
	public void run() {
		getUpdates(downType, id);
	}

	private void getUpdates(AutomaticDownloadType downType, String id) {
		//isNotFinished = true;
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
		editionFetcher.getListing(serviceURI, password, this);
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
			numOfItemsToDownload=0;
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
				numOfItemsToDownload = 1;
			}

		}
		// If user has set settings to download all periodicals
		else if (downType == AutomaticDownloadType.downloadAll) {
			numOfItemsToDownload=0;
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
					numOfItemsToDownload++;
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
			mBeanResults.add(result);
			Log.i(getClass().getName(),
					"Metadata Responce Fetched Periodical: " + result.getPeriodicalId() + " "
							+ result.getTitle() + " " + result.getEdition());
		}

		//check if all the results has come
		if(mBeanResults.size() == numOfItemsToDownload){
			listener.taskCompleted(mBeanResults);
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public SQLiteDatabase getPeriodicalDb() {
		return periodicalDb;
	}

	public void setPeriodicalDb(SQLiteDatabase periodicalDb) {
		this.periodicalDb = periodicalDb;
	}

	public BooksharePeriodicalDataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(BooksharePeriodicalDataSource dataSource) {
		this.dataSource = dataSource;
	}

	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public AutomaticDownloadType getDownType() {
		return downType;
	}

	public void setDownType(AutomaticDownloadType downType) {
		this.downType = downType;
	}

	public SubscriptionMetadataFetcherListener getListener() {
		return listener;
	}

	public void setListener(SubscriptionMetadataFetcherListener listener) {
		this.listener = listener;
	}

	@Override
	public boolean handleMessage(Message arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}

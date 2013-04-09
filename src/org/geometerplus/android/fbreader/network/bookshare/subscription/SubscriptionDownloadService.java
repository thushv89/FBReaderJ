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
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Webservice_Login;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * This is an intent service. This was used to stop simultaneous downloas as it
 * causes system not to respond as well as corrupt zip files. So with this
 * downloads are done sequentially
 * 
 * @author thushan
 * 
 */
public class SubscriptionDownloadService extends IntentService {

	BooksharePeriodicalDataSource dataSource;
	PeriodicalsSQLiteHelper dbHelper;
	private String username;
	private String password;

	private String omDownloadPassword;
	private boolean isFree = false;
	private boolean isOM;


	private String developerKey = BookshareDeveloperKey.DEVELOPER_KEY;


	private Set<Integer> myOngoingNotifications = new HashSet<Integer>();
	private SQLiteDatabase periodicalDb;

	private Bookshare_Error_Bean error;
	final BookshareWebservice bws = new BookshareWebservice(
			Bookshare_Webservice_Login.BOOKSHARE_API_HOST);

	private boolean downloadingCurrFinished=true;
	
	public SubscriptionDownloadService() {
		super("SubscriptionDownloadService");

	}
	

	@Override
	protected void onHandleIntent(Intent intent) {
		ArrayList<Bookshare_Edition_Metadata_Bean> mBeansToDownload=new ArrayList<Bookshare_Edition_Metadata_Bean>();
		boolean downloadSuccess = false;
		String downloadedBookDir = null;

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

		username = intent.getStringExtra("username");
		password = intent.getStringExtra("password");
		
		if (username == null || password == null || TextUtils.isEmpty(username)) {
			isFree = true;
		}

		mBeansToDownload = (ArrayList<Bookshare_Edition_Metadata_Bean>) intent
				.getSerializableExtra("metadata_beans_to_download");
		
		for(Bookshare_Edition_Metadata_Bean metadata_bean : mBeansToDownload){
			//hold on till the current download finishes
			
			Log.i(getClass().getName(),
					"Downloading one by one... " + "Title: "
							+ metadata_bean.getTitle() + " Edition: "
							+ metadata_bean.getEdition());
			
			// From here onwards till method's end it's similar to
			// DownloadFilesTask() Task
			final String id = metadata_bean.getContentId();
			String download_uri;
			if (isFree)
				download_uri = Bookshare_Webservice_Login.BOOKSHARE_API_PROTOCOL
				+ Bookshare_Webservice_Login.BOOKSHARE_API_HOST
				+ "/download/content/" + id + "/version/1?api_key="
				+ developerKey;
			// TODO: Uncomment & Implement
			/*
			 * else if(isOM){
			 * 
			 * download_uri = Bookshare_Webservice_Login.BOOKSHARE_API_PROTOCOL +
			 * Bookshare_Webservice_Login.BOOKSHARE_API_HOST + "/download/member/"
			 * +memberId+"content/"+id+"/version/1/for/"+username
			 * +"?api_key="+developerKey; }
			 */

			else {
				download_uri = Bookshare_Webservice_Login.BOOKSHARE_API_PROTOCOL
						+ Bookshare_Webservice_Login.BOOKSHARE_API_HOST
						+ "/download/content/" + id + "/version/1/for/" + username
						+ "?api_key=" + developerKey;
			}

			final Notification progressNotification = createDownloadProgressNotification(metadata_bean
					.getTitle());

			final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			myOngoingNotifications.add(Integer.valueOf(id));
			notificationManager.notify(Integer.valueOf(id), progressNotification);

			try {
				System.out.println("download_uri :" + download_uri);
				HttpResponse response = bws.getHttpResponse(password, download_uri);
				// Get hold of the response entity
				HttpEntity entity = response.getEntity();

				if (entity != null) {
					String filename =null;
					if (metadata_bean.getTitle() != null
							&& metadata_bean.getEdition() != null) {
						String temp = "";
						// Changed the file name to <title>_<edition>
						temp = metadata_bean.getTitle() + "_"
								+ metadata_bean.getEdition();

						filename = temp;
						filename = filename.replaceAll(" +", "_").replaceAll(":",
								"__");
						if (isOM) {
							// TODO: Uncomment & Implement
							// filename = filename + "_" + firstName + "_" +
							// lastName;
						}
					}
					String zip_file = Paths.BooksDirectoryOption().getValue() + "/"
							+ filename + ".zip";
					downloadedBookDir = Paths.BooksDirectoryOption().getValue()
							+ "/" + filename;

					File downloaded_zip_file = new File(zip_file);
					if (downloaded_zip_file.exists()) {
						downloaded_zip_file.delete();
					}
					Header header = entity.getContentType();
					// Log.w("FBR", "******  zip_file *****" + zip_file);
					final String headerValue = header.getValue();
					if (headerValue.contains("zip") || headerValue.contains("bks2")) {
						try {
							System.out.println("Contains zip");
							java.io.BufferedInputStream in = new java.io.BufferedInputStream(
									entity.getContent());
							java.io.FileOutputStream fos = new java.io.FileOutputStream(
									downloaded_zip_file);
							java.io.BufferedOutputStream bout = new BufferedOutputStream(
									fos, 1024);
							byte[] data = new byte[1024];
							int x = 0;
							while ((x = in.read(data, 0, 1024)) >= 0) {
								bout.write(data, 0, x);
							}
							fos.flush();
							bout.flush();
							fos.close();
							bout.close();
							in.close();

							System.out.println("******** Downloading complete");

							File file = new File(downloadedBookDir);
							if(!file.exists()){
								file.mkdir();
							}
							// Unzip the encrypted archive file
							if (!isFree) {
								System.out
								.println("******Before creating ZipFile******"
										+ zip_file);
								// Initiate ZipFile object with the path/name of
								// the zip file.
								ZipFile zipFile = new ZipFile(zip_file);

								// Check to see if the zip file is password
								// protected
								if (zipFile.isEncrypted()) {
									System.out.println("******isEncrypted******");

									// if yes, then set the password for the zip
									// file
									if (!isOM) {
										zipFile.setPassword(password);
									}
									// Set the OM password sent by the Intent
									else {
										// Obtain the SharedPreferences object
										// shared across the application. It is
										// stored in login activity
										SharedPreferences login_preference = PreferenceManager
												.getDefaultSharedPreferences(getApplicationContext());
										omDownloadPassword = login_preference
												.getString("downloadPassword", "");
										zipFile.setPassword(omDownloadPassword);
									}
								}

								// Get the list of file headers from the zip
								// file
								List fileHeaderList = zipFile.getFileHeaders();

								System.out.println("******Before for******");
								// Loop through the file headers
								for (int i = 0; i < fileHeaderList.size(); i++) {
									FileHeader fileHeader = (FileHeader) fileHeaderList
											.get(i);
									System.out.println(downloadedBookDir);
									// Extract the file to the specified
									// destination
									zipFile.extractFile(fileHeader,
											downloadedBookDir);
								}
							}
							// Unzip the non-encrypted archive file
							else {
								try {

									String destinationname = downloadedBookDir
											+ "/";
									byte[] buf = new byte[1024];
									ZipInputStream zipinputstream = null;
									ZipEntry zipentry;
									zipinputstream = new ZipInputStream(
											new FileInputStream(zip_file));

									zipentry = zipinputstream.getNextEntry();
									while (zipentry != null) {
										// for each entry to be extracted
										String entryName = zipentry.getName();
										System.out
										.println("entryname " + entryName);
										int n;
										FileOutputStream fileoutputstream;
										File newFile = new File(entryName);
										String directory = newFile.getParent();

										if (directory == null) {
											if (newFile.isDirectory())
												break;
										}

										fileoutputstream = new FileOutputStream(
												destinationname + entryName);

										while ((n = zipinputstream.read(buf, 0,
												1024)) > -1)
											fileoutputstream.write(buf, 0, n);

										fileoutputstream.close();
										zipinputstream.closeEntry();
										zipentry = zipinputstream.getNextEntry();

									}// while

									zipinputstream.close();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							// Delete the downloaded zip file as it has been
							// extracted
							downloaded_zip_file = new File(zip_file);
							if (downloaded_zip_file.exists()) {
								downloaded_zip_file.delete();
							}
							downloadSuccess = true;
						} catch (ZipException e) {
							Log.e("FBR", "Zip Exception", e);
						}
					} else {
						downloadSuccess = false;
						error = new Bookshare_Error_Bean();
						error.parseInputStream(response.getEntity().getContent());
					}
				}
			} catch (URISyntaxException use) {
				System.out.println("URISyntaxException: " + use);
			} catch (IOException ie) {
				System.out.println("IOException: " + ie);
			}

			if (downloadSuccess) {
				// Get download time/date
				Calendar currCal = Calendar.getInstance();
				String currentDate = currCal.get(Calendar.MONTH) + "/"
						+ currCal.get(Calendar.DATE) + "/"
						+ currCal.get(Calendar.YEAR) + "";
				String currentTime = currCal.get(Calendar.HOUR_OF_DAY) + ":"
						+ currCal.get(Calendar.MINUTE) + ":"
						+ currCal.get(Calendar.SECOND) + "";

				// create alldb and subscribeddb entities to be inserted to
				// their respective dbs
				AllDbPeriodicalEntity allEntity = new AllDbPeriodicalEntity(
						metadata_bean.getPeriodicalId(), metadata_bean.getTitle(),
						metadata_bean.getEdition(), Integer.parseInt(metadata_bean
								.getRevision()), currentDate, currentTime);
				SubscribedDbPeriodicalEntity subEntity = new SubscribedDbPeriodicalEntity(
						metadata_bean.getPeriodicalId(), metadata_bean.getTitle(),
						metadata_bean.getEdition(), Integer.parseInt(metadata_bean
								.getRevision()));
				dataSource.insertEntity(periodicalDb,
						PeriodicalsSQLiteHelper.TABLE_ALL_PERIODICALS, allEntity);
				if (dataSource.doesExist(periodicalDb,
						PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS,
						subEntity)) {
					SubscribedDbPeriodicalEntity dbSubEntity = (SubscribedDbPeriodicalEntity) dataSource
							.getEntity(
									periodicalDb,
									PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS,
									subEntity);
					// Check whether we're actually trying to put a greater version
					// than existing version
					if (subEntity.getLatestEdition().equals(
							PeriodicalDBUtils.getRecentEditionString(
									subEntity.getLatestEdition(),
									dbSubEntity.getLatestEdition()))) {
						dataSource
						.insertEntity(
								periodicalDb,
								PeriodicalsSQLiteHelper.TABLE_SUBSCRIBED_PERIODICALS,
								subEntity);
					}
				}
			} else {
				Log.e(getClass().getName(), "******** Download failed ******** :(");
				//downloadedBookDir = null;
			}

			final Handler downloadFinishHandler = new Handler() {
				public void handleMessage(Message message) {
					final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

					DownloadFinishHandlerObject obj = (DownloadFinishHandlerObject) message.obj;
					int id = Integer.valueOf(obj.getmBean().getContentId());
					notificationManager.cancel(id);
					myOngoingNotifications.remove(Integer.valueOf(id));
					File file = null;

					if (message.arg1==1) {
						file = new File(getOpfFile(obj.getDownloadDir()).getPath());
					}
					notificationManager.notify(
							id,
							createDownloadFinishNotification(file,
									obj.getmBean().getTitle(), message.what != 0));
					downloadingCurrFinished=true;
				}
			};

			//downloadFinishHandler.sendEmptyMessage(downloadSuccess ? 1 : 0);
			Message msg = new Message();
			msg.arg1 = downloadSuccess ? 1 : 0;
			msg.obj = new DownloadFinishHandlerObject(metadata_bean, downloadedBookDir);
			Log.i(getClass().getName(), "Sending message Download success:"+downloadSuccess
					+" Periodical:"+metadata_bean.getTitle()
					+" Download Dir:"+downloadedBookDir);
			downloadFinishHandler.sendMessage(msg);

			
		}
				

	}	

	class DownloadFinishHandlerObject{
		Bookshare_Edition_Metadata_Bean mBean;
		String downloadDir;
		public DownloadFinishHandlerObject(Bookshare_Edition_Metadata_Bean mBean, String downloadDir) {
			this.mBean = mBean;
			this.downloadDir = downloadDir;
		}
		public Bookshare_Edition_Metadata_Bean getmBean() {
			return mBean;
		}
		public void setmBean(Bookshare_Edition_Metadata_Bean mBean) {
			this.mBean = mBean;
		}
		public String getDownloadDir() {
			return downloadDir;
		}
		public void setDownloadDir(String downloadDir) {
			this.downloadDir = downloadDir;
		}

	}
	private String getFileName(Bookshare_Edition_Metadata_Bean mBean){
		String filename;
		String temp = "";
		// Changed the file name to <title>_<edition>
		temp = mBean.getTitle() + "_"
				+ mBean.getEdition();

		filename = temp;
		filename = filename.replaceAll(" +", "_").replaceAll(":",
				"__");

		return filename;		
	}

	private String getDownloadDir(Bookshare_Edition_Metadata_Bean mBean){
		String filename = getFileName(mBean);
		String downloadedDir = Paths.BooksDirectoryOption().getValue()
				+ "/" + filename;
		return downloadedDir;		
	}

	private ZLFile getOpfFile(String downloadedBookDir) {
		ZLFile bookDir = ZLFile.createFileByPath(downloadedBookDir);
		List<ZLFile> bookEntries = bookDir.children();
		ZLFile opfFile = null;
		for (ZLFile entry : bookEntries) {
			if (entry.getExtension().equals("opf")) {
				opfFile = entry;
				break;
			}
		}
		return opfFile;
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

	@Override
	public void onDestroy() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
		periodicalDb.close();
		super.onDestroy();
	}

	private Notification createDownloadFinishNotification(File file,
			String title, boolean success) {
		final ZLResource resource = BookDownloaderService.getResource();
		final String tickerText = success ? resource.getResource(
				"tickerSuccess").getValue() : resource.getResource(
						"tickerError").getValue();
				final String contentText = success ? resource.getResource(
						"contentSuccess").getValue() : resource.getResource(
								"contentError").getValue();
						final Notification notification = new Notification(
								android.R.drawable.stat_sys_download_done, tickerText,
								System.currentTimeMillis());
						notification.flags |= Notification.FLAG_AUTO_CANCEL;
						final Intent intent = success ? getFBReaderIntent(file) : new Intent();
						final PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
								intent, 0);
						notification.setLatestEventInfo(getApplicationContext(), title,
								contentText, contentIntent);
						return notification;
	}

	private Notification createDownloadProgressNotification(String title) {
		final RemoteViews contentView = new RemoteViews(getPackageName(),
				R.layout.download_notification);
		contentView.setTextViewText(R.id.download_notification_title, title);
		contentView.setTextViewText(R.id.download_notification_progress_text,
				"");
		contentView.setProgressBar(R.id.download_notification_progress_bar,
				100, 0, true);

		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(), 0);

		final Notification notification = new Notification();
		notification.icon = android.R.drawable.stat_sys_download;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.contentView = contentView;
		notification.contentIntent = contentIntent;

		return notification;
	}

}

package org.geometerplus.android.fbreader.network.bookshare;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.app.Dialog;
import android.content.res.Resources;
import android.view.Window;
import android.view.WindowManager;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import org.accessibility.VoiceableDialog;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.bookshare.net.BookshareWebservice;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.BookTree;
import org.geometerplus.fbreader.library.Library;
import org.geometerplus.fbreader.library.LibraryTree;
import org.geometerplus.fbreader.tree.FBTree;
import org.benetech.android.R;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


/**
 * Shows the details of a selected book.
 * Will also show a download option if applicable.
 *
 */
public class Bookshare_Book_Details extends Activity{

	private String username;
	private String password;
	private Bookshare_Metadata_Bean metadata_bean;
	private InputStream inputStream;
	private BookshareWebservice bws = new BookshareWebservice();
	private final int DATA_FETCHED = 99;
	private ProgressDialog progessDialog;
	private View book_detail_view;
	private TextView bookshare_book_detail_title_text;
	private TextView bookshare_book_detail_authors_text;
	private TextView bookshare_book_detail_isbn_text;
	private TextView bookshare_book_detail_language_text;
	private TextView bookshare_book_detail_category_text;
	private TextView bookshare_book_detail_publish_date_text;
	private TextView bookshare_book_detail_publisher_text;
	private TextView bookshare_book_detail_copyright_text;
	private TextView bookshare_book_detail_synopsis_text;
	private TextView bookshare_download_not_available_text;
	private Button btn_download;
	boolean isDownloadable;
	private final int BOOKSHARE_BOOK_DETAILS_FINISHED = 1;
	private boolean isFree = false;
	private boolean isOM;
	private String developerKey = BookshareDeveloperKey.DEVELOPER_KEY;
	private final int START_BOOKSHARE_OM_LIST = 0;
	private String memberId = null;
	private String omDownloadPassword;
	private boolean downloadSuccess;
	private Book downloadedBook;
	private LibraryTree libraryTreeBeforeDownload;
	private Vector<Long> bookInstances;
    private Resources resources;

	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.bookshare_blank_page);
        resources = getApplicationContext().getResources();

		// Set full screen
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		Intent intent  = getIntent();
		username = intent.getStringExtra("username");
		password = intent.getStringExtra("password");
		
		if(username == null || password == null){
			isFree = true;
		}
		// Obtain the application wide SharedPreferences object and store the login information
		SharedPreferences login_preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		isOM = login_preference.getBoolean("isOM", false);
		
		final String uri = intent.getStringExtra("ID_SEARCH_URI");

		progessDialog = ProgressDialog.show(this, null, "Fetching book details. Please wait.", true);
		new Thread(){
			public void run(){
				try{
					inputStream = bws.getResponseStream(password, uri);
					Message msg = Message.obtain(handler);
					msg.what = DATA_FETCHED;
					msg.sendToTarget();
				}
				catch(IOException ioe){
					System.out.println(ioe);
				}
				catch(URISyntaxException use){
					System.out.println(use);
				}
			}
		}.start();
	}
	
	// Start downlading task if the OM download password has been received
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == START_BOOKSHARE_OM_LIST){
			if(data != null){
				memberId = data.getStringExtra("memberId");
				new DownloadFilesTask().execute();
			}
		}
	}

	// Handler for processing the returned stream from book detail search
	Handler handler = new Handler(){
		public void handleMessage(Message msg){
			if(msg.what==DATA_FETCHED){
				progessDialog.cancel();

				String response_HTML = bws.convertStreamToString(inputStream);
				String response = response_HTML.replace("&apos;", "\'").replace("&quot;", "\"").replace("&amp;", "and").replace("&#xd;\n", "\n").replace("&#x97;", "-");
				
				// Parse the response String
				parseResponse(response);
				String temp = "";

				if(metadata_bean == null){
					TextView txtView_msg = (TextView)findViewById(R.id.bookshare_blank_txtView_msg);
                    String noBookFoundMsg = "Book not found.";
					txtView_msg.setText(noBookFoundMsg);

                    View decorView = getWindow().getDecorView();
                    if (null != decorView) {
                        decorView.setContentDescription(noBookFoundMsg);
                    }
				}
				if(metadata_bean != null){
					setIsDownloadable(metadata_bean);
					setContentView(R.layout.bookshare_book_detail);
					book_detail_view = (View)findViewById(R.id.book_detail_view);
					bookshare_book_detail_title_text = (TextView)findViewById(R.id.bookshare_book_detail_title_text);
					bookshare_book_detail_authors_text = (TextView)findViewById(R.id.bookshare_book_detail_authors_text);
					bookshare_book_detail_isbn_text= (TextView)findViewById(R.id.bookshare_book_detail_isbn_text);
					bookshare_book_detail_language_text = (TextView)findViewById(R.id.bookshare_book_detail_language_text);
					bookshare_book_detail_category_text = (TextView)findViewById(R.id.bookshare_book_detail_category_text);
					bookshare_book_detail_publish_date_text = (TextView)findViewById(R.id.bookshare_book_detail_publish_date_text);
					bookshare_book_detail_publisher_text = (TextView)findViewById(R.id.bookshare_book_detail_publisher_text);
					bookshare_book_detail_copyright_text = (TextView)findViewById(R.id.bookshare_book_detail_copyright_text);
					bookshare_book_detail_synopsis_text = (TextView)findViewById(R.id.bookshare_book_detail_synopsis_text);
					btn_download = (Button)findViewById(R.id.bookshare_btn_download);
					bookshare_download_not_available_text = (TextView) findViewById(R.id.bookshare_download_not_available_msg);
					book_detail_view.requestFocus();
					// If the book is not downloadable, do not show the download button
					if(!isDownloadable){
						btn_download.setVisibility(View.GONE);
					} else {
						bookshare_download_not_available_text.setVisibility(View.GONE);
						btn_download.setOnClickListener(new OnClickListener(){
							public void onClick(View v){
								
								if(btn_download.getText().toString().equalsIgnoreCase("Download")){
									
									// Start a new Activity for getting the OM member list
									// See onActivityResult for further processing
									if(isOM){
										Intent intent = new Intent(getApplicationContext(), Bookshare_OM_List.class);
										intent.putExtra("username", username);
										intent.putExtra("password", password);
										startActivityForResult(intent, START_BOOKSHARE_OM_LIST);
									}
									else{
										new DownloadFilesTask().execute();
									}
								}
								
								// Navigate to the local library
								else if(btn_download.getText().toString().equalsIgnoreCase("Read Book")){
									setResult(BOOKSHARE_BOOK_DETAILS_FINISHED);
									if(downloadedBook == null){
										final VoiceableDialog finishedDialog = new VoiceableDialog(btn_download.getContext());
                                        String message =  resources.getString(R.string.book_details_open_error);
                                        finishedDialog.popup(message, 2000);
									}
									else{
										Library.Instance().addBookToRecentList(downloadedBook);
										((FBReaderApp)FBReaderApp.Instance()).openBook(downloadedBook, null);
										finish();
									}
								}
							}
						});
					}
					
					// Set the fields of the layout with book details
					if(metadata_bean.getTitle()!=null){
						for(int i = 0; i < metadata_bean.getTitle().length; i++){
							temp = temp + metadata_bean.getTitle()[i];
						}
						if(temp == null){
							temp = "";
						}
						bookshare_book_detail_title_text.setText(temp);
						temp = "";
					}

					if(metadata_bean.getAuthors() != null){
						for(int i = 0; i < metadata_bean.getAuthors().length; i++){
							if(i==0){
								temp = metadata_bean.getAuthors()[i];
							}
							else{
								temp = temp + ", "+metadata_bean.getAuthors()[i];
							}
						}
						if(temp == null){
							temp = "";
						}
						temp = temp.trim().equals("") ? "Not available" : temp;
						bookshare_book_detail_authors_text.setText(temp);
						temp = "";
					}
					else{
						bookshare_book_detail_authors_text.setText("Not available");
					}

					if(metadata_bean.getIsbn() != null){
						temp = metadata_bean.getIsbn().trim().equals("") ? "Not available" : metadata_bean.getIsbn();
						bookshare_book_detail_isbn_text.setText(temp);
						temp = "";
					}
					else{
						bookshare_book_detail_isbn_text.setText("Not available");
					}

					if(metadata_bean.getLanguage() != null){
						temp = metadata_bean.getLanguage().trim().equals("") ? "Not available" : metadata_bean.getLanguage();
						bookshare_book_detail_language_text.setText(temp);
						temp = "";
					}
					else{
						bookshare_book_detail_language_text.setText("Not available");
					}

					if(metadata_bean.getCategory() != null){
						for(int i = 0; i < metadata_bean.getCategory().length; i++){
							if(i==0){
								temp = metadata_bean.getCategory()[i];
							}
							else{
								temp = temp + ", "+metadata_bean.getCategory()[i];
							}
						}

						if(temp == null){
							temp = "";
						}
						temp = temp.trim().equals("") ? "Not available" : temp;
						bookshare_book_detail_category_text.setText(temp);
						temp = "";
					}
					else{
						bookshare_book_detail_category_text.setText("Not available");
					}

					if(metadata_bean.getPublishDate() != null){
						StringBuilder str_date =  new StringBuilder(metadata_bean.getPublishDate());
						String mm = str_date.substring(0,2);
						String month = "";
						if(mm.equalsIgnoreCase("01")){
							month = "January";
						}
						else if(mm.equals("02")){
							month = "February";
						}
						else if(mm.equals("03")){
							month = "March";
						}
						else if(mm.equals("04")){
							month = "April";
						}
						else if(mm.equals("05")){
							month = "May";
						}
						else if(mm.equals("06")){
							month = "June";
						}
						else if(mm.equals("07")){
							month = "July";
						}
						else if(mm.equals("08")){
							month = "August";
						}
						else if(mm.equals("09")){
							month = "September";
						}
						else if(mm.equals("10")){
							month = "October";
						}
						else if(mm.equals("11")){
							month = "November";
						}
						else if(mm.equals("12")){
							month = "December";
						}

						String publish_date = str_date.substring(2, 4) + " "+month+" " + str_date.substring(4);
						temp = publish_date.trim().equals("") ? "Not available" : publish_date;
						bookshare_book_detail_publish_date_text.setText(temp);
						temp = "";
					}
					else{
						bookshare_book_detail_publish_date_text.setText("Not available");
					}

					if(metadata_bean.getPublisher() != null){
						temp = metadata_bean.getPublisher().trim().equals("") ? "Not available" : metadata_bean.getPublisher();
						bookshare_book_detail_publisher_text.setText(temp);
						temp = "";
					}
					else{
						bookshare_book_detail_publisher_text.setText("Not available");
					}

					if(metadata_bean.getCopyright() != null){
						temp = metadata_bean.getCopyright().trim().equals("") ? "Not available" : metadata_bean.getCopyright();
						bookshare_book_detail_copyright_text.setText(temp);
						temp = "";
					}
					else{
						bookshare_book_detail_copyright_text.setText("Not available");
					}

					if(metadata_bean.getCompleteSynopsis() != null &&
							metadata_bean.getBriefSynopsis() != null){

						for(int i = 0; i < metadata_bean.getCompleteSynopsis().length; i++){
							if(i==0){
								temp = metadata_bean.getCompleteSynopsis()[i];
							}
							else{
								temp = temp + " "+metadata_bean.getCompleteSynopsis()[i];
							}
						}
						if(temp == null){
							temp = "";
						}
						temp  = temp.trim().equals("") ? "Not available" : temp;
						bookshare_book_detail_synopsis_text.setText(temp.trim());
						//System.out.println("Complete synopsis = "+temp.trim());
						temp = "";
					}
					else if(metadata_bean.getBriefSynopsis() != null &&
							metadata_bean.getCompleteSynopsis() == null){
						for(int i = 0; i < metadata_bean.getBriefSynopsis().length; i++){
							if(i==0){
								temp = metadata_bean.getBriefSynopsis()[i];
							}
							else{
								temp = temp + " "+metadata_bean.getBriefSynopsis()[i];
							}
						}
						if(temp == null){
							temp = "";
						}
						temp = temp.trim().equals("") ? "Not available" : temp;
						bookshare_book_detail_synopsis_text.setText(temp.trim());
						System.out.println("Brief Synopsis = "+temp);
						temp = "";
					}
					else if(metadata_bean.getCompleteSynopsis() != null &&
							metadata_bean.getBriefSynopsis() == null){
						for(int i = 0; i < metadata_bean.getCompleteSynopsis().length; i++){
							if(i==0){
								temp = metadata_bean.getCompleteSynopsis()[i];
							}
							else{
								temp = temp + " "+metadata_bean.getCompleteSynopsis()[i];
							}
						}
						if(temp == null){
							temp = "";
						}
						temp  = temp.trim().equals("") ? "Not available" : temp;

						bookshare_book_detail_synopsis_text.setText(temp.trim());
						temp = "";
					}
					else if(metadata_bean.getBriefSynopsis() == null &&
							metadata_bean.getCompleteSynopsis() == null){
						bookshare_book_detail_synopsis_text.setText("No Synopsis available");
					}
				}
			}	
		}
	};
	
	// A custom AsyncTask class for carrying out the downloading task in a separate background thread
	private class DownloadFilesTask extends AsyncTask<Void, Void, Void>{
		
		private Bookshare_Error_Bean error;
		
		// Will be called in the UI thread
		@Override
		protected void onPreExecute(){
			btn_download.setText("Downloading Book...");
			libraryTreeBeforeDownload = Library.Instance().getFirstLevelTree(Library.ROOT_BY_AUTHOR);
			if(bookInstances == null){
				bookInstances = new Vector<Long>();						
			}
			else{
				bookInstances.clear();
			}
			for (FBTree tree : libraryTreeBeforeDownload) {
				if(tree instanceof BookTree){
					Book book = ((BookTree)tree).Book;
					// Add the book instance to the Vector
					bookInstances.add(book.getId());
				}
			}
			
			// Disable the download button while the download is in progress
			btn_download.setEnabled(false);
		}
		
		// Will be called in a separate thread
		@Override
		protected Void doInBackground(Void... params) {			 
			final String id = metadata_bean.getContentId();
			String download_uri = "";
			if(isFree)
				download_uri = "https://api.bookshare.org/download/content/"+id+"/version/1?api_key="+developerKey;
			else if(isOM){
				download_uri = "https://api.bookshare.org/download/member/"+memberId+"content/"+id+"/version/1/for/"+username+"?api_key="+developerKey;
			}
			else{
				download_uri = "https://api.bookshare.org/download/content/"+id+"/version/1/for/"+username+"?api_key="+developerKey;
			}
			
			try{
				System.out.println("download_uri :"+download_uri);
				HttpResponse response = bws.getHttpResponse(password, download_uri);
				// Get hold of the response entity
				HttpEntity entity = response.getEntity();
				
				if (entity != null) {
					String filename = "bookshare_"+Math.random()*10000+".zip";
					if(metadata_bean.getTitle() != null){
						String temp = "";
						for(int i = 0; i < metadata_bean.getTitle().length; i++){
							temp = temp + metadata_bean.getTitle()[i];
						}
						filename = temp;
						filename = filename.replaceAll(" +", "_").replaceAll(":", "__");
					}
					String zip_file = Paths.BooksDirectoryOption().getValue() +"/"+ filename + ".zip";
					
					File downloaded_zip_file = new File(zip_file);
					if(downloaded_zip_file.exists()){
						downloaded_zip_file.delete();
					}
					Header header = entity.getContentType();
					System.out.println("******  zip_file *****"+zip_file);
					if(header.getValue().contains("zip")){
						try{
							System.out.println("Contains zip");
							java.io.BufferedInputStream in = new java.io.BufferedInputStream(entity.getContent());
							java.io.FileOutputStream fos = new java.io.FileOutputStream(downloaded_zip_file);
							java.io.BufferedOutputStream bout = new BufferedOutputStream(fos,1024);
							byte[] data = new byte[1024];
							int x=0;
							while((x=in.read(data,0,1024))>=0){
								bout.write(data,0,x);
							}
							fos.flush();
							bout.flush();
							fos.close();
							bout.close();
							in.close();
							
							System.out.println("******** Downloading complete");
							
							// Unzip the encrypted archive file 
							if(!isFree){
								System.out.println("******Before creating ZipFile******"+zip_file);
								// Initiate ZipFile object with the path/name of the zip file.
								ZipFile zipFile = new ZipFile(zip_file);
								
								// Check to see if the zip file is password protected
								if (zipFile.isEncrypted()) {
									System.out.println("******isEncrypted******");

									// if yes, then set the password for the zip file
									if(!isOM){
										zipFile.setPassword(password);
									}									
									// Set the OM password sent by the Intent
									else{
										// Obtain the SharedPreferences object shared across the application. It is stored in login activity
										SharedPreferences login_preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
										omDownloadPassword = login_preference.getString("downloadPassword", "");
										zipFile.setPassword(omDownloadPassword);
									}
								}
								
								// Get the list of file headers from the zip file
								List fileHeaderList = zipFile.getFileHeaders();

								System.out.println("******Before for******");
								// Loop through the file headers
								for (int i = 0; i < fileHeaderList.size(); i++) {
									FileHeader fileHeader = (FileHeader)fileHeaderList.get(i);
									System.out.println(Paths.BooksDirectoryOption().getValue() +"/"+ filename);
									// Extract the file to the specified destination
									zipFile.extractFile(fileHeader, Paths.BooksDirectoryOption().getValue() +"/"+ filename);
								}
							}
							// Unzip the non-encrypted archive file
							else{
								try
						        {
									File file = new File(Paths.BooksDirectoryOption().getValue() +"/"+ filename);
									file.mkdir();
						            String destinationname = Paths.BooksDirectoryOption().getValue() +"/"+ filename + "/";
						            byte[] buf = new byte[1024];
						            ZipInputStream zipinputstream = null;
						            ZipEntry zipentry;
						            zipinputstream = new ZipInputStream(new FileInputStream(zip_file));

						            zipentry = zipinputstream.getNextEntry();
						            while (zipentry != null)
						            {
						                // for each entry to be extracted
						                String entryName = zipentry.getName();
						                System.out.println("entryname "+entryName);
						                int n;
						                FileOutputStream fileoutputstream;
						                File newFile = new File(entryName);
						                String directory = newFile.getParent();
						                
						                if(directory == null)
						                {
						                    if(newFile.isDirectory())
						                        break;
						                }
						                
						                fileoutputstream = new FileOutputStream(
						                   destinationname+entryName);             

						                while ((n = zipinputstream.read(buf, 0, 1024)) > -1)
						                    fileoutputstream.write(buf, 0, n);

						                fileoutputstream.close(); 
						                zipinputstream.closeEntry();
						                zipentry = zipinputstream.getNextEntry();

						            }//while

						            zipinputstream.close();
						        }
						        catch (Exception e)
						        {
						            e.printStackTrace();
						        }
							}
							// Delete the downloaded zip file as it has been extracted
							downloaded_zip_file = new File(zip_file);
							if(downloaded_zip_file.exists()){
								downloaded_zip_file.delete();
							}
							downloadSuccess  = true;
						}
						catch(ZipException e){
							System.out.println("ZipException:"+e);
						}
					}
					else{
						downloadSuccess = false;
						error = new Bookshare_Error_Bean();
						error.parseInputStream(response.getEntity().getContent());
					}
				}
			}
			catch(URISyntaxException use){
				System.out.println("URISyntaxException: "+use);
			}
			catch(IOException ie){
				System.out.println("IOException: "+ie);
			}
			return null;
		}

		// Will be called in the UI thread
		@Override
		protected void onPostExecute(Void param){
			
			// Clear the library instance to fetch recently downloaded book
            // todo - Rom - need to reimplement after new code base
			//Library.Instance().clear();

			// Get the latest set of books
			LibraryTree libraryTreeAfterDownload = Library.Instance().getFirstLevelTree(Library.ROOT_RECENT);
			for (FBTree tree : libraryTreeAfterDownload) {
				if(tree instanceof BookTree){
					Book book = ((BookTree)tree).Book;
					// Locate the recently downloaded book
					if(!bookInstances.contains(book.getId())){
						downloadedBook = book;						
						// Add the downloaded book to recents list. The book will now appear under recent tab in local library.
						Library.Instance().addBookToRecentList(book);
						break;
					}
				}
			}

            final VoiceableDialog finishedDialog = new VoiceableDialog(btn_download.getContext());
			
			if(downloadSuccess){
                String message =  resources.getString(R.string.book_details_download_success);
                finishedDialog.popup(message, 1500);
				btn_download.setText(resources.getString(R.string.book_details_download_success));
				btn_download.setEnabled(true);
			}
			else{
				String message = error != null ? error.getMessagesFormatted() : "Download Failed!";
				message = resources.getString(R.string.book_details_download_error);
                finishedDialog.popup(message, 1500);

				btn_download.setText(resources.getString(R.string.book_details_download_error));
				btn_download.setEnabled(true);
				//finish();
			}
		}
    }

	/**
	 * Uses a SAX parser to parse the response
	 * @param response String representing the response
	 */
	private void parseResponse(String response){

		InputSource is = new InputSource(new StringReader(response));

		try{
			/* Get a SAXParser from the SAXPArserFactory. */
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp;
			sp = spf.newSAXParser();

			/* Get the XMLReader of the SAXParser we created. */
			XMLReader parser = sp.getXMLReader();
			parser.setContentHandler(new SAXHandler());
			parser.parse(is);
		}
		catch(SAXException e){
			System.out.println(e);
		}
		catch (ParserConfigurationException e) {
			System.out.println(e);
		}
		catch(IOException ioe){
		}
	}
	
	// Class that applies parsing logic
	private class SAXHandler extends DefaultHandler{

		int count;
		boolean metadata = false;
		boolean contentId = false;
		boolean daisy = false;
		boolean brf = false;
		boolean downloadFormats = false;
		boolean images = false;
		boolean isbn = false;
		boolean authors = false;
		boolean title = false;
		boolean publishDate = false;
		boolean publisher = false;
		boolean copyright = false;
		boolean language = false;
		boolean briefSynopsis = false;
		boolean completeSynopsis = false;
		boolean quality = false;
		boolean category = false;
		boolean bookshareId = false;
		boolean freelyAvailable = false;

		boolean authorElementVisited = false;
		boolean downloadFormatElementVisited = false;
		boolean titleElementVisited = false;
		boolean categoryElementVisited = false;
		boolean briefSynopsisElementVisited = false;
		boolean completeSynopsisElementVisited = false;
		Vector<String> vector_author;
		Vector<String> vector_downloadFormat;
		Vector<String> vector_category;
		Vector<String> vector_briefSynopsis;
		Vector<String> vector_completeSynopsis;
		Vector<String> vector_title;

		public void startElement(String namespaceURI, String localName, String qName, Attributes atts){

			if(qName.equalsIgnoreCase("metadata")){
				System.out.println("******* metadata visited");
				metadata = true;
				metadata_bean = new Bookshare_Metadata_Bean();
				authorElementVisited = false;
				downloadFormatElementVisited = false;
				titleElementVisited = false;
				categoryElementVisited = false;
				briefSynopsisElementVisited = false;
				completeSynopsisElementVisited = false;
				vector_author = new Vector<String>();
				vector_downloadFormat = new Vector<String>();
				vector_category = new Vector<String>();
				vector_briefSynopsis = new Vector<String>();
				vector_completeSynopsis = new Vector<String>();
				vector_title = new Vector<String>();
			}
			if(qName.equalsIgnoreCase("content-id")){
				contentId = true;
			}
			if(qName.equalsIgnoreCase("daisy")){
				daisy = true;
			}
			if(qName.equalsIgnoreCase("brf")){
				brf = true;
			}
			if(qName.equalsIgnoreCase("download-format")){
				downloadFormats = true;
				if(!downloadFormatElementVisited){
					downloadFormatElementVisited = true;
				}
			}
			if(qName.equalsIgnoreCase("images")){
				images = true;
			}
			if(qName.equalsIgnoreCase("isbn10") || qName.equalsIgnoreCase("isbn13") ){
				isbn = true;
			}
			if(qName.equalsIgnoreCase("author")){
				authors = true;
				if(!authorElementVisited){
					authorElementVisited = true;
				}
			}
			if(qName.equalsIgnoreCase("title")){
				title = true;
				if(!titleElementVisited){
					titleElementVisited = true;
				}
			}
			if(qName.equalsIgnoreCase("publish-date")){
				publishDate = true;
			}
			if(qName.equalsIgnoreCase("publisher")){
				publisher = true;
			}
			if(qName.equalsIgnoreCase("copyright")){
				copyright = true;
			}
			if(qName.equalsIgnoreCase("language")){
				language = true;
			}
			if(qName.equalsIgnoreCase("brief-synopsis")){				
				briefSynopsis = true;
				if(!briefSynopsisElementVisited){
					briefSynopsisElementVisited = true;
				}
			}
			if(qName.equalsIgnoreCase("complete-synopsis")){
				completeSynopsis = true;
				if(!completeSynopsisElementVisited){
					completeSynopsisElementVisited = true;
				}
			}
			if(qName.equalsIgnoreCase("freely-available")){
				freelyAvailable = true;
			}
			if(qName.equalsIgnoreCase("quality")){
				quality = true;
			}
			if(qName.equalsIgnoreCase("bookshare-id")){
				bookshareId = true;
			}
			if(qName.equalsIgnoreCase("category")){
				category = true;
				if(!categoryElementVisited){
					categoryElementVisited = true;
				}
			}
		}

		public void endElement(String uri, String localName, String qName){

			//  End of one metadata element parsing.
			if(qName.equalsIgnoreCase("metadata")){
				metadata = false;
			}
			if(qName.equalsIgnoreCase("content-id")){
				contentId = false;
			}
			if(qName.equalsIgnoreCase("daisy")){
				daisy = false;
			}
			if(qName.equalsIgnoreCase("brf")){
				brf = false;
			}
			if(qName.equalsIgnoreCase("download-format")){
				downloadFormats = false;
			}
			if(qName.equalsIgnoreCase("images")){
				images = false;
			}
			if(qName.equalsIgnoreCase("isbn10") || qName.equalsIgnoreCase("isbn13") ){
				isbn = false;
			}
			if(qName.equalsIgnoreCase("author")){
				authors = false;
			}
			if(qName.equalsIgnoreCase("title")){
				title = false;
			}
			if(qName.equalsIgnoreCase("publish-date")){
				publishDate = false;
			}
			if(qName.equalsIgnoreCase("publisher")){
				publisher = false;
			}
			if(qName.equalsIgnoreCase("copyright")){
				copyright = false;
			}
			if(qName.equalsIgnoreCase("language")){
				language = false;
			}
			if(qName.equalsIgnoreCase("brief-synopsis")){
				briefSynopsis = false;
			}
			if(qName.equalsIgnoreCase("complete-synopsis")){
				completeSynopsis = false;
			}
			if(qName.equalsIgnoreCase("freely-available")){
				freelyAvailable = false;
			}
			if(qName.equalsIgnoreCase("quality")){
				quality = false;
			}
			if(qName.equalsIgnoreCase("category")){
				category = false;
			}
			if(qName.equalsIgnoreCase("bookshare-id")){
				bookshareId = false;
			}
		}
		public void characters(char[] c, int start, int length){

			if(metadata){
				if(contentId){
					metadata_bean.setContentId(new String(c,start,length));
				}
				if(daisy){
					metadata_bean.setDaisy(new String(c,start,length));
				}
				if(brf){
					metadata_bean.setBrf(new String(c,start,length));
				}
				if(downloadFormats){
					vector_downloadFormat.add(new String(c,start,length));
					metadata_bean.setDownloadFormats(vector_downloadFormat.toArray(new String[0]));
				}
				if(images){
					metadata_bean.setImages(new String(c,start,length));
				}
				if(isbn){
					metadata_bean.setIsbn(new String(c,start,length));
				}

				if(authors){
					vector_author.add(new String(c,start,length));
					metadata_bean.setAuthors(vector_author.toArray(new String[0]));
				}
				if(title){
					vector_title.add(new String(c,start,length));
					metadata_bean.setTitle(vector_title.toArray(new String[0]));
				}
				if(publishDate){
					metadata_bean.setPublishDate(new String(c,start,length));
				}
				if(publisher){
					metadata_bean.setPublisher(new String(c,start,length));
				}
				if(copyright){
					metadata_bean.setCopyright(new String(c,start,length));
				}
				if(language){
					metadata_bean.setLanguage(new String(c,start,length));
				}
				if(briefSynopsis){
					vector_briefSynopsis.add(new String(c,start,length));
					metadata_bean.setBriefSynopsis(vector_briefSynopsis.toArray(new String[0]));
				}
				if(completeSynopsis){
					vector_completeSynopsis.add(new String(c,start,length));
					metadata_bean.setCompleteSynopsis(vector_completeSynopsis.toArray(new String[0]));
				}
				if(quality){
					metadata_bean.setQuality(new String(c,start,length));
				}
				if(category){
					vector_category.add(new String(c,start,length));
					metadata_bean.setCategory(vector_category.toArray(new String[0]));
					System.out.println("metadata_bean.getCategory() = "+metadata_bean.getCategory());

				}
				if(bookshareId){
					metadata_bean.setBookshareId(new String(c,start,length));
				}
				if(freelyAvailable){
					metadata_bean.setFreelyAvailable(new String(c,start,length));
				}
			}
		}
	}
	
	// For keeping the screen from rotating
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
	}
	
	//Determine whether the book is downloadable.
	private void setIsDownloadable(final Bookshare_Metadata_Bean bean) {
		isDownloadable = (bean.getDownloadFormats() != null && bean.getDownloadFormats().length > 0);
	}
}

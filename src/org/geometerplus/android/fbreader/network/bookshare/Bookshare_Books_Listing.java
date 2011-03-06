package org.geometerplus.android.fbreader.network.bookshare;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.bookshare.net.BookshareWebservice;
import org.geometerplus.zlibrary.ui.android.R;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

/**
 * This ListActivity shows the search results
 * in form of a ListView.
 *
 */
public class Bookshare_Books_Listing extends ListActivity{

	private String URI_BOOKSHARE_ID_SEARCH ="https://api.bookshare.org/book/id/";
	private String username;
	private String password;
	private String requestURI;
	private String requestType;
	private String responseType;
	private final int DATA_FETCHED = 99;
	private Vector<Bookshare_Result_Bean> vectorResults;
	private ProgressDialog pd_spinning;
	private final int START_BOOKSHARE_BOOK_DETAILS_ACTIVITY = 0;
	private final int BOOKSHARE_BOOK_DETAILS_FINISHED = 1;
	private final int BOOKSHARE_BOOKS_LISTING_FINISHED = 2;
	private ArrayList<TreeMap<String,Object>> list = new ArrayList<TreeMap<String, Object>>();
	TreeMap<Integer,Object> icons_map = new TreeMap<Integer,Object>();
	InputStream inputStream;
	final BookshareWebservice bws = new BookshareWebservice();
	private int total_pages_result;
	private int current_result_page = 1;
	private boolean total_pages_count_known = false;
	private boolean isFree = false;
	private String developerKey = BookshareDeveloperKey.DEVELOPER_KEY;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		Intent intent  = getIntent();
		username = intent.getStringExtra("ws_username");
		password = intent.getStringExtra("ws_password");

		if(username == null || password == null){
			isFree = true;
		}
		
		requestURI = intent.getStringExtra("REQUEST_URI");
		System.out.println("requestURI = "+requestURI);
		requestType = intent.getStringExtra("REQUEST_TYPE");
		
		if(requestType.equalsIgnoreCase("Title Search")
				|| requestType.equalsIgnoreCase("Author Search")
				|| requestType.equalsIgnoreCase("Latest Books")){
			responseType = "Book List Response";
		}
		else if(requestType.equalsIgnoreCase("Bookshare ID Search")
				|| requestType.equalsIgnoreCase("ISBN Search")){
			responseType  = "Book Metadata Response";
		}
		getListing(requestURI);
	}
	
	/*
	 * Spawn a new Thread for carrying out the search
	 */
	private void getListing(final String uri){
		
		vectorResults = new Vector<Bookshare_Result_Bean>();

		// Show a Progress Dialog before the book opens
		pd_spinning = ProgressDialog.show(this, null, "Fetching books data. Please wait.", Boolean.TRUE);
		
		new Thread(){
			public void run(){
				try{
					inputStream = bws.getResponseStream(username, password, uri);
					
					// Once the response is obtained, send message to the handler
					Message msg = Message.obtain();
					msg.what = DATA_FETCHED;
					msg.setTarget(handler);
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
	

	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		super.onPrepareOptionsMenu(menu);
		menu.clear();

		if(current_result_page > 1 ){
			MenuItem item = menu.add("Previous Page");
			item.setIcon(R.drawable.arrow_left_blue);			
		}

		if(current_result_page < total_pages_result ){
			MenuItem item = menu.add("Next Page");
			item.setIcon(R.drawable.arrow_right_blue);
		}

		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getTitle().equals("Next Page")){
			current_result_page++;
		}
		else if(item .getTitle().equals("Previous Page")){
			current_result_page--;
		}
		list.clear();
		
		StringBuilder strBuilder = new StringBuilder(requestURI);
		int index = -1;
		
		if((index = strBuilder.indexOf("?api_key=")) != -1){
			strBuilder.delete(index, strBuilder.length());
			strBuilder.append("/page/"+current_result_page+"?api_key="+developerKey);
		}
		getListing(strBuilder.toString());
		return true;
	}

	// Handler for dealing with the stream obtained as a result of search 
	Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg){

			// Message received that data has been fetched from the bookshare web services 
			if(msg.what == DATA_FETCHED){

				setContentView(R.layout.bookshare_menu_main);

				// Dismiss the progress dialog
				pd_spinning.cancel();

				String response_HTML = bws.convertStreamToString(inputStream);
				
				// Cleanup the HTML formatted tags
				String response = response_HTML.replace("&apos;", "'").replace("&quot;", "\"").replace("&amp;", "&").replace("&#xd;","").replace("&#x97;", "-");
				
				// Parse the response of search result
				parseResponse(response);

				if(responseType.equalsIgnoreCase("Book Metadata Response")){
					//Do nothing
				}

				// Returned response is of our use. Process it
				if(responseType.equalsIgnoreCase("Book List Response")){
					list.clear();
					
					// For each bean object stored in the vector, create a row in the list
					for(Bookshare_Result_Bean bean : vectorResults){
						String authors = "";
						TreeMap<String, Object> row_item = new TreeMap<String, Object>();
						row_item.put("title", bean.getTitle());
						for(int i = 0; i < bean.getAuthor().length; i++){
							if(i==0){
								authors = bean.getAuthor()[i];
							}
							else{
								authors = authors +", "+ bean.getAuthor()[i];
							}
						}
						row_item.put("authors", authors);
						row_item.put("icon", R.drawable.titles);
						
						// Add a download icon if the book is available to download
						if(!isFree){
							if(bean.getAvailableToDownload().equals("1")){
								row_item.put("download_icon", R.drawable.download_icon);
							}
						}
						else if(isFree && bean.getAvailableToDownload().equals("1") &&
									bean.getFreelyAvailable().equals("1")){
							row_item.put("download_icon", R.drawable.download_icon);
						}
						else{
							row_item.put("download_icon", R.drawable.black_icon);
						}
						list.add(row_item);
					}
				}
				
				// Instantiate the custom SimpleAdapter for populating the ListView
				MySimpleAdapter simpleadapter = new MySimpleAdapter(
						getApplicationContext(),list,
						R.layout.bookshare_menu_item,
						new String[]{"title","authors","icon","download_icon"},
						new int[]{R.id.text1, R.id.text2,R.id.row_icon, R.id.bookshare_download_icon});


				//Set the adapter for this view
				setListAdapter(simpleadapter);

				ListView lv = getListView();
				lv.setTextFilterEnabled(true);
				lv.setOnItemClickListener(new OnItemClickListener(){

					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

						// Obtain the layout for selected row
						LinearLayout row_view  = (LinearLayout)view;

						// Obtain the text of the row containing title
						TextView txt_title_name = (TextView)row_view.findViewById(R.id.text1);
						
						// Obtain the text of the row
						TextView txt_authors_name = (TextView)row_view.findViewById(R.id.text2);
						
						String authors = "";

						// Find the corresponding bean object for this row
						for(Bookshare_Result_Bean bean : vectorResults){

							// Retrieve author name(s), which serve as a comparison parameter along with title
							for(int i = 0; i < bean.getAuthor().length; i++){
								if(i==0){
									authors = bean.getAuthor()[i];
								}
								else{
									authors = authors +", "+ bean.getAuthor()[i];
								}
							}
							
							// Find the matching bean entry from the vector and get its book ID
							if(bean.getTitle().equalsIgnoreCase(txt_title_name.getText().toString()) &&
									authors.equalsIgnoreCase(txt_authors_name.getText().toString())){
								String bookshare_ID = bean.getId();
								Intent intent = new Intent(getApplicationContext(),Bookshare_Book_Details.class);
								String uri = "";
								if(isFree)
									uri = URI_BOOKSHARE_ID_SEARCH + bookshare_ID + "?api_key="+developerKey;
								else
									uri = URI_BOOKSHARE_ID_SEARCH + bookshare_ID +"/for/"+username+"?api_key="+developerKey;
								
								if((isFree && bean.getAvailableToDownload().equals("1") &&
										bean.getFreelyAvailable().equals("1")) ||
										(!isFree && bean.getAvailableToDownload().equals("1"))){
									intent.putExtra("isDownloadable", true);
								}
								else{
									intent.putExtra("isDownloadable", false);
								}
								intent.putExtra("ID_SEARCH_URI", uri);
								if(!isFree){
									intent.putExtra("username", username);
									intent.putExtra("password", password);
								}

								startActivityForResult(intent, START_BOOKSHARE_BOOK_DETAILS_ACTIVITY);
								break;
							}
						}
					}
				});
			}
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == START_BOOKSHARE_BOOK_DETAILS_ACTIVITY){
			if(resultCode == BOOKSHARE_BOOK_DETAILS_FINISHED){
				setResult(BOOKSHARE_BOOKS_LISTING_FINISHED);
				finish();
			}
		}
	}

	// Used for keeping the the screen from rotating
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
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
			System.out.println(ioe);
		}
	}

	// Class containing the logic for parsing the response of search results
	private class SAXHandler extends DefaultHandler{

		int count;
		boolean result = false;
		boolean id = false;
		boolean title = false;
		boolean author = false;
		boolean download_format = false;
		boolean images = false;
		boolean freely_available = false;
		boolean available_to_download = false;
		boolean num_pages = false;

		boolean authorElementVisited = false;
		boolean downloadFormatElementVisited = false; 
		Vector<String> vector_author;
		Vector<String> vector_downloadFormat;

		Bookshare_Result_Bean result_bean;

		public void startElement(String namespaceURI, String localName, String qName, Attributes atts){
			
			if(!total_pages_count_known)
			{
				if(qName.equalsIgnoreCase("num-pages")){
					num_pages = true;
					total_pages_count_known = false;
				}
			}

			if(qName.equalsIgnoreCase("result")){
				result = true;
				result_bean = new Bookshare_Result_Bean();
				authorElementVisited = false;
				downloadFormatElementVisited = false;
				vector_author = new Vector<String>();
				vector_downloadFormat = new Vector<String>();
			}
			if(qName.equalsIgnoreCase("id")){
				id = true;
			}
			if(qName.equalsIgnoreCase("title")){
				title = true;
			}
			if(qName.equalsIgnoreCase("author")){
				author = true;
				if(!authorElementVisited){
					authorElementVisited = true;
				}
			}
			if(qName.equalsIgnoreCase("download-format")){
				download_format = true;
				if(!downloadFormatElementVisited){
					downloadFormatElementVisited = true;
				}
			}
			if(qName.equalsIgnoreCase("images")){
				images = true;
			}
			if(qName.equalsIgnoreCase("freely-available")){
				freely_available = true;
			}
			if(qName.equalsIgnoreCase("available-to-download")){
				available_to_download = true;
			}
		}

		public void endElement(String uri, String localName, String qName){

			if(num_pages){
				if(qName.equalsIgnoreCase("num-pages")){
					num_pages = false;
				}
			}
			if(qName.equalsIgnoreCase("result")){
				result = false;
				vectorResults.add(result_bean);
				result_bean = null;
			}
			if(qName.equalsIgnoreCase("id")){
				id = false;
			}
			if(qName.equalsIgnoreCase("title")){
				title = false;
			}
			if(qName.equalsIgnoreCase("author")){
				author = false;
			}
			if(qName.equalsIgnoreCase("download-format")){
				download_format = false;
			}
			if(qName.equalsIgnoreCase("images")){
				images = false;
			}
			if(qName.equalsIgnoreCase("freely-available")){
				freely_available = false;
			}
			if(qName.equalsIgnoreCase("available-to-download")){
				available_to_download = false;
			}
		}

		public void characters(char[] c, int start, int length){
			
			if(num_pages){
				total_pages_result = Integer.parseInt(new String(c,start,length));
			}
			if(result){
				if(id){
					result_bean.setId(new String(c,start,length));
				}
				if(title){
					result_bean.setTitle(new String(c,start,length));
				}
				if(author){
					vector_author.add(new String(c,start,length));
					result_bean.setAuthor(vector_author.toArray(new String[0]));
				}
				if(download_format){
					vector_downloadFormat.add(new String(c,start,length));
					result_bean.setDownloadFormats(vector_downloadFormat.toArray(new String[0]));
				}
				if(images){
					result_bean.setImages(new String(c,start,length));
				}
				if(freely_available){
					result_bean.setFreelyAvailable(new String(c,start,length));
				}
				if(available_to_download){
					result_bean.setAvailableToDownload(new String(c,start,length));
				}
			}
		}
	}

	// A custom SimpleAdapter class for providing data to the ListView
	private class MySimpleAdapter extends SimpleAdapter{
		public MySimpleAdapter(Context context, List<? extends Map<String, ?>> data,
				int resource, String[] from, int[] to) {
			super(context, data, resource, from, to);
		}

		/*
		 * Retrieves view for the item in the adapter, at the
		 * specified position and populates it with data.
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.bookshare_menu_item, null);
			}

			TreeMap<String, Object> data = (TreeMap<String, Object>) getItem(position);

			((TextView) convertView.findViewById(R.id.text1))
			.setText((String) data.get("title"));

			((TextView) convertView.findViewById(R.id.text2))
			.setText((String) data.get("authors"));

			((ImageView) convertView.findViewById(R.id.row_icon))
			.setImageResource(((Integer)data.get("icon")).intValue());

			if(data.get("download_icon") != null){				
				((ImageView)convertView.findViewById(R.id.bookshare_download_icon))
				.setImageResource(((Integer)data.get("download_icon")).intValue());
			}
			return convertView;
		}
	}
}

package org.geometerplus.android.fbreader.network.bookshare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.geometerplus.zlibrary.core.util.ZLNetworkUtil;
import org.geometerplus.zlibrary.ui.android.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This ListActivity shows options for retrieving data from Bookshare.
 */
public class Bookshare_Menu extends ListActivity {

	private final String[] items = {"Title Search","Author Search","ISBN Search","Latest Books","Popular Books"};
	private final String[] description = {"Search by title","Search by author","Search by book's ISBN","Search the latest catalog","Search popular books "};
	ArrayList<TreeMap<String,Object>> list = new ArrayList<TreeMap<String, Object>>();
	private Dialog dialog;
	private EditText dialog_search_term;
	private TextView dialog_search_title;
	private TextView dialog_example_text;
	private String search_term = "";
	private String URI_String = "https://api.bookshare.org/book/";
	private String query_type;
	private Intent intent;
	private final int START_BOOKSHARE_BOOKS_LISTING_ACTIVITY = 0;
	private final int BOOKSHARE_BOOKS_LISTING_FINISHED = 2;
	private final int BOOKSHARE_MENU_FINISHED = 1;
	private String username;
	private String password;
	private boolean isFree = false; 
	private String developerKey = BookshareDeveloperKey.DEVELOPER_KEY;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.bookshare_menu_main);
		
		// Fetch the login info from the caller intent
		Intent callerIntent  = getIntent();
		username = callerIntent.getStringExtra("username");
		password = callerIntent.getStringExtra("password");
		
		if(username == null || password == null){
			isFree = true;
		}
		final int[] drawables = new int[] {
            R.drawable.titles,
            R.drawable.authors,
            R.drawable.isbn,
            R.drawable.latest,
            R.drawable.isbn		        
		};
		//Create a TreeMap for use in the SimpleAdapter
		for(int i = 0; i < drawables.length; i++){
			TreeMap<String, Object> row_item = new TreeMap<String, Object>();
			row_item.put("Name", items[i]);
			row_item.put("description", description[i]);
			row_item.put("icon", drawables[i]);
			list.add(row_item);
		}		
		// Construct a SimpleAdapter which will serve as data source for this ListView
		MySimpleAdapter simpleadapter = new MySimpleAdapter(
				this,list,
				R.layout.bookshare_menu_item,
				new String[]{"Name","description","icon"},
				new int[]{R.id.text1, R.id.text2,R.id.row_icon});

		//Set the adapter for this view
		setListAdapter(simpleadapter);
		
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		dialog = new Dialog(this);
		dialog.setContentView(R.layout.bookshare_dialog);
		dialog_search_term = (EditText)dialog.findViewById(R.id.bookshare_dialog_search_edit_txt);
		dialog_search_title = (TextView)dialog.findViewById(R.id.bookshare_dialog_search_txt);
		dialog_example_text = (TextView)dialog.findViewById(R.id.bookshare_dialog_search_example);
		Button dialog_ok = (Button)dialog.findViewById(R.id.bookshare_dialog_btn_ok);
		Button dialog_cancel = (Button)dialog.findViewById(R.id.bookshare_dialog_btn_cancel);
		
		dialog_ok.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				
				// Hide the virtual keyboard
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(dialog_search_term.getWindowToken(), 0);
				
				// Remove the leading and trailing spaces
				String search_term = ZLNetworkUtil.htmlEncode(dialog_search_term.getText().toString().trim());
				
				if(search_term.equals("")){
					Toast toast = Toast.makeText(getApplicationContext(), "Search term cannot be blank", Toast.LENGTH_SHORT);
					toast.show();
					return;
				}
				
				boolean isMetadataSearch = false;

				if(query_type.equalsIgnoreCase("Title Search")){
					if(isFree)
						search_term = URI_String+"search/title/"+search_term+"?api_key="+developerKey;
					else
						search_term = URI_String+"search/title/"+search_term+"/for/"+username+"?api_key="+developerKey;
				}
				else if(query_type.equalsIgnoreCase("Author Search")){
					if(isFree)
						search_term = URI_String+"search/author/"+search_term+"?api_key="+developerKey;
					else
						search_term = URI_String+"search/author/"+search_term+"/for/"+username+"?api_key="+developerKey;
				}
				else if(query_type.equalsIgnoreCase("ISBN Search")){
					if(isFree)
						search_term = URI_String+"isbn/"+search_term+"?api_key="+developerKey;
					else
						search_term = URI_String+"isbn/"+search_term+"/for/"+username+"?api_key="+developerKey;
					
					isMetadataSearch = true;
				}
				/*else if(query_type.equalsIgnoreCase("Latest Books")){
					int num = 0;
					try{
						num = Integer.parseInt(search_term);
					}
					catch(NumberFormatException e){
						Toast toast = Toast.makeText(getApplicationContext(), search_term.trim()+": Enter date in MMDDYYYY format",Toast.LENGTH_SHORT);
						toast.show();
					}
					if(search_term.length()==8){
						if(isFree)
							search_term = URI_String+"search/since/"+search_term+"?api_key="+developerKey;
						else
							search_term = URI_String+"search/since/"+search_term+"/for/"+username+"?api_key="+developerKey;
					}
					else{
						Toast toast = Toast.makeText(getApplicationContext(), search_term.trim()+": Enter date in MMDDYYYY format",Toast.LENGTH_SHORT);
						toast.show();
						return;
					}
				}*/

				if(isMetadataSearch){
					intent.putExtra("ID_SEARCH_URI", search_term);
					isMetadataSearch = false;
				}
				else{
					intent.putExtra("REQUEST_URI", search_term);
				}
				dialog.dismiss();
				if(!isFree){
					intent.putExtra("username", username);
					intent.putExtra("password", password);
				}
				
				startActivityForResult(intent, START_BOOKSHARE_BOOKS_LISTING_ACTIVITY);
			}
		});
		
		dialog_cancel.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				dialog.dismiss();
			}
		});
		
		//Listener for the ListView
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				// Obtain the layout for selected row
				LinearLayout row_view  = (LinearLayout)view;
				
				// Obtain the text of the row
				TextView txt_name = (TextView)row_view.findViewById(R.id.text1);

				// Clear the EditText box of any previous text
				dialog_search_term.setText("");

				if(txt_name.getText().equals("Title Search")){
					dialog.setTitle("Search by Book Title");
					dialog_search_title.setText("Enter Book title to be searched");
					dialog_example_text.setText("E.g. Moby Dick or Potter");
                    dialog_search_term.setContentDescription("Enter title");
					query_type = "Title Search";
					intent = new Intent(getApplicationContext(),Bookshare_Books_Listing.class);
					intent.putExtra("REQUEST_TYPE", "Title Search");
					dialog.show();
				}
				else if(txt_name.getText().equals("Author Search")){
					dialog.setTitle("Search by Author Name");
					dialog_search_title.setText("Enter author name to be searched");
					dialog_example_text.setText("E.g. Mark Twain or Lewis");
                    dialog_search_term.setContentDescription("Enter author name");
					query_type = "Author Search";
					intent = new Intent(getApplicationContext(),Bookshare_Books_Listing.class);
					intent.putExtra("REQUEST_TYPE","Author Search");
					dialog.show();
				}
				else if(txt_name.getText().equals("ISBN Search")){
					dialog.setTitle("Search by ISBN");
					dialog_search_title.setText("Enter 10 or 13 digit ISBN");
					dialog_example_text.setText("E.g. 9780670059218 or 9781416503064");
                    dialog_search_term.setContentDescription("Enter ISBN");
					query_type = "ISBN Search";
					intent = new Intent(getApplicationContext(),Bookshare_Book_Details.class);
					intent.putExtra("REQUEST_TYPE","ISBN Search");
					dialog.show();
				}
				else if(txt_name.getText().equals("Latest Books")){
	                // Changed the behavior to search for the latest book without having to enter the date range
					//dialog.setTitle("Search for latest books");
					//dialog_search_title.setText("Enter \"from\" date in MMDDYYYY format");
					//dialog_example_text.setText("E.g. 01012001 or 10022005");					
					if(isFree)
						search_term = URI_String+"latest?api_key="+developerKey;
					else
						search_term = URI_String+"latest/for/"+username+"?api_key="+developerKey;
					query_type = "Latest Books";
					intent = new Intent(getApplicationContext(),Bookshare_Books_Listing.class);
					intent.putExtra("REQUEST_TYPE","Latest Books");
					intent.putExtra("REQUEST_URI", search_term);
					if(!isFree){
						intent.putExtra("username", username);
						intent.putExtra("password", password);
					}
					startActivityForResult(intent, START_BOOKSHARE_BOOKS_LISTING_ACTIVITY);
				}
				
				// Option to search for popular books on Bookshare website
				else if(txt_name.getText().equals("Popular Books")){
					if(isFree)
						search_term = URI_String+"popular?api_key="+developerKey;
					else
						search_term = URI_String+"popular/for/"+username+"?api_key="+developerKey;
					query_type = "Popular Books";
					intent = new Intent(getApplicationContext(),Bookshare_Books_Listing.class);
					intent.putExtra("REQUEST_TYPE","Popular Books");
					intent.putExtra("REQUEST_URI", search_term);
					if(!isFree){
						intent.putExtra("username", username);
						intent.putExtra("password", password);
					}
					startActivityForResult(intent, START_BOOKSHARE_BOOKS_LISTING_ACTIVITY);
				}
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		if(isFree){
			menu.add("Log In");
		}
		else{
			menu.add("Log Out");
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getTitle().equals("Log Out")){
			new AlertDialog.Builder(this)
            .setTitle("")
            .setMessage("Log out?")
            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {					
					// Upon logout clear the stored login credentials
					SharedPreferences login = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					SharedPreferences.Editor editor = login.edit();
					editor.putString("username", "");
					editor.putString("password", "");
					editor.putBoolean("isOM", false);
					editor.commit();
					finish();
				}
			})
			.setNegativeButton("NO", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
            .show();
		}
		else if(item.getTitle().equals("Log In")){
			Intent intent = new Intent(getApplicationContext(), Bookshare_Webservice_Login.class);
			startActivity(intent);
			finish();
		}
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == START_BOOKSHARE_BOOKS_LISTING_ACTIVITY){
			if(resultCode == BOOKSHARE_BOOKS_LISTING_FINISHED){
				setResult(BOOKSHARE_MENU_FINISHED);
				finish();
			}
		}
	}
	
 	// A custom SimpleAdapter class for providing data to the ListView
	private class MySimpleAdapter extends SimpleAdapter{

	    public MySimpleAdapter(Context context, List<? extends Map<String, ?>> data,
                int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        /**
         * Retrieves view for the item in the adapter, at the
         * specified position and populates it with data.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.bookshare_menu_item, null);
            }
            final TreeMap<String, Object> data = (TreeMap<String, Object>) getItem(position);
            ((TextView) convertView.findViewById(R.id.text1)).setText((String) data.get("Name"));
            ((TextView) convertView.findViewById(R.id.text2)).setText((String) data.get("description"));
            ((ImageView) convertView.findViewById(R.id.row_icon))
                    .setImageResource(((Integer)data.get("icon")).intValue());
            return convertView;
        }
	}
}

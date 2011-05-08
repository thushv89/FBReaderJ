package org.geometerplus.android.fbreader.network.bookshare;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.bookshare.net.BookshareWebservice;
import org.geometerplus.zlibrary.ui.android.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Login page for the webservice account of Bookshare.
 * Webservice login is mandatory for accessing generally
 * available services of bookshare API. This includes
 * searching and downloading public domain books.
 */
public class Bookshare_Webservice_Login extends Activity{

	private String BOOKSHARE_URL = "https://api.bookshare.org/book/searchFTS/title/*potter*";
	private Button btn_login;
	private Button btn_reset;
	private Button btn_continue_without_login;
	private Button btn_free_content;
	private TextView text_username;
	private TextView text_password;
	private EditText editText_username;
	private EditText editText_password;
	private Intent intent;
	private final static int LOGIN_SUCCESSFUL = 1;
	private final static int LOGIN_FAILED = -1;
	private String username;
	private String password;
	private int status;
	private ProgressDialog pd_spinning;
	private boolean isFree= false;
	private String developerKey = BookshareDeveloperKey.DEVELOPER_KEY;
	private boolean isOM = false;
	private String response;
	

	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		// Obtain the SharedPreferences object shared across the application
		SharedPreferences login_preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		username = login_preference.getString("username", "");
		password = login_preference.getString("password", "");
		
		// If login credentials are already stored, navigate to the next Activity
		if(!username.equals("") && !password.equals("")){
			intent = new Intent(getApplicationContext(), Bookshare_Menu.class);
			intent.putExtra("username", username);
			intent.putExtra("password", password);
			startActivity(intent);
			finish();
		}
		
		// Remove the title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// Set to full screen
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.bookshare_webservice_login);

		btn_login = (Button)findViewById(R.id.btn_bookshare_bookshare_webservice_login);
		btn_reset = (Button)findViewById(R.id.btn_bookshare_bookshare_webservice_password);
		btn_continue_without_login = (Button)findViewById(R.id.btn_bookshare_bookshare_continue_without_login);
		
		text_username = (TextView)findViewById(R.id.bookshare_login_username_text);
		text_password = (TextView)findViewById(R.id.bookshare_login_password_text);
		editText_username = (EditText)findViewById(R.id.bookshare_login_username_edit_text);
		editText_password = (EditText)findViewById(R.id.bookshare_login_password_edit_text);

		// Listener for login button
		btn_login.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				
				// Hide the virtual keyboard
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(editText_username .getWindowToken(), 0);

				username = editText_username.getText().toString().trim();
				password = editText_password.getText().toString().trim();

				// Test for conditions where the input might be blank
				if(username.equals("") && password.equals("")){
					Toast t = Toast.makeText(getApplicationContext(),"Username/Password field cannot be blank!", Toast.LENGTH_SHORT);
					t.show();
				}
				else if(username.equals("") && !password.equals("")){
					Toast t = Toast.makeText(getApplicationContext(),"Username field cannot be blank!", Toast.LENGTH_SHORT);
					t.show();
				}
				else if(!username.equals("") && password.equals("")){
					Toast t = Toast.makeText(getApplicationContext(),"Password field cannot be blank!", Toast.LENGTH_SHORT);
					t.show();
				}
				else{
					startProgressDialog();
					// Start a new AsyncTask for background processing
					new AuthenticationTask().execute();
				}
			}
		});
		
		// Listener for reset button
		btn_reset.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				editText_username.setText("");
				editText_password.setText("");
				editText_username.requestFocus();
			}
		});
		
		btn_continue_without_login.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				getFreeContent();
			}
		});
	}
	
	private void getFreeContent(){
		isFree = true;
		isOM = false;
		username = null;
		password = null;
		
		if(isFree){
			pd_spinning = ProgressDialog.show(this, null, "Fetching free books data. Please wait.", Boolean.TRUE);
		}
		else{
			pd_spinning = ProgressDialog.show(this, null, "Authenticating. Please wait.", Boolean.TRUE);
		}

		// Start a new AsyncTask for background processing
		new AuthenticationTask().execute();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		
		menu.add(Menu.NONE,1,Menu.NONE,"Cancel");
		menu.add(Menu.NONE,2,Menu.NONE,"Free Content");
		return true;
	}
	
/*	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		// Toggle the entry depending on whether the login is for Individual Member or Organizational Member
		MenuItem item = menu.findItem(3);
		if(!isOM){
			item.setTitle("OM login");
		}
		else{
			item.setTitle("IM login");
		}
			
		return true;
	}
*/	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuitem){
		
		if(menuitem.getTitle().equals("Cancel")){
			finish();
		}

		// Toggle the entry depending on whether the login is for Individual Member or Organizational Member
/*		else if(menuitem.getTitle().equals("OM login")){
			text_username.setText("OM username");
			text_password.setText("OM password");
			menuitem.setTitle("IM login");
			isOM = true;
		}
		else if(menuitem.getTitle().equals("IM login")){
			text_username.setText("IM username");
			text_password.setText("IM password");
			menuitem.setTitle("OM login");
			isOM = false;
		}*/
		else if(menuitem.getTitle().equals("Free Content")){
			isFree = true;
			isOM = false;
			username = null;
			password = null;
			
			if(isFree){
				pd_spinning = ProgressDialog.show(this, null, "Fetching free books data. Please wait.", Boolean.TRUE);
			}
			else{
				pd_spinning = ProgressDialog.show(this, null, "Authenticating. Please wait.", Boolean.TRUE);
			}

			// Start a new AsyncTask for background processing
			new AuthenticationTask().execute();
		}
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
	}

	/*
	 * An AsyncTask class which carries out the authentication 
	 * in the background.
	 */
	private class AuthenticationTask extends AsyncTask<Void, Void, Void>{

		/*
		 * (non-Javadoc)
		 * This method is called in the UI thread just before the
		 * doInBackground is called. Disable the UI elements while
		 * the authentication is being done.
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute(){
			
			btn_login.setEnabled(false);
			btn_reset.setEnabled(false);
			Toast t;
			
			if(isFree){
				editText_username.setText("");
				editText_password.setText("");
				text_username.setText("");
				text_password.setText("");
			}
			editText_username.setEnabled(false);
			editText_password.setEnabled(false);
		}


		/*
		 * (non-Javadoc)
		 * The entire body of this method is executed in a 
		 * newly spawned thread. Carry out the actual authentication task here
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Void doInBackground(Void... params) {

			String result_HTML = "";

			try{
				
				// Get a BookshareWebservice instance for accessing the utility methods
				final BookshareWebservice bws = new BookshareWebservice();
				if(isFree){
					BOOKSHARE_URL = BOOKSHARE_URL + "?api_key="+developerKey;
				}
				else{
					BOOKSHARE_URL = "https://api.bookshare.org/user/preferences/list/for/"+username+"/?api_key="+developerKey;
				}
				System.out.println("BOOKSHARE_URL = "+BOOKSHARE_URL);
				InputStream inputStream = bws.getResponseStream(password, BOOKSHARE_URL);
				result_HTML = bws.convertStreamToString(inputStream);

				// Cleanup the HTML formatted tags
				response = result_HTML.replace("&apos;", "'").replace("&quot;", "\"").replace("&amp;", "&").replace("&#xd;","").replace("&#x97;", "-");

				//System.out.println(response);
			}
			catch(URISyntaxException use){
				System.out.println(use);
				Toast t = Toast.makeText(getApplicationContext(),"Network Error", Toast.LENGTH_SHORT);
				t.show();
			}
			catch(IOException ioe){
				System.out.println(ioe);
				Toast t = Toast.makeText(getApplicationContext(),"Network Error", Toast.LENGTH_SHORT);
				t.show();
			}

			// Authentication failed
			if(result_HTML.contains("<status-code>401</status-code>") || result_HTML.contains("<status-code>500</status-code>")
					|| result_HTML.contains("<status-code>403</status-code>") || result_HTML.contains("<status-code>404</status-code>")){
				
				System.out.println("LOGIN_FAILED");
				System.out.println(result_HTML);
				status = LOGIN_FAILED;
			}
			else{
				if(!isFree){
					Bookshare_UserType userTypeObj = new Bookshare_UserType();
					isOM = userTypeObj.isOM(response);
					if(isOM){
						String downloadPassword = userTypeObj.getDownloadPassword();
						if(downloadPassword == null){
							status = LOGIN_FAILED;
							return null;
						}
						
						SharedPreferences login_preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
						SharedPreferences.Editor editor = login_preference.edit();
						editor.putString("downloadPassword", downloadPassword);
						editor.commit();
					}
				}
				status = LOGIN_SUCCESSFUL;
			}
			return null;
		}
		
		/*
		 * (non-Javadoc)
		 * Called in the UI thread immediately after the
		 * doInBackground ends. Re-enable the UI elements.
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		public void onPostExecute(Void result){
			super.onPostExecute(result);

			btn_login.setEnabled(true);
			btn_reset.setEnabled(true);
			editText_username.setEnabled(true);
			editText_password.setEnabled(true);
			editText_username.requestFocus();
			if(pd_spinning != null)
				pd_spinning.cancel();

			switch(status){
			
			// Navigate to the next Activity
			case LOGIN_SUCCESSFUL:
				intent = new Intent(getApplicationContext(), Bookshare_Menu.class);
				
				if(!isFree){
					intent.putExtra("username", username);
					intent.putExtra("password", password);
				}
				
				// Obtain the application wide SharedPreferences object and store the login information
				SharedPreferences login_preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				SharedPreferences.Editor editor = login_preference.edit();
				editor.putString("username", username);
				editor.putString("password", password);
				editor.putBoolean("isOM", isOM);
				editor.commit();
				startActivity(intent);
				finish();
				break;
				
			// Give the failure notification and show the login screen
			case LOGIN_FAILED:
				BOOKSHARE_URL = "https://api.bookshare.org/book/searchFTS/title/*potter*";
				Toast toast = Toast.makeText(getApplicationContext(), "Login failed!", Toast.LENGTH_SHORT);
				toast.show();
				editText_username.setText("");
				editText_password.setText("");
				break;

			default:
				break;
			}
		}
	}
	private void startProgressDialog(){
		if(isFree){
			pd_spinning = ProgressDialog.show(this, null, "Fetching free books data. Please wait.", Boolean.TRUE);
		}
		else{
			pd_spinning = ProgressDialog.show(this, null, "Authenticating. Please wait.", Boolean.TRUE);
		}
	}

}

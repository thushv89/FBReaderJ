package org.geometerplus.android.fbreader.network.bookshare;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.bookshare.net.BookshareWebservice;
import org.geometerplus.zlibrary.ui.android.R;

import android.app.Activity;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Login page for the webservice account of Bookshare.
 * Webservice login is mandatory for accessing generally
 * available services of bookshare API. This includes
 * searching downloading public domain books.
 */
public class Bookshare_Webservice_Login extends Activity{

	private String BOOKSHARE_URL ="http://service.bookshare.org/";
	private Button btn_login;
	private Button btn_reset;
	private EditText editText_username;
	private EditText editText_password;
	private Intent intent;
	private final static int LOGIN_SUCCESSFUL = 1;
	private final static int LOGIN_FAILED = -1;
	private String ws_username;
	private String ws_password;
	private int status;

	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		// Obtain the SharedPreferences object shared across the application
		SharedPreferences login_preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		ws_username = login_preference.getString("username", "");
		ws_password = login_preference.getString("password", "");
		
		// If login credentials are already stored, navigate to the next Activity
		if(!ws_username.equals("") && !ws_password.equals("")){
			intent = new Intent(getApplicationContext(), Bookshare_Menu.class);
			intent.putExtra("ws_username", ws_username);
			intent.putExtra("ws_password", ws_password);
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

		editText_username = (EditText)findViewById(R.id.bookshare_webservice_login_username_edit_text);
		editText_password = (EditText)findViewById(R.id.bookshare_webservice_login_password_edit_text);
		
		// Listener for login button
		btn_login.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				
				// Hide the virtual keyboard
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(editText_username .getWindowToken(), 0);

				ws_username = editText_username.getText().toString().trim();
				ws_password = editText_password.getText().toString().trim();

				// Test for conditions where the inout might be blank
				if(ws_username.equals("") && ws_password.equals("")){
					Toast t = Toast.makeText(getApplicationContext(),"Username/Password field cannot be blank!", Toast.LENGTH_SHORT);
					t.show();
				}
				else if(ws_username.equals("") && !ws_password.equals("")){
					Toast t = Toast.makeText(getApplicationContext(),"Username field cannot be blank!", Toast.LENGTH_SHORT);
					t.show();
				}
				else if(!ws_username.equals("") && ws_password.equals("")){
					Toast t = Toast.makeText(getApplicationContext(),"Password field cannot be blank!", Toast.LENGTH_SHORT);
					t.show();
				}
				else{
					
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
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		
		MenuItem item = menu.add("Cancel");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuitem){
		
		if(menuitem.getTitle().equals("Cancel")){
			finish();
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
			editText_username.setEnabled(false);
			editText_password.setEnabled(false);

			Toast t = Toast.makeText(getApplicationContext(),"Authenticating, Please wait...", Toast.LENGTH_LONG);
			t.show();
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
				InputStream inputStream = bws.getResponseStream(ws_username, ws_password, BOOKSHARE_URL);
				result_HTML = bws.convertStreamToString(inputStream);
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
			
			// Authenticaiton failed
			if(result_HTML.contains("<status-code>401</status-code>")){
				status = LOGIN_FAILED;
			}
			else{
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

			switch(status){
			
			// Navigate to the next Activity
			case LOGIN_SUCCESSFUL:
				intent = new Intent(getApplicationContext(), Bookshare_Menu.class);
				intent.putExtra("ws_username", ws_username);
				intent.putExtra("ws_password", ws_password);
				
				// Obtain the application wide SharedPreferences object and store the login information
				SharedPreferences login_preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				SharedPreferences.Editor editor = login_preference.edit();
				editor.putString("username", ws_username);
				editor.putString("password", ws_password);
				editor.commit();
				startActivity(intent);
				finish();
				break;
				
			// Give the failure notification and show the login screen
			case LOGIN_FAILED:
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
}

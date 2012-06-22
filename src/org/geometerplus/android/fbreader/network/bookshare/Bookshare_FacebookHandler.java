package org.geometerplus.android.fbreader.network.bookshare;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;


public class Bookshare_FacebookHandler {

	private Activity authActivity;
	private Context context;
	SharedPreferences mPrefs;
	private Facebook fb;
	private String accessToken;
	private String api_key;
	public Bookshare_FacebookHandler(Activity activity){
		accessToken="AAACEdEose0cBAH35akufUO9ZBsFMRwYn1eKte955nMKyVnMLy7VWLdZCKKIjOFpzL28gfP0OUPKO8mxPCBcGQ0kmhj1bsidt2mPKFqRFfO05yxZCLCz";
		api_key="349676738412199";
		authActivity=activity;
		context=authActivity.getBaseContext();	
		fb=new Facebook(api_key);
	}
	
	public void ssoInitialAuth(){
		fb.authorize(authActivity, new DialogListener() {
			@Override
			public void onComplete(Bundle values) {
				
			}
				
			@Override
			public void onFacebookError(FacebookError error) {
	
				Toast.makeText(context, "Couldn't connect to Facebook", Toast.LENGTH_LONG).show();			}

			@Override
			public void onError(DialogError e) {
				Toast.makeText(context, "Something is awful happened.", Toast.LENGTH_LONG).show();			}

			@Override
			public void onCancel() {

				Toast.makeText(context, "Connection terminated forcibly", Toast.LENGTH_LONG).show();
			}
			

			
		});
	}
	
	// get permission to access users information. When the user provide permission
	// the application will automatically do the rest of the work
	public void getFBPermission(){
		fb.authorize(authActivity, new String[] { "publish_stream", "friends_online_presence"},

				new DialogListener() {
			@Override
			public void onComplete(Bundle values) {}

			@Override
			public void onFacebookError(FacebookError error) {}

			@Override
			public void onError(DialogError e) {}

			@Override
			public void onCancel() {
				
			}
		}
		);
	}
	
	//Access token is needed in order to give access to the program for what is called
	//a session
	public void getAccessToken(){
		mPrefs=authActivity.getPreferences(authActivity.MODE_PRIVATE);
		String accessToken=mPrefs.getString("access_token", null);
		long tokenExpires=mPrefs.getLong("access_expires", 0);
		if(accessToken != null) {
			fb.setAccessToken(accessToken);
		}
		if(tokenExpires != 0) {
			fb.setAccessExpires(tokenExpires);
		}

		if(!fb.isSessionValid()){
			fb.authorize(authActivity,new String[] {},new DialogListener() {

				@Override
				public void onFacebookError(FacebookError e) {
					Toast.makeText(context, "Request could not be fulfilled. Please try again.", Toast.LENGTH_LONG).show();
				}

				@Override
				public void onError(DialogError e) {
					Toast.makeText(context, "Error has occured.", Toast.LENGTH_LONG).show();
				}

				@Override
				public void onComplete(Bundle values) {
					SharedPreferences.Editor editor = mPrefs.edit();
					editor.putString("access_token",fb.getAccessToken());
					editor.putLong("access_expires", fb.getAccessExpires());
					editor.commit();
				}

				@Override
				public void onCancel() {

				}
			});
		}
	}
	
	public void postOnWall(){
		
		 Bundle parameters = new Bundle();
         parameters.putString("message", "Hi, I found this book very interesting. You might like it too.");// the message to post to the wall
         //parameters.put
         
         try {
			String response = fb.request("me/feed", parameters, "POST");
			if (response != null && !response.equals("") && 
                    !response.equals("false")) {
				Toast.makeText(context, "Successfully posted on your wall", Toast.LENGTH_SHORT).show();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

 
	}
	
}


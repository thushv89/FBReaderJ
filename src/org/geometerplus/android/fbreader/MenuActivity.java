package org.geometerplus.android.fbreader;

import java.util.ArrayList;
import java.util.List;

import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.ui.android.R;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MenuActivity extends Activity {
	private List<Object> listItems = new ArrayList<Object>(); 
	private ListItemsAdapter adapter = null; 
	static boolean show_day = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog);
        
        for ( int i = 0; i < 13; i++ ) {
			Object object = new Object();
			listItems.add(object);
        }
        
		final ListView list = (ListView) findViewById(R.id.list);
		adapter = new ListItemsAdapter(listItems);
		list.setAdapter(adapter);
		System.out.println("****** list.isInTouchMode()"+list.isInTouchMode());
		//Item click listener for the ListView
		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				System.out.println("*********position = "+position);
				if(position==0){
					ZLApplication.Instance().doAction(ActionCode.SPEAK);
				}
				else if(position == 1){
					ZLApplication.Instance().doAction(ActionCode.SHOW_LIBRARY);
					finish();
				}
				else if(position == 2){
					ZLApplication.Instance().doAction(ActionCode.SHOW_NETWORK_LIBRARY);
					finish();
				}
				else if(position == 3){
					ZLApplication.Instance().doAction(ActionCode.SHOW_CONTENTS);
					finish();
				}
				else if(position == 4){
					ZLApplication.Instance().doAction(ActionCode.SHOW_BOOKMARKS);
					finish();
				}
				else if(position == 5){
					if(show_day)
						ZLApplication.Instance().doAction(ActionCode.SWITCH_TO_DAY_PROFILE);
					else
						ZLApplication.Instance().doAction(ActionCode.SWITCH_TO_NIGHT_PROFILE);
					show_day = !show_day;
					finish();
				}
				else if(position == 6){
					ZLApplication.Instance().doAction(ActionCode.SEARCH);
					finish();
				}
				else if(position == 7){
					ZLApplication.Instance().doAction(ActionCode.SHOW_PREFERENCES);
					finish();
				}
				else if(position == 8){
					ZLApplication.Instance().doAction(ActionCode.SHOW_BOOK_INFO);
					finish();
				}
				else if(position == 9){
					ZLApplication.Instance().doAction(ActionCode.ROTATE);
					finish();
				}
				else if(position == 10){
					ZLApplication.Instance().doAction(ActionCode.INCREASE_FONT);
					finish();
				}
				else if(position == 11){
					ZLApplication.Instance().doAction(ActionCode.DECREASE_FONT);
					finish();
				}
				else if(position == 12){
					ZLApplication.Instance().doAction(ActionCode.SHOW_NAVIGATION);
					finish();
				}
			}
		});
    }
    
	private class ListItemsAdapter extends ArrayAdapter<Object> {
		public ListItemsAdapter(List<Object> items) {
			super(MenuActivity.this, android.R.layout.simple_list_item_1, items);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			LayoutInflater inflater = getLayoutInflater();
			convertView = inflater.inflate(R.layout.dialog_items, null);

			holder = new ViewHolder();
			holder.text = (TextView) convertView.findViewById(R.id.text);		

			convertView.setTag(holder);

			// Bind the data efficiently with the holder.
			holder.text.setText( "Item: " + (position+1) );
			final Typeface typeface;
			//holder.text.setTextSize( 20 );

			holder.text.setTypeface(Typeface.defaultFromStyle (Typeface.NORMAL));
			
			switch ( position ) {
			case 0:
				holder.text.setText("Speak");
				break;
			case 1:
				holder.text.setText("Library");
				break;
			case 2:
				holder.text.setText("Network Library");	
				break;
			case 3:
				holder.text.setText("Table of Contents");
				break;
			case 4:
				holder.text.setText("Bookmarks");
				break;
			case 5:
				if(show_day)
					holder.text.setText("Day View");
				else
					holder.text.setText("Night View");
				break;
			case 6:
				holder.text.setText("Search");
				break;
			case 7:
				holder.text.setText("Settings");
				break;
			case 8:
				holder.text.setText("Book Info");
				break;
			case 9:
				holder.text.setText("Rotate Screen");
				break;
			case 10:
				holder.text.setText("Zoom In");
				break;
			case 11:
				holder.text.setText("Zoom Out");
				break;
			case 12:
				holder.text.setText("Navigate");
				break;
			default:
				holder.text.setText("Search");
				break;
			}
			
			return convertView;
		}

		private class ViewHolder {
			TextView text;
		}
	}
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		if (keyCode == KeyEvent.KEYCODE_CAMERA) {
			return true;
        } else if (keyCode == KeyEvent.KEYCODE_CALL) {
        	return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			return true;
		}

		return false;
	}
	
}
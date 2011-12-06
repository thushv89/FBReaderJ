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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MenuActivity extends Activity {

    private List<Object> listItems = new ArrayList<Object>(); 
	private ListItemsAdapter adapter = null; 
	static boolean showDayViewOption = false;

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
		list.setOnItemClickListener(new MainMenuClickListener(this));
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
			holder.text.setText(MenuControl.values()[position].getLabel());
			return convertView;
		}

		private class ViewHolder {
			TextView text;
		}
	}

	private class MainMenuClickListener implements OnItemClickListener {
	    private final Activity activity;

        private MainMenuClickListener(final Activity activity) {
            this.activity = activity;
        }

        public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
            MenuControl.values()[position].click(activity);
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

    /**
     * Order of FBReader's main menu options. Each enum entry has an associated label and onClick operation.
     */
    private enum MenuControl {
	    
	    speak("Speak", new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SPEAK);
	        }
	    }),
	    tableOfContents("Table of Contents", new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_CONTENTS);
                activity.finish();
	        }
	    }),
	    navigate("Navigate to Page", new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_NAVIGATION);
                activity.finish();
	        }
	    }),
	    bookmarks("Bookmarks", new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_BOOKMARKS);
                activity.finish();
	        }
	    }),
	    search("Search", new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SEARCH);
                activity.finish();
	        }
	    }),
	    bookInfo("Book Info", new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_BOOK_INFO);
                activity.finish();
	        }
	    }),
	    library("Library", new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_LIBRARY);
                activity.finish();
	        }
	    }),
	    networkLibrary("Network Library", new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_NETWORK_LIBRARY);
                activity.finish();
	        }
	    }),
	    settings("Settings", new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_PREFERENCES);
                activity.finish();
	        }
	    }),
	    rotateScreen("Rotate Screen", new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.ROTATE);
                activity.finish();
	        }
	    }),
	    dayNightView(
            new HasLabel() {
                public String getLabel() {
                    return MenuActivity.showDayViewOption? "Day View": "Night View";
                }
            },
            new MenuOperation() {
    	        public void click(final Activity activity) {
                    if (MenuActivity.showDayViewOption) {
                        ZLApplication.Instance().doAction(ActionCode.SWITCH_TO_DAY_PROFILE);
                    } else {
                        ZLApplication.Instance().doAction(ActionCode.SWITCH_TO_NIGHT_PROFILE);
                    }
                    MenuActivity.showDayViewOption = !MenuActivity.showDayViewOption;
                    activity.finish();
    	        }
            }
	    ),
	    zoomIn("Zoom In", new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.INCREASE_FONT);
                activity.finish();
	        }
	    }),
	    zoomOut("Zoom Out", new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.DECREASE_FONT);
                activity.finish();
	        }
	    });

	    private final HasLabel hasLabel;
	    private final MenuOperation menuOperation;

	    private MenuControl(final String label, final MenuOperation menuOperation) {
            this.hasLabel = new HasLabel() {
                public String getLabel() {
                    return label;
                }
            };
            this.menuOperation = menuOperation;
        }
        private MenuControl(final HasLabel hasLabel, final MenuOperation menuOperation) {
            this.hasLabel = hasLabel;
            this.menuOperation = menuOperation;
        }
        public String getLabel() {
            return this.hasLabel.getLabel();
        }
        public void click(final Activity menuActivity) {
            this.menuOperation.click(menuActivity);
        }
	}
	
	private interface MenuOperation {
	    public void click(Activity activity);
	}

    private interface HasLabel {
        public String getLabel();
    }

}
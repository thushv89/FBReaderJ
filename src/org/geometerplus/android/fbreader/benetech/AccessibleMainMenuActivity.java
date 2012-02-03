package org.geometerplus.android.fbreader.benetech;

import java.util.ArrayList;
import java.util.List;

import org.benetech.android.R;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.zlibrary.core.application.ZLApplication;

import android.app.Activity;
import android.content.res.Resources;
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
import android.widget.ListView;
import android.widget.TextView;

public class AccessibleMainMenuActivity extends Activity {

    private List<Object> listItems = new ArrayList<Object>();
    private ListView list;
    private static Resources resources;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog);
        resources = getApplicationContext().getResources();

        int menuItemLimit = 10;

        for ( int i = 0; i < menuItemLimit; i++ ) {
			Object object = new Object();
			listItems.add(object);
        }        
		list = (ListView) findViewById(R.id.list);
        ListItemsAdapter adapter = new ListItemsAdapter(listItems);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new MainMenuClickListener(this));
    }

	private class ListItemsAdapter extends ArrayAdapter<Object> {
		public ListItemsAdapter(List<Object> items) {
			super(AccessibleMainMenuActivity.this, android.R.layout.simple_list_item_1, items);
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
	    
	    speak(resources.getString(R.string.menu_speak), new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SPEAK);
                activity.finish();
	        }
	    }),
        bookshare(resources.getString(R.string.menu_bookshare), new MenuOperation() {
            public void click(Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.BOOKSHARE);
            }
        }),
        networkLibrary(resources.getString(R.string.menu_network_library), new MenuOperation() {
        	        public void click(final Activity activity) {
                        ZLApplication.Instance().doAction(ActionCode.SHOW_NETWORK_LIBRARY);
                        activity.finish();
        	        }
        	    }),
	    tableOfContents(resources.getString(R.string.menu_toc), new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_TOC);
                activity.finish();
	        }
	    }),
	    navigate(resources.getString(R.string.menu_navigate), new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_NAVIGATION);
                activity.finish();
	        }
	    }),
	    bookmarks(resources.getString(R.string.menu_bookmarks), new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_BOOKMARKS);
                activity.finish();
	        }
	    }),
	    search(resources.getString(R.string.menu_search), new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SEARCH);
                activity.finish();
	        }
	    }),
	    bookInfo(resources.getString(R.string.menu_book_info), new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_BOOK_INFO);
                activity.finish();
	        }
	    }),
	    library(resources.getString(R.string.menu_library), new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_LIBRARY);
                activity.finish();
	        }
	    }),

	    settings(resources.getString(R.string.menu_settings), new MenuOperation() {
	        public void click(final Activity activity) {
                ZLApplication.Instance().doAction(ActionCode.SHOW_PREFERENCES);
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
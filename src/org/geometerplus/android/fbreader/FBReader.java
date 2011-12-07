/*
 * Copyright (C) 2009-2010 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader;



import java.util.LinkedList;
import java.util.List;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidActivity;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public final class FBReader extends ZLAndroidActivity implements OnGestureListener, OnDoubleTapListener	 {
	static FBReader Instance;
	
	//Added for the detecting whether the talkback is on
	private final static String SCREENREADER_INTENT_ACTION = "android.accessibilityservice.AccessibilityService";
    private final static String SCREENREADER_INTENT_CATEGORY = "android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_SPOKEN";

	private int count = 0;
	private Dialog dialog;
	private EditText dialog_search_term;
	private TextView dialog_search_title;
	private TextView dialog_example_text;
	
//	private Speech speech;

	private int myFullScreenFlag;

	private static class NavigationButtonPanel extends ControlButtonPanel {
		public volatile boolean NavigateDragging;
		public ZLTextPosition StartPosition;

		@Override
		public void onShow() {
			if (FBReader.Instance != null && myControlPanel != null) {
				FBReader.Instance.setupNavigation(myControlPanel);
			}
		}

		@Override
		public void updateStates() {
			super.updateStates();
			if (!NavigateDragging && FBReader.Instance != null && myControlPanel != null) {
				FBReader.Instance.setupNavigation(myControlPanel);
			}
		}
	}

	private static class TextSearchButtonPanel extends ControlButtonPanel {
		@Override
		public void onHide() {
			final ZLTextView textView = (ZLTextView) ZLApplication.Instance().getCurrentView();
			textView.clearFindResults();
		}
	}

	private static TextSearchButtonPanel myTextSearchPanel;
	private static NavigationButtonPanel myNavigatePanel;

	@Override
	public void onCreate(Bundle icicle) {
		try
		{
			super.onCreate(icicle);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		/*
		android.telephony.TelephonyManager tele =
			(android.telephony.TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		System.err.println(tele.getNetworkOperator());
		*/
		Instance = this;
		final ZLAndroidApplication application = ZLAndroidApplication.Instance();
		myFullScreenFlag =
			application.ShowStatusBarOption.getValue() ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
		/*
		 * Added to make the app go non-fullscreen for barnes and noble testing 
		 */
		myFullScreenFlag = 0;
		getWindow().setFlags(
			WindowManager.LayoutParams.FLAG_FULLSCREEN, myFullScreenFlag
		);
		if (myTextSearchPanel == null) {
			myTextSearchPanel = new TextSearchButtonPanel();
			myTextSearchPanel.register();
		}
		if (myNavigatePanel == null) {
			myNavigatePanel = new NavigationButtonPanel();
			myNavigatePanel.register();
		}
	}
	boolean menuFlag;

	private boolean isScreenReaderActive() {
        // Restrict the set of intents to only accessibility services that have
        // the category FEEDBACK_SPOKEN (aka, screen readers).
        Intent screenReaderIntent = new Intent(SCREENREADER_INTENT_ACTION);
        screenReaderIntent.addCategory(SCREENREADER_INTENT_CATEGORY);
        List<ResolveInfo> screenReaders = getPackageManager().queryIntentServices(
                screenReaderIntent, 0);
        ContentResolver cr = getContentResolver();
        Cursor cursor = null;
        int status = 0;
        for (ResolveInfo screenReader : screenReaders) {
            // All screen readers are expected to implement a content provider
            // that responds to
            // content://<nameofpackage>.providers.StatusProvider
            cursor = cr.query(Uri.parse("content://" + screenReader.serviceInfo.packageName
                    + ".providers.StatusProvider"), null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                // These content providers use a special cursor that only has one element, 
                // an integer that is 1 if the screen reader is running.
                status = cursor.getInt(0);
                cursor.close();
                if (status == 1) {
                    return true;
                }
            }
        }
        return false;
    }
	
	/*
	 * Process Menu key event
	 * @see org.geometerplus.zlibrary.ui.android.library.ZLAndroidActivity#onKeyDown(int, android.view.KeyEvent)
	 * This method has been overridden to show a full screen menu when the menu button on the device is clicked
	 * instead of the menu shown at the bottom of the screen. Comment this method to show the regular menu.
	*/
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		System.out.println("************ Inside onKeyDown");
		if(keyCode == KeyEvent.KEYCODE_MENU){
			System.out.println("****** keyCode == KeyEvent.KEYCODE_MENU:");
	   		Intent i = new Intent(this, MenuActivity.class);
	   		startActivity(i);
		}

		if(isScreenReaderActive()){
			if (keyCode == KeyEvent.KEYCODE_MENU) {
				if(!menuFlag){
	    	   		System.out.println("******* Before starting the MenuActivity");
//	    	   		Intent i = new Intent(this, MenuActivity.class);
//	    	   		startActivity(i);
	    	   	}
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onStart() {
		super.onStart();
//		speech = new Speech(this);
		final ZLAndroidApplication application = ZLAndroidApplication.Instance();

		/*
		 * Added to make the app go non-fullscreen for barnes and noble testing 
		 */
		//final int fullScreenFlag =
		//	application.ShowStatusBarOption.getValue() ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
		final int fullScreenFlag = 0;

		if (fullScreenFlag != myFullScreenFlag) 
		{
			finish();
			startActivity(new Intent(this, this.getClass()));
		}

		final RelativeLayout root = (RelativeLayout)FBReader.this.findViewById(R.id.root_view);
		if (!myTextSearchPanel.hasControlPanel()) {
			final ControlPanel panel = new ControlPanel(this);

			panel.addButton(ActionCode.FIND_PREVIOUS, false, R.drawable.text_search_previous);
			panel.addButton(ActionCode.CLEAR_FIND_RESULTS, true, R.drawable.text_search_close);
			panel.addButton(ActionCode.FIND_NEXT, false, R.drawable.text_search_next);

			myTextSearchPanel.setControlPanel(panel, root, false);
		}
		if (!myNavigatePanel.hasControlPanel()) {
			final ControlPanel panel = new ControlPanel(this);
			final View layout = getLayoutInflater().inflate(R.layout.navigate, panel, false);
			
			createNavigation(layout);
			//speech.processView(layout);
			panel.setExtension(layout);
			myNavigatePanel.setControlPanel(panel, root, true);
		}
		findViewById(R.id.main_view).setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				if (!myNavigatePanel.getVisibility()) {
					navigate();
					return true;
				}
				return false;
			}
		});
	}

	private PowerManager.WakeLock myWakeLock;

	@Override
	public void onResume() {
		super.onResume();
		count=0;	
		ControlButtonPanel.restoreVisibilities();
		if (ZLAndroidApplication.Instance().DontTurnScreenOffOption.getValue()) {
			myWakeLock =
				((PowerManager)getSystemService(POWER_SERVICE)).
					newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "FBReader");
			myWakeLock.acquire();
		} else {
			myWakeLock = null;
		}
	}

	@Override
	public void onPause() {
		if (myWakeLock != null) {
			myWakeLock.release();
			myWakeLock = null;
		}
		ControlButtonPanel.saveVisibilities();
		super.onPause();
	}

	@Override
	public void onStop() {
		ControlButtonPanel.removeControlPanels();
		super.onStop();
	}

	void showTextSearchControls(boolean show) {
		if (show) {
			myTextSearchPanel.show(true);
		} else {
			myTextSearchPanel.hide(false);
		}
	}

	protected ZLApplication createApplication(String fileName) {
		new SQLiteBooksDatabase();
		String[] args = (fileName != null) ? new String[] { fileName } : new String[0];
		return new org.geometerplus.fbreader.fbreader.FBReader(args);
	}

	@Override
	public boolean onSearchRequested() {
		final LinkedList<Boolean> visibilities = new LinkedList<Boolean>();
		ControlButtonPanel.saveVisibilitiesTo(visibilities);
		ControlButtonPanel.hideAllPendingNotify();
		final SearchManager manager = (SearchManager)getSystemService(SEARCH_SERVICE);
		manager.setOnCancelListener(new SearchManager.OnCancelListener() {
			public void onCancel() {
				ControlButtonPanel.restoreVisibilitiesFrom(visibilities);
				manager.setOnCancelListener(null);
			}
		});
		final org.geometerplus.fbreader.fbreader.FBReader fbreader =
			(org.geometerplus.fbreader.fbreader.FBReader)ZLApplication.Instance();
		startSearch(fbreader.TextSearchPatternOption.getValue(), true, null, false);
		return true;
	}
	// Method to navigate to the specified page in the book
	private void navigateByPage(int page){
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view instanceof ZLTextView) {
			ZLTextView textView = (ZLTextView) view;
			if (page == 1) {
				textView.gotoHome();
			}
			else{
				textView.gotoPage(page);
			}
			ZLApplication.Instance().repaintView();
		}
	}
	
	public void navigate(){
		AccessibilityManager accessibilityManager =
	        (AccessibilityManager) getApplicationContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
		if(accessibilityManager.isEnabled()){
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.bookshare_dialog);
			dialog.setTitle("Navigate to page.");
			dialog_search_term = (EditText)dialog.findViewById(R.id.bookshare_dialog_search_edit_txt);
			dialog_search_title = (TextView)dialog.findViewById(R.id.bookshare_dialog_search_txt);
			dialog_example_text = (TextView)dialog.findViewById(R.id.bookshare_dialog_search_example);
			Button dialog_ok = (Button)dialog.findViewById(R.id.bookshare_dialog_btn_ok);
			Button dialog_cancel = (Button)dialog.findViewById(R.id.bookshare_dialog_btn_cancel);
			final ZLTextView textView = (ZLTextView) ZLApplication.Instance().getCurrentView();
			final int currentPage = textView.computeCurrentPage();
			final int pagesNumber = textView.computePageNumber();
			dialog_search_title.setText("Page number.");
			dialog_example_text.setText("Current page = "+currentPage+", Total pages = "+pagesNumber);
			dialog_search_term.setOnKeyListener(new OnKeyListener() {
			    public boolean onKey(View v, int keyCode, KeyEvent event) {
			        // If the event is a key-down event on the "enter" button
			        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
			            (keyCode == KeyEvent.KEYCODE_ENTER)) {
			          // Perform action on key press
						int page = 1;
						try{
							page = Integer.parseInt(dialog_search_term.getText().toString().trim());
						}
						catch(NumberFormatException nfe){
							dialog.dismiss();
							return true;
						}
						navigateByPage(page);
						dialog.dismiss();
			          return true;
			        }
			        return false;
			    }
			});
			dialog_ok.setOnClickListener(new OnClickListener(){
				public void onClick(View v){
					int page = 1;
					try{
						page = Integer.parseInt(dialog_search_term.getText().toString().trim());
					}
					catch(NumberFormatException nfe){
						dialog.dismiss();
						return;
					}
					navigateByPage(page);
					dialog.dismiss();
				}
			});
			dialog_cancel.setOnClickListener(new OnClickListener(){
				public void onClick(View v){
					dialog.dismiss();
				}
			});
			dialog.show();
		}
		else{
			final ZLTextView textView = (ZLTextView) ZLApplication.Instance().getCurrentView();
			myNavigatePanel.NavigateDragging = false;
			myNavigatePanel.StartPosition = new ZLTextFixedPosition(textView.getStartCursor());
			myNavigatePanel.show(true);
		}
	}
		
	public final boolean canNavigate() {
		final org.geometerplus.fbreader.fbreader.FBReader fbreader =
			(org.geometerplus.fbreader.fbreader.FBReader)ZLApplication.Instance();
		final ZLView view = fbreader.getCurrentView();
		if (!(view instanceof ZLTextView)) {
			return false;
		}
		final ZLTextModel textModel = ((ZLTextView) view).getModel();
		if (textModel == null || textModel.getParagraphsNumber() == 0) {
			return false;
		}
		final BookModel bookModel = fbreader.Model;
		return bookModel != null && bookModel.Book != null;
	}

	private final void createNavigation(View layout) {
		final SeekBar slider = (SeekBar) layout.findViewById(R.id.book_position_slider);
		final TextView text = (TextView) layout.findViewById(R.id.book_position_text);

		slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			private void gotoPage(int page) {
				final ZLView view = ZLApplication.Instance().getCurrentView();
				if (view instanceof ZLTextView) {
					ZLTextView textView = (ZLTextView) view;					
					if (page == 1) {
						textView.gotoHome();
					} else {
						textView.gotoPage(page);
					}
					ZLApplication.Instance().repaintView();
				}
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				myNavigatePanel.NavigateDragging = false;
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				myNavigatePanel.NavigateDragging = true;
			}

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					final int page = progress + 1;
					final int pagesNumber = seekBar.getMax() + 1; 
					text.setText(makeProgressText(page, pagesNumber));
					gotoPage(page);
				}
			}
		});

		final Button btnOk = (Button) layout.findViewById(android.R.id.button1);
		final Button btnCancel = (Button) layout.findViewById(android.R.id.button3);
		View.OnClickListener listener = new View.OnClickListener() {
			public void onClick(View v) {
				final ZLTextPosition position = myNavigatePanel.StartPosition;
				myNavigatePanel.StartPosition = null;
				if (v == btnCancel && position != null) {
					((ZLTextView) ZLApplication.Instance().getCurrentView()).gotoPosition(position);
				}
				myNavigatePanel.hide(true);
			}
		};
		btnOk.setOnClickListener(listener);
		btnCancel.setOnClickListener(listener);
		final ZLResource buttonResource = ZLResource.resource("dialog").getResource("button");
		btnOk.setText("Ok");//buttonResource.getResource("ok").getValue());
		btnCancel.setText("Cancel");//buttonResource.getResource("cancel").getValue());
	}

	private final void setupNavigation(ControlPanel panel) {
		final SeekBar slider = (SeekBar) panel.findViewById(R.id.book_position_slider);
		final TextView text = (TextView) panel.findViewById(R.id.book_position_text);

		final ZLTextView textView = (ZLTextView) ZLApplication.Instance().getCurrentView();
		final int page = textView.computeCurrentPage();
		final int pagesNumber = textView.computePageNumber();

		if (slider.getMax() != (pagesNumber - 1)
				|| slider.getProgress() != (page - 1)) {
			slider.setMax(pagesNumber - 1);
			slider.setProgress(page - 1);
			text.setText(makeProgressText(page, pagesNumber));
		}
	}

	private static String makeProgressText(int page, int pagesNumber) {
		return "" + page + " / " + pagesNumber;
	}

	@Override
	public boolean onDoubleTap(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) 
	{
	//	speech.speak("tap tap");
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) 
	{
	//	speech.speak("tap");
		return false;
	}

	@Override
	public boolean onDown(MotionEvent arg0) 
	{
		return false;
	}

	@Override
	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) 
	{
		return false;
	}

	@Override
	public void onLongPress(MotionEvent arg0) 
	{
	}

	@Override
	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) 
	{
		return false;
	}

	@Override
	public void onShowPress(MotionEvent arg0) 
	{
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent arg0) 
	{
		return false;
	}
	
	
}

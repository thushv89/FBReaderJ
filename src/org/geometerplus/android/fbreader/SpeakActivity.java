package org.geometerplus.android.fbreader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Menu;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReader;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.text.view.ZLTextElement;
import org.geometerplus.zlibrary.text.view.ZLTextParagraphCursor;
import org.geometerplus.zlibrary.text.view.ZLTextWord;
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor;
import org.geometerplus.zlibrary.ui.android.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;


// This class is used to compile for the non TTS version (regular). It contains the ImageButtons for TTS player controls
//public class SpeakActivity_nonTTS extends Activity implements OnInitListener, OnUtteranceCompletedListener {
public class SpeakActivity extends Activity implements OnInitListener, OnUtteranceCompletedListener{
    	static final int ACTIVE = 1;
    	static final int INACTIVE = 0;
	private static final int CHECK_TTS_INSTALLED = 0;
	private static final String PARAGRAPHUTTERANCE="PARAGRAPHUTTERANCE";

	static final int CURRENTORFORWARD = 0;
	static final int SEARCHFORWARD = 1;
	static final int SEARCHBACKWARD = 2;
		
    private TextToSpeech mTts=null;
    private FBView theView;
    private FBReader Reader; 
    private ZLTextParagraphCursor myParaCursor;
    
    private ImageButton pausebutton;
    private ImageButton forwardbutton;
    private ImageButton backbutton;
    
    private Button contentsButton;

    private ArrayList<String> sentenceList;
	private Iterator<String> sentenceListIterator;

    private int state = INACTIVE;
	private int lastSentence = 0;
	private int lastSpoken = 0;
	private boolean fromPause = false;
    private Activity activity;

    	class UpdateControls implements Runnable { 
    		private int buttonstate;
    		static final int PAUSE = 0;
    		static final int PLAY = 1;
			
		public void run() { 
			if(buttonstate==PLAY) { 
				pausebutton.setImageResource(R.drawable.speak_play);
					//pausebutton.setText("Play");
			} else if (buttonstate==PAUSE){
				pausebutton.setImageResource(R.drawable.speak_pause);
					//pausebutton.setText("Pause");
			}
		}
    		public UpdateControls(int value) { this.buttonstate = value; }
    	} 
    	
    	
    	private PhoneStateListener mPhoneListener = new PhoneStateListener()
    	{
    	        public void onCallStateChanged(int callState, String incomingNumber)
    	        {
    	        	if(callState == TelephonyManager.CALL_STATE_RINGING) {
    	        		stopTalking();
    	        		finish();
    	        	}
    	        }
    	};
    	
    	private OnClickListener forwardListener = new OnClickListener() {
    	    public void onClick(View v) {
    	    	stopTalking();
    	    	speakStringQueueFlush("FORWARD");
     	    	setState(INACTIVE);
  	      	    nextParagraph(SEARCHFORWARD);
    	    }
    	};

    	private OnClickListener backListener = new OnClickListener() {
    	    public void onClick(View v) {
    	    	stopTalking();
    	    	speakStringQueueFlush("BACK");
   	     	    setState(INACTIVE);
    	    	nextParagraph(SEARCHBACKWARD);
    	    }
    	};
    
        private OnClickListener contentsListener = new OnClickListener() {
            public void onClick(View view) {
                stopTalking();
                speakStringQueueFlush("TABLE OF CONTENTS");
                setState(INACTIVE);
                ZLApplication.Instance().doAction(ActionCode.SHOW_CONTENTS);
                activity.finish();
            }
        };
    	
    	private OnClickListener pauseListener = new OnClickListener() {
    	    public void onClick(View v) {

     	       if(state==ACTIVE){
     	    	  stopTalking(); 
     	    	  speakStringQueueFlush("PAUSE");
     	    	  fromPause = true;
     	    	  setState(INACTIVE);
     	      } else {
     	    	  setState(ACTIVE);
     	    	  speakStringQueueFlush("PLAY");
     	    	  nextParagraph(CURRENTORFORWARD);
     	      }
    	    }
    	};
 
    	private OnClickListener stopListener = new OnClickListener() {
    	    public void onClick(View v) {
    	    	stopTalking();
    	    	finish();
    	    }
    	};
    	
	   @Override
	   public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);     
	       
    		Thread.setDefaultUncaughtExceptionHandler(new org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this));
    		Reader = (FBReader)ZLApplication.Instance();
	       
	       //requestWindowFeature(Window.FEATURE_NO_TITLE);
	       setContentView(R.layout.view_spokentext);
	       
	       backbutton = (ImageButton)findViewById(R.id.spokentextback);
	       backbutton.setOnClickListener(backListener);
	       	       
	       forwardbutton = (ImageButton)findViewById(R.id.spokentextforward);
	       forwardbutton.setOnClickListener(forwardListener);
	       
	       pausebutton = (ImageButton)findViewById(R.id.spokentextpause);
	       pausebutton.setOnClickListener(pauseListener);
           
           contentsButton = (Button)findViewById(R.id.spokentextcontents);
           contentsButton.setOnClickListener(contentsListener);
	       
	       setState(INACTIVE);
	       	       
	       TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
	       tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
	       
 	       theView = ((FBReader)FBReader.Instance()).getTextView();
 		   
		   ZLTextWordCursor cursor = theView.getStartCursor();
		   myParaCursor = cursor.getParagraphCursor(); 
	       
	       Intent checkIntent = new Intent();
	       checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
	       startActivityForResult(checkIntent, CHECK_TTS_INSTALLED);
           pausebutton.requestFocus();
           activity = this;
	   }

    @Override
    protected void onStart() {
        super.onStart();
        pausebutton.requestFocus();
    }

    protected void onActivityResult(
	           int requestCode, int resultCode, Intent data) {
	       if (requestCode == CHECK_TTS_INSTALLED) {
	    	    
	    	     switch (resultCode) {
	    	     case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
	    	          mTts = new TextToSpeech(this, this);
	    	          break;
	    	     case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA:
	    	     case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA:
	    	     case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME:
	                  Intent installIntent = new Intent();
	                  installIntent.setAction(
	                  TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
	                  startActivity(installIntent);
	    	          break;
	    	     case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:
	    	     default:
	    	     }
	       }
	   }
	   
	   
// ZLTextWord cursor will navigate on a per-paragraph basis. 
// We have to split the paragraph into sentences. 
// Look for . at end of word
	private void getParagraphText(ZLTextParagraphCursor paraCursor) {
		StringBuilder sb = new StringBuilder();
	    	boolean inSentence = true;

		sentenceList = new ArrayList<String>();                      // clears out list, old list gets garbage collected

		ZLTextWordCursor cursor = new ZLTextWordCursor(paraCursor);

		while(!cursor.isEndOfParagraph()) { 
			ZLTextElement element = cursor.getElement();
			while (inSentence)  {
  			    if(element instanceof ZLTextWord) {
  			    	if (element.toString().indexOf(".") == (element.toString().length() -1) ) {           // detects period at end of element
   			    	   sb.append(element.toString().substring(0,element.toString().indexOf(".")));        // remove period	
  			    	   inSentence = false;
   			        } else {
                          sb.append(element.toString()).append(" ");
   			        }
  			    }
			    cursor.nextWord();	
			    if (cursor.isEndOfParagraph())
			    	break;
			    element = cursor.getElement();
			}

			sentenceList.add(sb.toString());              // arrayList of sentences

			sb.setLength(0);                             // reset stringbuilder
			inSentence = true;
		} 				
	    sentenceListIterator = sentenceList.iterator();     // set the iterator

	}
	

	private void setState(int value){
		state = value;
		
		if (state==ACTIVE) {
			pausebutton.post(new UpdateControls(UpdateControls.PAUSE));			 
		} else if (state==INACTIVE) {
			pausebutton.post(new UpdateControls(UpdateControls.PLAY));			 
		}
	}
	
	
	private void speakStringQueueFlush(String s){
		HashMap<String, String> callbackMap = new HashMap<String, String>();
		callbackMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,PARAGRAPHUTTERANCE);

		mTts.speak(s, TextToSpeech.QUEUE_FLUSH, callbackMap);			
	}
	
	private void loadSpeechEngine(){
        	String spkString;
        	int sentenceNumber = 0;

		if (fromPause) {                    // on returning from pause, iterate to the last sentence spoken
        		fromPause = false;
        		for (int i=1; i< lastSpoken; i++) {
    				sentenceListIterator.next();
        		}
        	}
		while (sentenceListIterator.hasNext())  { 	// if there are sentences in the sentence queue
            		sentenceNumber++;
			spkString = sentenceListIterator.next();

	     		HashMap<String, String> callbackMap = new HashMap<String, String>();
			callbackMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,Integer.toString(sentenceNumber));
		    	mTts.speak(spkString, TextToSpeech.QUEUE_ADD, callbackMap);	
		}
		
		lastSentence = sentenceNumber;
	}


    private void nextParagraph(int direction){
		ZLTextParagraphCursor localParaCursor;
       		boolean atLimit = false;

 		if (!(myParaCursor==null) && !atLimit){			
			switch (direction) {
			case SEARCHFORWARD:
				localParaCursor = myParaCursor.next();           // deal with the null pointer
				if (localParaCursor != null)  {
 				    myParaCursor = localParaCursor;              
				} else {
				    atLimit = true;
				    setState(INACTIVE);
				}
				break;
			case SEARCHBACKWARD:
				localParaCursor = myParaCursor.previous();      
				if (localParaCursor != null)  {
 				    myParaCursor = localParaCursor;                  
				} else {
				    atLimit = true;
				}
  				break;
			case CURRENTORFORWARD:				
				direction = SEARCHFORWARD;
  				break;
			}
			if ((!atLimit) && state == ACTIVE) {			
				getParagraphText(myParaCursor);
				loadSpeechEngine();
			}
		}	
	}
	
	
	@Override
	protected void  onDestroy() {	
		Reader.onWindowClosing(); // save the position
		setState(INACTIVE);
		mTts.shutdown();
		super.onDestroy();				
	}
	
	private void stopTalking() {
		setState(INACTIVE);
		if(mTts!=null){
		    mTts.stop();
		}
	}
	
	@Override
	protected void  onPause() {
		Reader.onWindowClosing(); // save the position
		super.onPause();
	}
	
	@Override
	public void  onBackPressed() {
		stopTalking();
		super.onBackPressed();
	}
	
	@Override
	protected void  onResume(){			
		super.onResume();
        pausebutton.requestFocus();
	}

	
//	@Override
	public void onInit(int status) {
		mTts.setOnUtteranceCompletedListener(this);
		setState(INACTIVE);
//		nextParagraphString(SEARCHFORWARD);
	}
	
	public void onUtteranceCompleted(String uttId) {
		String lastSentenceID = Integer.toString(lastSentence);
		if(state == ACTIVE && uttId.equals(lastSentenceID)) {
			 nextParagraph(SEARCHFORWARD);                        // nextParagraph can change sentenceListIterator
         	} else {
        		lastSpoken = Integer.parseInt(uttId);                // get last spoken id
         	}
	}

    /*
     * Process Menu key event
     * @see org.geometerplus.zlibrary.ui.android.library.ZLAndroidActivity#onKeyDown(int, android.view.KeyEvent)
     * This method has been overridden to show a full screen menu when the menu button on the device is clicked
     * instead of the menu shown at the bottom of the screen. Comment this method to show the regular menu.
    */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU){
            Intent i = new Intent(this, MenuActivity.class);
            startActivity(i);
        }

        return super.onKeyDown(keyCode, event);
    }

}
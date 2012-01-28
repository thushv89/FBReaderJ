package org.geometerplus.android.fbreader.benetech;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.accessibility.SimpleGestureFilter;
import org.geometerplus.android.fbreader.TOCActivity;
import org.geometerplus.android.fbreader.api.ApiServerImplementation;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.Library;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.text.view.ZLTextElement;
import org.geometerplus.zlibrary.text.view.ZLTextParagraphCursor;
import org.geometerplus.zlibrary.text.view.ZLTextWord;
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor;
import org.benetech.android.R;

public class SpeakActivity extends Activity implements OnInitListener, OnUtteranceCompletedListener, SimpleGestureFilter.SimpleGestureListener {
    private ApiServerImplementation myApi;

    static final int ACTIVE = 1;
    static final int INACTIVE = 0;
	private static final int CHECK_TTS_INSTALLED = 0;
    private static final int PLAY_AFTER_TOC = 1;
	private static final String PARAGRAPHUTTERANCE="PARAGRAPHUTTERANCE";

	static final int CURRENTORFORWARD = 0;
	static final int SEARCHFORWARD = 1;
	static final int SEARCHBACKWARD = 2;
		
    private TextToSpeech myTTS =null;
    private FBReaderApp Reader;
    private ZLTextParagraphCursor myParaCursor;
    
    private Button pausebutton;

    private Iterator<String> sentenceListIterator;

    private int state = INACTIVE;
	private int lastSentence = 0;
	private int lastSpoken = 0;
	private boolean fromPause = false;
    private Activity activity;
    private boolean resumePlaying = false;
    private Resources resources;
    private SimpleGestureFilter detector;
    private Vibrator myVib;


    private void setListener(int id, View.OnClickListener listener) {
        findViewById(id).setOnClickListener(listener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_spokentext);

        WindowManager.LayoutParams params =
        getWindow().getAttributes();
        params.gravity = Gravity.BOTTOM;
        this.getWindow().setAttributes(params);
        detector = new SimpleGestureFilter(this,this);
        myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        Reader = (FBReaderApp)ZLApplication.Instance();
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        setListener(R.id.spokentextback, new View.OnClickListener() {
            public void onClick(View v) {
                goBackward();
            }
        });

        final Button forwardbutton = (Button) findViewById(R.id.spokentextforward);
        forwardbutton.setOnClickListener(forwardListener);

        pausebutton = (Button)findViewById(R.id.spokentextpause);
        pausebutton.setOnClickListener(pauseListener);

        final Button contentsButton = (Button) findViewById(R.id.spokentextcontents);
        contentsButton.setOnClickListener(contentsListener);

        setState(INACTIVE);

        ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).listen(
            new PhoneStateListener() {
                public void onCallStateChanged(int state, String incomingNumber) {
                    if (state == TelephonyManager.CALL_STATE_RINGING) {
                        stopTalking();
                    }
                }
            },
            PhoneStateListener.LISTEN_CALL_STATE
        );

        myApi = new ApiServerImplementation();
        try {
            startActivityForResult(
                new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA), CHECK_TTS_INSTALLED
            );
        } catch (ActivityNotFoundException e) {
            showErrorMessage(getText(R.string.no_tts_installed), true);
        }

        resources = getApplicationContext().getResources();
        activity = this;

        setTitle(R.string.initializing);
    }

    class UpdateControls implements Runnable {
        private int buttonstate;
        static final int PAUSE = 0;
        static final int PLAY = 1;
			
		public void run() { 
			if(buttonstate==PLAY) { 
				pausebutton.setText(resources.getString(R.string.on_press_play));
				//pausebutton.setContentDescription(resources.getString(R.string.on_press_play));
			} else if (buttonstate==PAUSE){
			    pausebutton.setText(resources.getString(R.string.on_press_pause));
			    //pausebutton.setContentDescription(resources.getString(R.string.on_press_pause));
			}
		}

        public UpdateControls(int value) { this.buttonstate = value; }
    }
    	
    private OnClickListener forwardListener = new OnClickListener() {
        public void onClick(View v) {
            goForward();
        }
    };

    private void goForward() {
        stopTalking();
        setState(INACTIVE);
        nextParagraph(SEARCHFORWARD);
        speakBook();
    }

    private void goBackward() {
        stopTalking();
        setState(INACTIVE);
        nextParagraph(SEARCHBACKWARD);
        speakBook();
    }

    private OnClickListener contentsListener = new OnClickListener() {
        public void onClick(View view) {
            showContents();
        }
    };

    private void showContents() {
        stopTalking();
        setState(INACTIVE);
        Intent tocIntent = new Intent(activity, TOCActivity.class);
        activity.startActivityForResult(tocIntent, PLAY_AFTER_TOC);
    }

    private OnClickListener pauseListener = new OnClickListener() {
        public void onClick(View v) {

            playOrPause();
        }
    };

    private void playOrPause() {
        if(state==ACTIVE){
            stopTalking();
            fromPause = true;
            setState(INACTIVE);
        } else {
            speakBook();
        }
    }

    private void speakBook() {
        setState(ACTIVE);

        nextParagraph(CURRENTORFORWARD);
    }

    private OnClickListener stopListener = new OnClickListener() {
        public void onClick(View v) {
            stopTalking();
            finish();
        }
    };
    
    /** 
     * If book is available, add it to application title.
     */
    private final void setApplicationTitle() {
        Library library = Library.Instance();
        final Book currentBook = library.getRecentBook();
        
        if (currentBook != null) {
            StringBuilder title = new StringBuilder(currentBook.getTitle());
            if (title.toString().equals("About Bookshare Reader")) {
                title.append(getResources().getString(R.string.speak_title_postfix));
            }
            setTitle(title);
        }
    }

    @Override
    protected void onStart() {
        setApplicationTitle();
        super.onStart();

        pausebutton.requestFocus();
    }

    protected void onActivityResult(
	           int requestCode, int resultCode, Intent data) {
	    if (requestCode == CHECK_TTS_INSTALLED) {

            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                myTTS = new TextToSpeech(this, this);
            } else {
                startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
            }
        } else if (requestCode == PLAY_AFTER_TOC) {
            if (resultCode != TOCActivity.BACK_PRESSED) {
                resumePlaying = true;
            } else {
                fromPause = true;
            }
        }
    }

    private void showErrorMessage(final CharSequence text, final boolean fatal) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (fatal) {
                    setTitle(R.string.failure);
                }
                Toast.makeText(SpeakActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private volatile PowerManager.WakeLock myWakeLock;
	   
	   
// ZLTextWord cursor will navigate on a per-paragraph basis. 
// We have to split the paragraph into sentences. 
// Look for . at end of word
	private void getParagraphText(ZLTextParagraphCursor paraCursor) {
		StringBuilder sb = new StringBuilder();
	    	boolean inSentence = true;

        ArrayList<String> sentenceList = new ArrayList<String>();

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
            myTTS.speak(spkString, TextToSpeech.QUEUE_ADD, callbackMap);
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
        myTTS.shutdown();
		super.onDestroy();				
	}
	
	private void stopTalking() {
		setState(INACTIVE);
		if(myTTS !=null){
		    myTTS.stop();
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
        if (! fromPause) {
            final FBView theView = ((FBReaderApp) FBReaderApp.Instance()).getTextView();
            final ZLTextWordCursor cursor = theView.getStartCursor();
            myParaCursor = cursor.getParagraphCursor();
        }
        
        if (resumePlaying || fromPause) {
            resumePlaying = false;
            speakBook();
        }
	}

	
//	@Override
	public void onInit(int status) {
		myTTS.setOnUtteranceCompletedListener(this);
		setState(INACTIVE);
        setApplicationTitle();
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
            activity.finish();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
     public boolean dispatchTouchEvent(MotionEvent me){
       this.detector.onTouchEvent(me);
      return super.dispatchTouchEvent(me);
     }

    @Override
    public void onSwipe(int direction) {
        myVib.vibrate(50);
        switch (direction) {

            case SimpleGestureFilter.SWIPE_RIGHT :
                goForward();
                break;
            case SimpleGestureFilter.SWIPE_LEFT :
                goBackward();
                break;
            case SimpleGestureFilter.SWIPE_DOWN :
                showContents();
                break;
            case SimpleGestureFilter.SWIPE_UP :
                showContents();
                break;

          }
    }

    @Override
    public void onDoubleTap() {
        myVib.vibrate(50);
        playOrPause();
    }

/*    private void highlightParagraph()  {
        if (0 <= myParagraphIndex && myParagraphIndex < myParagraphsNumber) {
            myApi.highlightArea(
                    new TextPosition(myParagraphIndex, 0, 0),
                    new TextPosition(myParagraphIndex, Integer.MAX_VALUE, 0)
            );
        } else {
            myApi.clearHighlighting();
        }
    }*/

}
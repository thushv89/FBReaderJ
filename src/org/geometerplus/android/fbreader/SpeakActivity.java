package org.geometerplus.android.fbreader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
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
    	private ImageButton stopbutton;
    	private ArrayList sentenceList;
	private Iterator sentenceListIterator;

    	private int state = INACTIVE;
		

    	class UpdateControls implements Runnable { 
    		private int state;
    		static final int PAUSE = 0;
    		static final int PLAY = 1;
			
		public void run() { 
			if(state==PLAY) { 
				pausebutton.setImageResource(R.drawable.speak_play);
					//pausebutton.setText("Play");
			} else if (state==PAUSE){
				pausebutton.setImageResource(R.drawable.speak_pause);
					//pausebutton.setText("Pause");
			}
		}
    		public UpdateControls(int value) { this.state = value; }
    	} 
    	
    	
    	private PhoneStateListener mPhoneListener = new PhoneStateListener()
    	{
    	        public void onCallStateChanged(int state, String incomingNumber)
    	        {
    	        	if(state == TelephonyManager.CALL_STATE_RINGING) {
    	        		stopTalking();
    	        		finish();
    	        	}
    	        }
    	};
    	
    	private OnClickListener forwardListener = new OnClickListener() {
    	    public void onClick(View v) {
    	    	stopTalking();
    	    	speakString("FORWARD");
    	    	setState(INACTIVE);
  	      	    nextParagraph(SEARCHFORWARD);
    	    }
    	};

    	private OnClickListener backListener = new OnClickListener() {
    	    public void onClick(View v) {
    	    	stopTalking();
    	    	speakString("BACK");
    	    	setState(INACTIVE);
    	    	nextParagraph(SEARCHBACKWARD);
    	    }
    	};
    	
    	private OnClickListener pauseListener = new OnClickListener() {
    	    public void onClick(View v) {

     	       if(state==ACTIVE){
     	    	  stopTalking(); 
     	    	  speakString("PAUSE");
     	    	  setState(INACTIVE);
     	      } else {
     	    	  setState(ACTIVE);
     	    	  speakString("PLAY");
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
	       
	       setState(INACTIVE);
	       	       
	       TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
	       tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
	       
 		theView = ((FBReader)FBReader.Instance()).getTextView();
 		   
		ZLTextWordCursor cursor = theView.getStartCursor();
		myParaCursor = cursor.getParagraphCursor(); 
	       
	       Intent checkIntent = new Intent();
	       checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
	       startActivityForResult(checkIntent, CHECK_TTS_INSTALLED);


	   }
	   
	   protected void onActivityResult(
	           int requestCode, int resultCode, Intent data) {
	       if (requestCode == CHECK_TTS_INSTALLED) {
	           if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
	               // success, create the TTS instance
	               mTts = new TextToSpeech(this, this);
	           } else {
	               // missing data, install it
	               Intent installIntent = new Intent();
	               installIntent.setAction(
	                   TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
	               startActivity(installIntent);
	           }
	       }
	   }
	   
	   
// ZLTextWord cursor will navigate on a per-paragraph basis. 
// We have to split the paragraph into sentences. 
// Look for . at end of word
	private void getParagraphText(ZLTextParagraphCursor paraCursor) {
		StringBuilder sb = new StringBuilder();
	    	boolean inSentence = true;

		sentenceList = new ArrayList();                      // clears out list, old list gets GCed

		ZLTextWordCursor cursor = new ZLTextWordCursor(paraCursor);

		while(!cursor.isEndOfParagraph()) { 
			ZLTextElement element = cursor.getElement();
			while (inSentence == true)  {                         
  			    if(element instanceof ZLTextWord) {
  			    	if (element.toString().indexOf(".") == (element.toString().length() -1) ) {           // detects period at end of element
   			    	   sb.append(element.toString().substring(0,element.toString().indexOf(".")));        // remove period	
  			    	   inSentence = false;
   			        } else {   
  			    	   sb.append(element.toString()+" ");
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
		
		if(state==ACTIVE){
			pausebutton.post(new UpdateControls(UpdateControls.PAUSE));			 
		}else if(state==INACTIVE) {
			pausebutton.post(new UpdateControls(UpdateControls.PLAY));			 
		}
	}
	
	private void speakString(String s){
		setState(ACTIVE);	

		HashMap<String, String> callbackMap = new HashMap<String, String>();
		callbackMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,PARAGRAPHUTTERANCE);

		mTts.speak(s, TextToSpeech.QUEUE_ADD, callbackMap);			
	}
	
	private void showString(String s){
		theView.gotoPosition(myParaCursor.Index,0,0);
		
		Reader.repaintView(); 
		Reader.showBookTextView();
		//theView.getModel().Book.storePosition(BookTextView.getStartCursor());
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
			if (!atLimit)  {			
				getParagraphText(myParaCursor);
			}
		}	
	}
	
	
	@Override
	protected void  onDestroy(){	
		Reader.onWindowClosing(); // save the position
		setState(INACTIVE);
		mTts.shutdown();
		super.onDestroy();				
	}
	
	private void stopTalking(){
		setState(INACTIVE);
		if(mTts!=null){
		    mTts.stop();
		}
	}
	
	@Override
	protected void  onPause(){
		Reader.onWindowClosing(); // save the position
		super.onPause();
	}
	
	@Override
	public void  onBackPressed(){
		stopTalking();
		super.onBackPressed();
	}
	
	@Override
	protected void  onResume(){			
		super.onResume();
	}

	
//	@Override
	public void onInit(int status) {
		mTts.setOnUtteranceCompletedListener(this);
		setState(INACTIVE);
//		nextParagraphString(SEARCHFORWARD);
	}
	
	public void onUtteranceCompleted(String uttId) {
		String spkString = "";
		if(state == ACTIVE && uttId.equals(this.PARAGRAPHUTTERANCE)) {
			if (!sentenceListIterator.hasNext())  {
			    nextParagraph(SEARCHFORWARD);                        // nextParagraph can change sentenceListIterator
			}
            		if (sentenceListIterator.hasNext())  { 			 // if there are sentences in the sentence queue
                		spkString = sentenceListIterator.next().toString();
            			speakString(spkString);
            		}
        	} else {
			setState(INACTIVE);
		}		
	}
}
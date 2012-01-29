/*
 * Copyright (C) 2009-2011 Geometer Plus <contact@geometerplus.com>
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

package org.geometerplus.android.fbreader.benetech;

import java.util.HashMap;
import java.util.Locale;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.accessibility.SimpleGestureFilter;
import org.benetech.android.R;
import org.geometerplus.android.fbreader.TOCActivity;
import org.geometerplus.android.fbreader.api.ApiServerImplementation;
import org.geometerplus.android.fbreader.api.TextPosition;

public class NewSpeakActivity extends Activity implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener, SimpleGestureFilter.SimpleGestureListener  {
    private ApiServerImplementation myApi;

	private static final String UTTERANCE_ID = "FBReaderTTSPlugin";

	private TextToSpeech myTTS;

	private int myParagraphIndex = -1;
	private int myParagraphsNumber;

	private boolean myIsActive = false;

    private static final int PLAY_AFTER_TOC = 1;
    private SimpleGestureFilter detector;
    private Vibrator myVib;
    private int lastSentence = 0;
    private int lastSpoken = 0;
    private boolean justPaused = false;

    private void setListener(int id, View.OnClickListener listener) {
		findViewById(id).setOnClickListener(listener);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.view_spokentext);

        WindowManager.LayoutParams params =
        getWindow().getAttributes();
        params.gravity = Gravity.BOTTOM;
        this.getWindow().setAttributes(params);
        detector = new SimpleGestureFilter(this,this);
        myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

		setListener(R.id.spokentextback, new View.OnClickListener() {
			public void onClick(View v) {
                goBackward();
			}
		});
		setListener(R.id.spokentextforward, new View.OnClickListener() {
			public void onClick(View v) {
                goForward();
			}
		});
/*		setListener(R.id.button_close, new View.OnClickListener() {
			public void onClick(View v) {
				stopTalking();
				finish();
			}
		});*/
		setListener(R.id.spokentextpause, new View.OnClickListener() {
			public void onClick(View v) {
                playOrPause();
            }
		});

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

		setActive(false);
		setActionsEnabled(false);

		myApi = new ApiServerImplementation();
		try {
			startActivityForResult(
				new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA), 0
			);
		} catch (ActivityNotFoundException e) {
			showErrorMessage(getText(R.string.no_tts_installed), true);
		}

		setTitle(R.string.initializing);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
			myTTS = new TextToSpeech(this, this);
		} else {
			startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
        findViewById(R.id.spokentextpause).requestFocus();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		stopTalking();
        myApi.clearHighlighting();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (myTTS != null) {
			myTTS.shutdown();
		}
		super.onDestroy();
	}

	private volatile int myInitializationStatus;
	private static int TTS_INITIALIZED = 2;
	private static int FULLY_INITIALIZED =  TTS_INITIALIZED;

	// implements TextToSpeech.OnInitListener
	public void onInit(int status) {
		if (myInitializationStatus != FULLY_INITIALIZED) {
			myInitializationStatus |= TTS_INITIALIZED;
			if (myInitializationStatus == FULLY_INITIALIZED) {
				doFinalInitialization();
			}
		}
	}

	private void setActionsEnabled(final boolean enabled) {
		runOnUiThread(new Runnable() {
			public void run() {
				findViewById(R.id.spokentextback).setEnabled(enabled);
				findViewById(R.id.spokentextforward).setEnabled(enabled);
				findViewById(R.id.spokentextpause).setEnabled(enabled);
			}
		});
	}

	private void doFinalInitialization() {
		myTTS.setOnUtteranceCompletedListener(this);

		try {
			setTitle(myApi.getBookTitle());

			Locale locale = null;
			final String language = myApi.getBookLanguage();
			if ("other".equals(language)) {
				locale = Locale.getDefault();
				if (myTTS.isLanguageAvailable(locale) < 0) {
					locale = Locale.ENGLISH;
				}
				showErrorMessage(
					getText(R.string.language_is_not_set).toString()
						.replace("%0", locale.getDisplayLanguage()),
					false
				);
			} else {
				final String languageCode = myApi.getBookLanguage();
				try {
					locale = new Locale(languageCode);
				} catch (Exception e) {
				}
				if (locale == null || myTTS.isLanguageAvailable(locale) < 0) {
					final Locale originalLocale = locale;
					locale = Locale.getDefault();
					if (myTTS.isLanguageAvailable(locale) < 0) {
						locale = Locale.ENGLISH;
					}
					showErrorMessage(
						getText(R.string.no_data_for_language).toString()
							.replace("%0", originalLocale != null
								? originalLocale.getDisplayLanguage() : languageCode)
							.replace("%1", locale.getDisplayLanguage()),
						false
					);
				}
			}
			myTTS.setLanguage(locale);

			myParagraphIndex = myApi.getPageStart().ParagraphIndex;
			myParagraphsNumber = myApi.getParagraphsNumber();
			setActionsEnabled(true);
			speakParagraph(getNextParagraph());
		} catch (Exception e) {
			setActionsEnabled(false);
			showErrorMessage(getText(R.string.initialization_error), true);
		}
	}

	@Override
	public void onUtteranceCompleted(String uttId) {
        String lastSentenceID = Integer.toString(lastSentence - 1);
		if (myIsActive && uttId.equals(lastSentenceID)) {
            ++myParagraphIndex;
            speakParagraph(getNextParagraph());
            if (myParagraphIndex >= myParagraphsNumber) {
                stopTalking();
            }
		} else {
            lastSpoken = Integer.parseInt(uttId);
		}
	}

	private void highlightParagraph()  {
		if (0 <= myParagraphIndex && myParagraphIndex < myParagraphsNumber) {
			myApi.highlightArea(
                    new TextPosition(myParagraphIndex, 0, 0),
                    new TextPosition(myParagraphIndex, Integer.MAX_VALUE, 0)
            );
		} else {
			myApi.clearHighlighting();
		}
	}

	private void stopTalking() {
		setActive(false);
		if (myTTS != null) {
			myTTS.stop();
		}
	}

	private void showErrorMessage(final CharSequence text, final boolean fatal) {
		runOnUiThread(new Runnable() {
			public void run() {
				if (fatal) {
					setTitle(R.string.failure);
				}
				Toast.makeText(NewSpeakActivity.this, text, Toast.LENGTH_SHORT).show();
			}
		});
	}

	private volatile PowerManager.WakeLock myWakeLock;

	private synchronized void setActive(final boolean active) {
		myIsActive = active;

		runOnUiThread(new Runnable() {
			public void run() {
                ((Button)findViewById(R.id.spokentextpause)).setText(active ? R.string.on_press_pause : R.string.on_press_play);
                findViewById(R.id.spokentextpause).requestFocus();
			}
		});

		if (active) {
			if (myWakeLock == null) {
				myWakeLock =
					((PowerManager)getSystemService(POWER_SERVICE))
						.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FBReader TTS plugin");
				myWakeLock.acquire();
			}
		} else {
			if (myWakeLock != null) {
				myWakeLock.release();
				myWakeLock = null;
			}
		}
	}

	private void speakStringNew(String text, final int sentenceNumber) {
		HashMap<String, String> callbackMap = new HashMap<String, String>();
		callbackMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, Integer.toString(sentenceNumber));
		myTTS.speak(text, TextToSpeech.QUEUE_ADD, callbackMap);
	}

	private void gotoPreviousParagraph() {
        for (int i = myParagraphIndex - 1; i >= 0; --i) {
            if (myApi.getParagraphText(i).length() > 0) {
                myParagraphIndex = i;
                break;
            }
        }
        if (myApi.getPageStart().ParagraphIndex >= myParagraphIndex) {
            myApi.setPageStart(new TextPosition(myParagraphIndex, 0, 0));
        }
        highlightParagraph();
        runOnUiThread(new Runnable() {
            public void run() {
                findViewById(R.id.spokentextforward).setEnabled(true);
                findViewById(R.id.spokentextpause).setEnabled(true);
            }
        });

	}

	private String getNextParagraph() {
			String text = "";
			for (; myParagraphIndex < myParagraphsNumber; ++myParagraphIndex) {
				final String s = myApi.getParagraphText(myParagraphIndex);
				if (s.length() > 0) {
					text = s;
					break;
				}
			}
			if (!"".equals(text) && !myApi.isPageEndOfText()) {
				myApi.setPageStart(new TextPosition(myParagraphIndex, 0, 0));
			}
			highlightParagraph();
			if (myParagraphIndex >= myParagraphsNumber) {
				runOnUiThread(new Runnable() {
					public void run() {
						findViewById(R.id.spokentextforward).setEnabled(false);
						//findViewById(R.id.spokentextpause).setEnabled(false);
					}
				});
			}
			return text;

	}

    // Bookshare custom methods

    private void speakParagraph(String text) {
        setActive(true);
        String[] sentenceArray = text.split("[\\.\\!\\?]");
        lastSentence = sentenceArray.length;
        int startingSentenceNumber = 0;

        if (justPaused) {                    // on returning from pause, start with the last sentence spoken
            justPaused = false;
            startingSentenceNumber = lastSpoken;
        }

        for(int i = startingSentenceNumber; i< lastSentence; i++) {
            speakStringNew(sentenceArray[i], i);
        }
    }

    private void playOrPause() {
            if (!myIsActive) {
                speakParagraph(getNextParagraph());
            } else {
                stopTalking();
                setActive(false);
                justPaused = true;
            }
        }

    private void goForward() {
        stopTalking();
        if (myParagraphIndex < myParagraphsNumber) {
            ++myParagraphIndex;
            speakParagraph(getNextParagraph());
        }
    }

    private void goBackward() {
        stopTalking();
        gotoPreviousParagraph();
        speakParagraph(getNextParagraph());
    }

    private void showContents() {
        stopTalking();
        setActive(false);
        Intent tocIntent = new Intent(this, TOCActivity.class);
        startActivityForResult(tocIntent, PLAY_AFTER_TOC);
    }

    @Override
         public boolean dispatchTouchEvent(MotionEvent me){
           this.detector.onTouchEvent(me);
          return super.dispatchTouchEvent(me);
         }

    @Override
    public void onSwipe(int direction) {
        myVib.vibrate(100);
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
        myVib.vibrate(100);
        playOrPause();
    }
}

package org.geometerplus.android.fbreader;

import org.geometerplus.android.fbreader.benetech.NewSpeakActivity;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

import android.content.Intent;

/**
 * @author roms
 * Action to bring up speak buttons
 */
public class ShowSpeakAction extends FBAndroidAction {
    ShowSpeakAction(FBReader baseActivity, FBReaderApp fbreader) {
        super(baseActivity, fbreader);
    }

    @Override
    protected void run(Object... params) {
        Intent intent = new Intent(BaseActivity.getApplicationContext(), NewSpeakActivity.class);
        BaseActivity.startActivityIfNeeded(intent, 0);
    }
}

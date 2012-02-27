package org.accessibility;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;

/**
 * @author roms
 *         Date: 2/24/12
 */
public class ParentCloserDialog extends VoiceableDialog {
    public ParentCloserDialog(Context context, final Activity parentActivity) {
        super(context);
        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                parentActivity.finish();
            }
        });
    }
}

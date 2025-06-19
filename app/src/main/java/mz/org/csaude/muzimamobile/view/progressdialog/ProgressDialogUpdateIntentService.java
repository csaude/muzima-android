/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package mz.org.csaude.muzimamobile.view.progressdialog;

import android.app.IntentService;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import mz.org.csaude.muzimamobile.utils.Constants;
import mz.org.csaude.muzimamobile.view.BroadcastListenerActivity;

public class ProgressDialogUpdateIntentService extends IntentService{
    public ProgressDialogUpdateIntentService(){
        super(Constants.ProgressDialogConstants.PROGRESS_UPDATE_ACTION);
    }

    @Override
    public void onHandleIntent(Intent intent){
        intent.setAction(BroadcastListenerActivity.PROGRESS_UPDATE_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }
}

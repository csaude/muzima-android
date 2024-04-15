/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.reports;

import android.app.Activity;
import com.muzima.utils.Constants;
import com.muzima.view.SyncIntent;

public class DownloadPatientReportsIntent extends SyncIntent {
    public DownloadPatientReportsIntent(Activity activity, String[] selectedReportsArray) {
        super(activity);
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_PATIENT_REPORTS);
        putExtra(Constants.SyncPatientReportsConstants.REPORT_UUIDS, selectedReportsArray);
    }
}

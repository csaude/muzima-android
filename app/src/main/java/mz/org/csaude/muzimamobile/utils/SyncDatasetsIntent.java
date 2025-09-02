/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package mz.org.csaude.muzimamobile.utils;

import android.content.Context;

import com.muzima.api.model.SetupConfigurationTemplate;
import mz.org.csaude.muzimamobile.view.SyncIntent;

public class SyncDatasetsIntent extends SyncIntent {
    public SyncDatasetsIntent(Context context, SetupConfigurationTemplate configBeforeConfigUpdate){
        super(context);
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_DATASETS);
        putExtra(Constants.DataSyncServiceConstants.CONFIG_BEFORE_UPDATE, configBeforeConfigUpdate);
    }
}

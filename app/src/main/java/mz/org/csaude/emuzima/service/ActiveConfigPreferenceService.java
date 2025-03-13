/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package mz.org.csaude.emuzima.service;

import static mz.org.csaude.emuzima.utils.DeviceDetailsUtil.generatePseudoDeviceId;
import static mz.org.csaude.emuzima.utils.StringUtils.EMPTY;

import android.util.Log;

import mz.org.csaude.emuzima.MuzimaApplication;
import mz.org.csaude.emuzima.R;
import com.muzima.api.model.AppUsageLogs;
import mz.org.csaude.emuzima.utils.MuzimaPreferences;

import java.util.Date;
import java.util.UUID;

public class ActiveConfigPreferenceService extends PreferenceService{
    private final MuzimaApplication application;

    public ActiveConfigPreferenceService(MuzimaApplication application){
        super(application.getApplicationContext());
        this.application = application;
    }

    public String getActiveConfigUuid(){
        String key = context.getResources().getString(R.string.active_config_uuid);
        return MuzimaPreferences.getStringPreference(context, key, EMPTY);
    }

    public void setActiveConfigUuid(String uuid){
        try {
            String key = context.getResources().getString(R.string.active_config_uuid);
            MuzimaPreferences.setStringPreference(context, key, uuid);

            AppUsageLogs setupConfig = new AppUsageLogs();
            setupConfig.setUuid(UUID.randomUUID().toString());
            setupConfig.setLogKey(com.muzima.util.Constants.AppUsageLogs.SET_UP_CONFIG_UUID);
            setupConfig.setLogvalue(uuid);
            setupConfig.setUpdateDatetime(new Date());
            setupConfig.setLogSynced(false);

            String loggedInUser = application.getAuthenticatedUserId();
            setupConfig.setUserName(loggedInUser);

            String pseudoDeviceId = generatePseudoDeviceId();
            setupConfig.setDeviceId(pseudoDeviceId);
            application.getAppUsageLogsController().saveOrUpdateAppUsageLog(setupConfig);
        } catch (Throwable e){
            Log.e(getClass().getSimpleName(), "Error saving active config preference / log");
        }
    }
}

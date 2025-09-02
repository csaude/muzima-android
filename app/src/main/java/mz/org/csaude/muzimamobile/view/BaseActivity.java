/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package mz.org.csaude.muzimamobile.view;

import android.content.Intent;
import android.os.Bundle;
import mz.org.csaude.muzimamobile.MuzimaApplication;
import mz.org.csaude.muzimamobile.service.MuzimaLoggerService;
import mz.org.csaude.muzimamobile.utils.ApplicationStep;
import mz.org.csaude.muzimamobile.utils.LanguageUtil;
import mz.org.csaude.muzimamobile.utils.StringUtils;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;
import java.util.Map;

public class BaseActivity extends AppCompatActivity {

    private final LanguageUtil languageUtil = new LanguageUtil();
    protected ApplicationStep applicationStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setupActionBar();

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        if(bundle != null) {
            if (StringUtils.stringHasValue((String) bundle.getSerializable("step"))) {
                applicationStep = ApplicationStep.fastCreate((String) bundle.getSerializable("step"));
            }
        }
        applicationStep = ApplicationStep.fastCreate(ApplicationStep.STEP_INIT);
    }

    private void setupActionBar() {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowTitleEnabled(true);
        }
    }

    @Override
    public void onUserInteraction() {
        ((MuzimaApplication) getApplication()).restartTimer();
        super.onUserInteraction();
    }

    @Override
    protected void onResume() {
        super.onResume();
        languageUtil.onResume(this);
        ((MuzimaApplication) getApplication()).setCurrentActivity(this);
    }
    protected void logEvent(String tag, String details){
        if(StringUtils.isEmpty(details)){
            details = "{}";
        }
        MuzimaApplication muzimaApplication = (MuzimaApplication)getApplicationContext();
        MuzimaLoggerService.log(muzimaApplication,tag,  details);
    }

    protected void logEvent(String tag){
        logEvent(tag,null);
    }

    /**
     * Move from one {@link android.app.Activity} to another
     *
     * @param clazz target activity
     * @param params params to be sent
     * @param finishCurrentActivity condition to finish or not the current activity
     */
    private void nextActivity(Class clazz, Map<String, Object> params, boolean finishCurrentActivity){

        Intent intent = new Intent(getApplication(), clazz);
        Bundle bundle = new Bundle();

        if (params != null && params.size() > 0){
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (entry.getValue() instanceof Serializable) {
                    bundle.putSerializable(entry.getKey(), (Serializable) entry.getValue());
                }
            }
            intent.putExtras(bundle);
        }
        startActivity(intent);
        if (finishCurrentActivity) finish();
    }

    /**
     * Forward from current activity to a new one passed on the param without finishing current one
     * @param clazz target activity
     */
    public void nextActivity(Class clazz){
        nextActivity(clazz, null, false);
    }

    /**
     * Forward from current activity to a new one passed on the param finishing current one
     * @param clazz target activity
     */
    public void nextActivityFinishingCurrent(Class clazz){
        nextActivity(clazz, null, true);
    }

    /**
     * Forward from current activity to a new one passed on the param without finishing current one, sending params
     *
     * @param clazz target activity
     * @param params params to be sent to the other activity
     */
    public void nextActivity(Class clazz, Map<String, Object> params){
        nextActivity(clazz, params, false);
    }

    /**
     * Forward from current activity to a new one passed on the param finishing current one, sending params
     *
     * @param clazz target activity
     * @param params params to be sent to the other activity
     */
    public void nextActivityFinishingCurrent(Class clazz, Map<String, Object> params){
        nextActivity(clazz, params, true);
    }

}

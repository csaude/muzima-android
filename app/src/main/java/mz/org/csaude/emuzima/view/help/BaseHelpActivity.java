/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package mz.org.csaude.emuzima.view.help;

import android.os.Bundle;
import android.view.Menu;
import mz.org.csaude.emuzima.MuzimaApplication;
import mz.org.csaude.emuzima.domain.Credentials;
import mz.org.csaude.emuzima.utils.ThemeUtils;
import mz.org.csaude.emuzima.view.BaseActivity;

public class BaseHelpActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (isUserLoggedOut()) {
            ((MuzimaApplication) getApplication()).cancelTimer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public String getUserName() {
        Credentials credentials = new Credentials(this);
        return credentials.getUserName();
    }

    public boolean isUserLoggedOut() {
        Credentials credentials = new Credentials(this);
        return credentials.isEmpty();
    }
}

/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package mz.org.csaude.emuzima.adapters.forms;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import mz.org.csaude.emuzima.MuzimaApplication;
import mz.org.csaude.emuzima.controller.FormController;
import mz.org.csaude.emuzima.view.fragments.forms.AllFormsListFragment;
import mz.org.csaude.emuzima.view.fragments.forms.AvailableFormsFragment;
import mz.org.csaude.emuzima.view.fragments.forms.DownloadedFormsFragment;

public class NewFormsPagerAdapter extends FragmentPagerAdapter {

    private final Integer totalTabs;
    protected final Context context;

    public NewFormsPagerAdapter(@NonNull FragmentManager fm, Integer totalTabs, Context context) {
        super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
        this.totalTabs = totalTabs;
        this.context = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        FormController formController = ((MuzimaApplication) context.getApplicationContext()).getFormController();

        if (position == 1)
            return DownloadedFormsFragment.newInstance(formController);
        else if (position == 2)
            return new AvailableFormsFragment();

        return new AllFormsListFragment();
    }

    @Override
    public int getCount() {
        return totalTabs;
    }
}

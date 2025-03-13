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
import mz.org.csaude.emuzima.controller.FormController;
import mz.org.csaude.emuzima.controller.ObservationController;
import mz.org.csaude.emuzima.model.AvailableForm;
import mz.org.csaude.emuzima.model.collections.AvailableForms;
import mz.org.csaude.emuzima.tasks.FormsAdapterBackgroundQueryTask;

/**
 * Responsible to list down all the registration forms. Will be in use only if there are more than 1 form.
 */
public class RegistrationFormsAdapter extends FormsAdapter<AvailableForm> {
    private final AvailableForms availableForms;


    public RegistrationFormsAdapter(Context context, int textViewResourceId, FormController formController, AvailableForms availableForms, ObservationController observationController) {
        super(context, textViewResourceId, formController, observationController);
        this.availableForms = availableForms;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<AvailableForm> {

        BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected AvailableForms doInBackground(Void... voids) {
            return availableForms;
        }

        @Override
        protected void onBackgroundError(Exception e) {

        }
    }

}

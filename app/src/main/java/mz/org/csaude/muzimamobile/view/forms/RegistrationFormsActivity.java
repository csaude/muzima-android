/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package mz.org.csaude.muzimamobile.view.forms;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import mz.org.csaude.muzimamobile.MuzimaApplication;
import mz.org.csaude.muzimamobile.R;
import mz.org.csaude.muzimamobile.adapters.forms.RegistrationFormsAdapter;
import com.muzima.api.model.Patient;
import mz.org.csaude.muzimamobile.controller.FormController;
import mz.org.csaude.muzimamobile.controller.ObservationController;
import mz.org.csaude.muzimamobile.model.AvailableForm;
import mz.org.csaude.muzimamobile.model.collections.AvailableForms;
import mz.org.csaude.muzimamobile.utils.LanguageUtil;
import mz.org.csaude.muzimamobile.utils.ThemeUtils;
import mz.org.csaude.muzimamobile.view.BaseAuthenticatedActivity;
import mz.org.csaude.muzimamobile.view.MainDashboardActivity;
import mz.org.csaude.muzimamobile.view.patients.PatientSummaryActivity;
import mz.org.csaude.muzimamobile.view.relationship.RelationshipsListActivity;

import java.util.UUID;

public class RegistrationFormsActivity extends BaseAuthenticatedActivity {
    private RegistrationFormsAdapter registrationFormsAdapter;
    private Patient patient;
    private Patient indexPatient;
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_form_list);

        patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        indexPatient = (Patient) getIntent().getSerializableExtra(RelationshipsListActivity.INDEX_PATIENT);


        FormController formController = ((MuzimaApplication) getApplicationContext()).getFormController();
        ObservationController observationController = ((MuzimaApplication) getApplicationContext()).getObservationController();
        AvailableForms availableForms = getRegistrationForms(formController);
        if (availableForms.isEmpty()){
            showRegistrationFormsMissingAlert();
        }else {
            if (isOnlyOneRegistrationForm(availableForms)) {
                startWebViewActivity(availableForms.get(0));
            } else {
                prepareRegistrationAdapter(formController, availableForms, observationController);
            }
        }
        logEvent("VIEW_REGISTRATION_FORMS");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    private void showRegistrationFormsMissingAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationFormsActivity.this);
        builder.setCancelable(false)
                .setIcon(ThemeUtils.getIconWarning(getApplicationContext()))
                .setTitle(getResources().getString(R.string.general_alert))
                .setMessage(getResources().getString(R.string.general_registration_form_missing_message))
                .setPositiveButton(R.string.general_ok, launchDashboard())
                .show();
    }

    private DialogInterface.OnClickListener launchDashboard() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity( new Intent(getApplicationContext(), MainDashboardActivity.class));
                finish();
            }
        };
    }

    private void prepareRegistrationAdapter(FormController formController, AvailableForms availableForms, ObservationController observationController) {
        registrationFormsAdapter = new RegistrationFormsAdapter(this, R.layout.item_forms_list,
                formController, availableForms, observationController);
        ListView list = findViewById(R.id.list);
        list.setOnItemClickListener(startRegistrationOnClick());
        list.setAdapter(registrationFormsAdapter);
        registrationFormsAdapter.reloadData();
    }

    private boolean isOnlyOneRegistrationForm(AvailableForms availableForms) {
        return availableForms.size() == 1;
    }

    private AdapterView.OnItemClickListener startRegistrationOnClick() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AvailableForm form = registrationFormsAdapter.getItem(position);
                startWebViewActivity(form);
            }
        };
    }

    private void startWebViewActivity(AvailableForm form) {
        if (patient == null) {
            patient = new Patient();
            patient.setUuid(String.valueOf(UUID.randomUUID()));
        }
        Intent intent = new FormViewIntent(this, form, patient, false);
        intent.putExtra(RelationshipsListActivity.INDEX_PATIENT, indexPatient);
        startActivity(intent);
        finish();
    }

    private AvailableForms getRegistrationForms(FormController formController) {
        AvailableForms availableForms = null;
        try {
            availableForms = formController.getDownloadedRegistrationForms();
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(), "Error while retrieving registration forms from Lucene");
        }
        return availableForms;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

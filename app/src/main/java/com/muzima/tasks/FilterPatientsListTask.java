package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.model.events.CohortFilterActionEvent;

import java.util.List;

public class FilterPatientsListTask implements Runnable {

    private Context context;
    private CohortFilterActionEvent event;
    private PatientsListFilterCallback patientsListFilterCallback;

    public FilterPatientsListTask(Context context, CohortFilterActionEvent event, PatientsListFilterCallback patientsListFilterCallback) {
        this.context = context;
        this.event = event;
        this.patientsListFilterCallback = patientsListFilterCallback;
    }

    @Override
    public void run() {
        try {
            if (event.getFilter() == null && !event.isNoSelectionEvent()) {
                patientsListFilterCallback.onPatientsFiltered(((MuzimaApplication) context.getApplicationContext()).getPatientController()
                        .getAllPatients());
            } else if (event.getFilter() != null && !event.isNoSelectionEvent()) {
                List<Patient> patientList = ((MuzimaApplication) context.getApplicationContext()).getPatientController()
                        .getPatientsForCohorts(new String[]{event.getFilter().getUuid()});
                patientsListFilterCallback.onPatientsFiltered(patientList);
            }
        } catch (PatientController.PatientLoadException ex) {
            ex.printStackTrace();
        }
    }

    public interface PatientsListFilterCallback {
        void onPatientsFiltered(List<Patient> patientList);
    }
}

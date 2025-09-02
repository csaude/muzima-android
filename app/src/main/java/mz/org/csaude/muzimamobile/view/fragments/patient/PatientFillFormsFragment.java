/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package mz.org.csaude.muzimamobile.view.fragments.patient;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import mz.org.csaude.muzimamobile.MuzimaApplication;
import mz.org.csaude.muzimamobile.R;
import mz.org.csaude.muzimamobile.adapters.forms.ClientSummaryFormsAdapter;
import com.muzima.api.model.DerivedObservation;
import com.muzima.api.model.Patient;
import mz.org.csaude.muzimamobile.controller.DerivedObservationController;
import mz.org.csaude.muzimamobile.controller.PatientController;
import mz.org.csaude.muzimamobile.model.AvailableForm;
import mz.org.csaude.muzimamobile.model.collections.AvailableForms;
import mz.org.csaude.muzimamobile.tasks.FormsLoaderService;
import mz.org.csaude.muzimamobile.view.custom.MuzimaRecyclerView;
import mz.org.csaude.muzimamobile.view.forms.FormViewIntent;
import mz.org.csaude.muzimamobile.view.forms.FormsWithDataActivity;
import mz.org.csaude.muzimamobile.view.relationship.RelationshipsListActivity;

import java.util.ArrayList;
import java.util.List;

public class PatientFillFormsFragment extends Fragment implements FormsLoaderService.FormsLoadedCallback, ClientSummaryFormsAdapter.OnFormClickedListener {

    private ClientSummaryFormsAdapter formsAdapter;
    private Patient patient;
    private final String patientUuid;
    private List<AvailableForm> forms = new ArrayList<>();

    public PatientFillFormsFragment(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        initializeResources(view);
        loadData();
        return view;
    }

    private void loadData() {
        ((MuzimaApplication) requireActivity().getApplicationContext()).getExecutorService()
                .execute(new FormsLoaderService(requireActivity().getApplicationContext(), this));
    }

    private void initializeResources(View view) {
        MuzimaRecyclerView formsRecyclerView = view.findViewById(R.id.recycler_list);
        formsRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false));

        formsAdapter = new ClientSummaryFormsAdapter(forms, this);
        formsRecyclerView.setAdapter(formsAdapter);
        formsRecyclerView.setNoDataLayout(view.findViewById(R.id.no_data_layout),
                getString(R.string.info_forms_unavailable),
                getString(R.string.info_no_forms_data_tip));
        try {
            patient = ((MuzimaApplication) requireActivity().getApplicationContext()).getPatientController().getPatientByUuid(patientUuid);
        }catch (PatientController.PatientLoadException ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void onFormsLoaded(final AvailableForms formList) {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                forms.addAll(formList);
                formsAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onFormClickedListener(int position) {
        AvailableForm form = forms.get(position);

        if (!hasInterventionsDefined(form, patient)) {
            showWarningMessage("Nenhuma categoria de elegibilidade encontrada para este paciente, tente sincronizar o aplicativo com o servidor.");
            return; // Stop execution, preventing intent creation
        }

        Intent intent = new FormViewIntent(getActivity(), form, patient , false);
        intent.putExtra(RelationshipsListActivity.INDEX_PATIENT, patient);
        requireActivity().startActivityForResult(intent, FormsWithDataActivity.FORM_VIEW_ACTIVITY_RESULT);
    }

    private boolean hasInterventionsDefined(AvailableForm form, Patient patient) {
        if (form == null || form.getFormUuid() == null) {
            return false;
        }

        DerivedObservationController derivedObservationController =
                ((MuzimaApplication) requireActivity().getApplication()).getDerivedObservationController();

        if (form.getFormUuid().equals("fdd67221-5d1a-49e9-97e2-2f69aa5e26bc") ||
                (form.getFormUuid().equals("709989e1-dbb1-4beb-b906-62e70aa95a8a"))) {

            try {
                if (patient == null || patient.getUuid() == null) {
                    return false;
                }

                List<DerivedObservation> derivedObservations =
                        derivedObservationController.getDerivedObservationByPatientUuid(patient.getUuid());

                return derivedObservations != null && !derivedObservations.isEmpty();

            } catch (DerivedObservationController.DerivedObservationFetchException e) {
                Log.e(getClass().getSimpleName(), "Error while fetching derived observations", e);
                return false;
            }
        }

        return true;
    }

    private void showWarningMessage(String message) {
        requireActivity().runOnUiThread(() -> new AlertDialog.Builder(requireActivity())
                .setTitle("Atenção!")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show());
    }

}

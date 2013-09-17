package com.muzima.adapters.forms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.emilsjolander.components.stickylistheaders.StickyListHeadersAdapter;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersSectionIndexerAdapterWrapper;
import com.muzima.R;
import com.muzima.controller.FormController;
import com.muzima.model.FormWithPatientData;
import com.muzima.model.PatientMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class SectionedFormsAdapter<T extends FormWithPatientData> extends FormsAdapter<T> implements StickyListHeadersAdapter, SectionIndexer {
    private static final String TAG = "SectionedFormsAdapter";

    private List<PatientMetaData> patients;
    private ListView listView;

    public SectionedFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
        patients = new ArrayList<PatientMetaData>();
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.layout_forms_list_section_header, parent, false);
            holder.patientName = (TextView) convertView.findViewById(R.id.patientName);
            holder.patientIdentifier = (TextView) convertView.findViewById(R.id.patientId);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        PatientMetaData patientMetaData = patients.get(getSectionForPosition(position));
        holder.patientName.setText(patientMetaData.getDisplayName());
        holder.patientIdentifier.setText(patientMetaData.getPatientIdentifier());

        return convertView;
    }

    public void setListView(ListView listView) {
        this.listView = listView;
    }

    @Override
    public long getHeaderId(int position) {
        return patients.indexOf(getItem(position).getPatientMetaData());
    }

    @Override
    public Object[] getSections() {
        String[] familyNames = new String[patients.size()];
        for(int i = 0; i < patients.size(); i++){
            familyNames[i] = String.valueOf(patients.get(i).getPatientFamilyName().charAt(0));
        }
        return familyNames;
    }

    @Override
    public int getPositionForSection(int section) {
        if (section >= patients.size()) {
            section = patients.size() - 1;
        } else if (section < 0) {
            section = 0;
        }

        int position = 0;
        for (int i = 0; i < getCount(); i++) {
            FormWithPatientData item = getItem(i);
            if(item.getPatientMetaData().equals(patients.get(section))){
                position = i;
                break;
            }
        }
        return position;
    }

    @Override
    public int getSectionForPosition(int position) {
        if(position >= getCount()){
            position = getCount()-1;
        }else if(position < 0){
            position = 0;
        }

        return patients.indexOf(getItem(position).getPatientMetaData());
    }

    public void sortFormsByPatientName(List<T> forms) {
        Collections.sort(patients);
        Collections.sort(forms, alphabaticalComparator);
        setNotifyOnChange(false);
        clear();
        addAll(forms);
        StickyListHeadersSectionIndexerAdapterWrapper adapter = (StickyListHeadersSectionIndexerAdapterWrapper) listView.getAdapter();
        adapter.notifyDataSetChanged();
    }

    public void sortPatientsByDate() {

    }

    List<PatientMetaData> getPatients() {
        return patients;
    }

    public void setPatients(List<PatientMetaData> patients) {
        this.patients = patients;
    }

    private static class HeaderViewHolder {
        TextView patientName;
        TextView patientIdentifier;
    }

    public List<T> getCurrentListData() {
        List<T> formsWithPatientData = new ArrayList<T>();
        for (int x = 0; x < getCount(); x++) {
            formsWithPatientData.add(getItem(x));
        }
        return formsWithPatientData;
    }

    private Comparator<FormWithPatientData> alphabaticalComparator = new Comparator<FormWithPatientData>() {
        @Override
        public int compare(FormWithPatientData lhs, FormWithPatientData rhs) {
            return lhs.getPatientMetaData().compareTo(rhs.getPatientMetaData());
        }
    };

    protected List<PatientMetaData> buildPatientsList(List<T> forms) {
        List<PatientMetaData> result = new ArrayList<PatientMetaData>();
        for (FormWithPatientData form : forms) {
            PatientMetaData patientMetaData = form.getPatientMetaData();
            if (!result.contains(patientMetaData)) {
                result.add(patientMetaData);
            }
        }
        return result;
    }
}
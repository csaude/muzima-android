package com.muzima.adapters.observations;

import static com.muzima.utils.ConceptUtils.getConceptNameFromConceptNamesByLocale;
import static com.muzima.utils.Constants.FGH.Concepts.HEALTHWORKER_ASSIGNMENT_CONCEPT_ID;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Provider;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.model.ObsData;
import com.muzima.model.ObsGroups;
import com.muzima.util.JsonUtils;
import com.muzima.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ObservationGroupAdapter extends BaseTableAdapter {

    private final ObsGroups obsGroup[];
    List<Concept> concepts = new ArrayList<>();
    List<Concept> conceptWithObservations = new ArrayList<>();
    private final LayoutInflater layoutInflater;
    private final String patientUuid;
    private final ConceptController conceptController;
    private final ObservationController observationController;
    boolean isGroupingEnabled;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    private List<String> h;
    private String headers[];
    List<ObsGroups> obsGroups = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    MuzimaApplication app;
    private  Context context;
    private boolean shouldReplaceProviderIdWithNames;
    Map<String, Integer> map = new LinkedHashMap<>( );
    Map<String, String> conceptGroupMap = new LinkedHashMap<>();
    Map<String, List<Observation>> conceptsObservations = new LinkedHashMap<>();


    public List<String> getHeaders() {
        List<String> dates = new ArrayList();
        dates.add("");
        try {
            List<Observation> observations = observationController.getObservationsByPatient(patientUuid);
            Collections.sort(observations, obsDateTimeComparator);
            for (Observation observation : observations) {
                if (observation.getObservationDatetime() != null) {
                    String formattedDate = dateFormat.format(observation.getObservationDatetime());
                    if(dates == null){
                        dates.add(formattedDate);
                    } else if(!dates.contains(formattedDate)){
                        dates.add(formattedDate);
                    }
                }
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s."), e);
        }
        return dates;
    }

    private final Comparator<Observation> obsDateTimeComparator = (lhs, rhs) -> {
        if (lhs.getObservationDatetime()==null)
            return -1;
        if (rhs.getObservationDatetime()==null)
            return 1;
        return -(lhs.getObservationDatetime()
                .compareTo(rhs.getObservationDatetime()));
    };

    private final float density;

    public ObservationGroupAdapter(Context context, String patientUuid) {
        this.app = (MuzimaApplication) context.getApplicationContext();
        layoutInflater = LayoutInflater.from(context);
        this.patientUuid = patientUuid;
        this.conceptController = app.getConceptController();
        this.observationController = app.getObservationController();
        this.context = context;
        shouldReplaceProviderIdWithNames = app.getMuzimaSettingController().isPatientTagGenerationEnabled();

        h = getHeaders();
        headers = h.toArray(new String[0]);

        List<String> conceptUuids = new ArrayList<>();
        List<Object> objects = null;

        String json = "";
        try {
            SetupConfigurationTemplate activeSetupConfig = app.getSetupConfigurationController().getActiveSetupConfigurationTemplate();
            json = activeSetupConfig.getConfigJson();

            isGroupingEnabled = JsonUtils.readAsBoolean(json, "$['config']['requireConceptGroups']");
            if(isGroupingEnabled) {
                objects = JsonUtils.readAsObjectList(json, "$['config']['configConceptGroups']");
                if (objects != null) {
                    for (Object object : objects) {
                        net.minidev.json.JSONObject configConceptGroups = (net.minidev.json.JSONObject) object;

                        net.minidev.json.JSONObject jsonObject = configConceptGroups;
                        List<Object> concepts = JsonUtils.readAsObjectList(configConceptGroups.toJSONString(), "concepts");
                        Object group = jsonObject.get("name");
                        for (Object concept : concepts) {
                            net.minidev.json.JSONObject concept1 = (net.minidev.json.JSONObject) concept;
                            String conceptUuid = concept1.get("uuid").toString();
                            conceptUuids.add(conceptUuid);

                            Concept cpt = conceptController.getConceptByUuid(conceptUuid);
                            if (cpt != null) {
                                List<Observation> observations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, cpt.getId());
                                if (observations.size() > 0) {
                                    if(!groups.contains(group.toString())) {
                                        obsGroups.add(new ObsGroups(group.toString()));
                                        groups.add(group.toString());
                                    }
                                    conceptsObservations.put(conceptUuid,observations);
                                    conceptGroupMap.put(conceptUuid,group.toString());
                                }
                            }
                        }
                    }
                }
            }

            concepts = conceptController.getConcepts();
            boolean isOtherGroupAdded = false;
            for(Concept concept : concepts){
                if(!conceptUuids.contains(concept.getUuid())){
                    if(!isOtherGroupAdded){
                        if (concept != null) {
                            List<Observation> observations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, concept.getId());
                            if (observations.size() > 0) {
                                if(!groups.contains(app.getString(R.string.general_other))) {
                                    obsGroups.add(new ObsGroups(app.getString(R.string.general_other)));
                                    groups.add(app.getString(R.string.general_other));
                                }
                                conceptGroupMap.put(concept.getUuid(),app.getString(R.string.general_other));
                                conceptsObservations.put(concept.getUuid(),observations);
                            }

                        }
                    }
                }
            }
        } catch (ConceptController.ConceptFetchException | ObservationController.LoadObservationException | SetupConfigurationController.SetupConfigurationFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while loading Observations or fetching concepts ",e);
        }

        obsGroup = obsGroups.toArray(new ObsGroups[0]);

        density = context.getResources().getDisplayMetrics().density;
        getObservationByConcept();
    }


    public void getObservationByConcept(){
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            String applicationLanguage = preferences.getString(context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_english));

            List<String> conceptUuids = new ArrayList<>();

            for (Map.Entry<String,List<Observation>> pair : conceptsObservations.entrySet()){
                Concept concept = conceptController.getConceptByUuid(pair.getKey());
                conceptUuids.add(pair.getKey());
                if (concept != null) {
                    List<String> conceptRow = new ArrayList<>();
                    List<Observation> observations = pair.getValue();
                    if (observations.size() > 0) {
                        conceptWithObservations.add(concept);
                        for (String dateString : h) {
                            if (dateString.isEmpty()) {
                                conceptRow.add(getConceptNameFromConceptNamesByLocale(concept.getConceptNames(), applicationLanguage));
                            } else {
                                String value = "";
                                for (Observation observation : observations) {
                                    if (dateString.equals(dateFormat.format(observation.getObservationDatetime()))) {
                                        if (shouldReplaceProviderIdWithNames && observation.getConcept().getId() == HEALTHWORKER_ASSIGNMENT_CONCEPT_ID) {
                                            Provider provider = app.getProviderController().getProviderBySystemId(observation.getValueText());
                                            if (provider != null) {
                                                value = provider.getName();
                                            } else {
                                                value = observation.getValueText();
                                            }
                                        } else {
                                            if (concept.isNumeric()) {
                                                value = String.valueOf(observation.getValueNumeric());
                                            } else if (concept.isCoded()) {
                                                value = getConceptNameFromConceptNamesByLocale(observation.getValueCoded().getConceptNames(), applicationLanguage);
                                            } else if (concept.isDatetime()) {
                                                if(observation.getValueDatetime() != null)
                                                    value = dateFormat.format(observation.getValueDatetime());
                                            } else {
                                                value = observation.getValueText();
                                            }
                                        }
                                    }
                                }
                                if (!StringUtils.isEmpty(value)) {
                                    conceptRow.add(value);
                                } else {
                                    conceptRow.add("");
                                }
                            }
                        }
                        map.put(getConceptNameFromConceptNamesByLocale(concept.getConceptNames(), applicationLanguage), groups.indexOf(conceptGroupMap.get(pair.getKey())));
                        obsGroup[groups.indexOf(conceptGroupMap.get(pair.getKey()))].list.add(new ObsData(conceptRow.toArray(new String[0])));
                    }
                }

            }
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while fetching concepts ",e);
        }
    }

    @Override
    public int getRowCount() {
        return conceptWithObservations.size()+obsGroup.length;
    }

    @Override
    public int getColumnCount() {
        return h.size()-1;
    }

    @Override
    public View getView(int row, int column, View convertView, ViewGroup parent) {
        final View view;
        switch (getItemViewType(row, column)) {
            case 0:
                view = getFirstHeader(row, column, convertView, parent);
                break;
            case 1:
                view = getHeader(row, column, convertView, parent);
                break;
            case 2:
                view = getFirstBody(row, column, convertView, parent);
                break;
            case 3:
                view = getBody(row, column, convertView, parent);
                break;
            case 4:
                view = getGroupView(row, column, convertView, parent);
                break;
            default:
                throw new RuntimeException("Not sure what went wrong");
        }
        return view;
    }

    private View getFirstHeader(int row, int column, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_table_header_first, parent, false);

        }
        ((TextView) convertView.findViewById(R.id.text1)).setText(headers[0]);
        return convertView;
    }

    private View getHeader(int row, int column, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_table_header, parent, false);
        }
        ((TextView) convertView.findViewById(R.id.text1)).setText(headers[column + 1]);
        return convertView;
    }

    private View getFirstBody(int row, int column, View convertView, ViewGroup parent) {

        convertView = layoutInflater.inflate(R.layout.item_table_first, parent, false);
        convertView.setBackgroundResource(map.get(getDevice(row).data[0]) % 2 == 0 ? R.drawable.bg_table_color1 : R.drawable.bg_table_color2);

        ((TextView) convertView.findViewById(android.R.id.text1)).setText(getDevice(row).data[column + 1]);
        return convertView;
    }

    private View getBody(int row, int column, View convertView, ViewGroup parent) {
        convertView = layoutInflater.inflate(R.layout.item_table, parent, false);

        ((TextView) convertView.findViewById(android.R.id.text1)).setText(getDevice(row).data[column + 1]);
        ((TextView) convertView.findViewById(android.R.id.text1)).setBackgroundResource(map.get(getDevice(row).data[0]) % 2 == 0 ? R.drawable.table_border1 : R.drawable.table_border2);

        return convertView;
    }

    private View getGroupView(int row, int column, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_table_group, parent, false);
        }
        final String string;
        if (column == -1) {
            string = getGroup(row).name;
        } else {
            string = "";
        }

        ((TextView) convertView.findViewById(android.R.id.text1)).setText(string);
        convertView.setBackgroundResource(groups.indexOf(getGroup(row).name) % 2 == 0 ? R.drawable.bg_table_color1 : R.drawable.bg_table_color2);
        return convertView;
    }

    @Override
    public int getWidth(int column) {
        return Math.round(120 * density);
    }

    @Override
    public int getHeight(int row) {
        final int height;
        if (row == -1) {
            if(conceptWithObservations.size() == 0)
                height = 0;
            else
                height = 40;
        } else if (isGroup(row)) {
            if(!isGroupingEnabled) {
                height = 0;
            }else{
                height = 40;
            }
        } else {
            height = 60;
        }
        return Math.round(height * density);
    }

    @Override
    public int getItemViewType(int row, int column) {
        final int itemViewType;
        if (row == -1 && column == -1) {
            itemViewType = 0;
        } else if (row == -1) {
            itemViewType = 1;
        } else if (isGroup(row)) {
            itemViewType = 4;
        } else if (column == -1) {
            itemViewType = 2;
        } else {
            itemViewType = 3;
        }
        return itemViewType;
    }

    private boolean isGroup(int row) {
        int group = 0;
        while (row > 0) {
            row -= obsGroup[group].size() + 1;
            group++;
        }
        return row == 0;
    }

    private ObsGroups getGroup(int row) {
        int group = 0;
        while (row >= 0) {
            row -= obsGroup[group].size() + 1;
            group++;
        }
        return obsGroup[group - 1];
    }

    private ObsData getDevice(int row) {
        int group = 0;
        while (row >= 0) {
            row -= obsGroup[group].size() + 1;
            group++;
        }
        group--;
        return obsGroup[group].get(row + obsGroup[group].size());
    }

    @Override
    public int getViewTypeCount() {
        return 5;
    }
}
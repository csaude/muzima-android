/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.reports;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.PatientReport;
import com.muzima.controller.PatientReportController;

public abstract class ReportsAdapter extends ListAdapter<PatientReport> {
    final PatientReportController patientReportController;
    final String patientUuid;
    BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    ReportsAdapter(Context context, int textViewResourceId, PatientReportController patientReportController, String patientUuid) {
        super(context, textViewResourceId);
        this.patientReportController = patientReportController;
        this.patientUuid = patientUuid;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_cohorts_list, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.setTextToName(getItem(position).getName());
        return convertView;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    class ViewHolder {
        private CheckedTextView name;
        private ImageView downloadedImage;
        private ImageView pendingUpdateImage;

        ViewHolder(View convertView) {
            this.downloadedImage = (ImageView) convertView.findViewById(R.id.downloadImg);
            this.pendingUpdateImage = (ImageView) convertView.findViewById(R.id.pendingUpdateImg);
            this.name = (CheckedTextView) convertView.findViewById(R.id.cohort_name);
        }

        void displayDownloadImage() {
            downloadedImage.setVisibility(View.VISIBLE);
        }

        void hideDownloadImage() {
            downloadedImage.setVisibility(View.GONE);
        }

        void hidePendingUpdateImage() {
            pendingUpdateImage.setVisibility(View.GONE);
        }

        void setTextToName(String text) {
            name.setText(text);
        }

        void setDefaultTextColor(){
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            theme.resolveAttribute(R.attr.primaryTextColor, typedValue, true);
            name.setTextColor(typedValue.data);
        }
    }
}

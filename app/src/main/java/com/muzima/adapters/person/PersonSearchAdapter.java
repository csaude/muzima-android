package com.muzima.adapters.person;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.PersonAttribute;
import com.muzima.listners.LoadMoreListener;
import com.muzima.model.patient.PatientItem;
import com.muzima.utils.ViewUtil;
import com.muzima.view.main.HTCMainActivity;

import java.util.Date;
import java.util.List;

public class PersonSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements RecyclerAdapter.BackgroundListQueryTaskListener {

    List<PatientItem> records;

    LoadMoreListener loadMoreListener;

    protected final int VIEW_TYPE_ITEM = 0;
    protected final int VIEW_TYPE_LOADING = 1;

    private HTCMainActivity activity;
    protected boolean isLoading;

    protected int lastVisibleItem, totalItemCount;

    private boolean detailsSection;

    private Context context;

    public PersonSearchAdapter(RecyclerView recyclerView, List<PatientItem> records, Activity activity, Context context) {
        this.records = records;
        this.activity = (HTCMainActivity) activity;
        this.context = context;

        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = linearLayoutManager.getItemCount();
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                if (!isLoading && linearLayoutManager.findLastCompletelyVisibleItemPosition() == records.size() - 1) {
                    if (loadMoreListener != null) {
                        loadMoreListener.onLoadMore();
                    }
                    isLoading = true;
                }
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return records.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void setLoaded() {
        isLoading = false;
    }

    public boolean isLoading() {
        return isLoading;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == VIEW_TYPE_ITEM) {
             view = layoutInflater.inflate(R.layout.person_list_item, parent, false);
            return new PersonViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            view = layoutInflater.inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof PersonViewHolder){
            PersonViewHolder personViewHolder = (PersonViewHolder) holder;
            Patient patient = records.get(position).getPatient();
            personViewHolder.name.setText(patient.getDisplayName());

            personViewHolder.details.animate().setDuration(200).rotation(180);

            personViewHolder.details.animate().setDuration(200).rotation(180);

            Date dob = patient.getBirthdate();
            if(dob != null) {
               // personViewHolder.age.setText(context.createConfigurationContext(configuration).getResources().getString(R.string.general_years ,String.format(Locale.getDefault(), "%d ", DateUtils.calculateAge(dob))));
            }else{
                personViewHolder.age.setText(String.format(""));
            }

            personViewHolder.identifier.setText(patient.getIdentifier());
            if (patient.getGender() != null) {
                personViewHolder.sex.setImageResource(patient.getGender().equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.gender_female);
            }
            for (PersonAddress address : patient.getAddresses()){
                if (address.isPreferred()) {
                    personViewHolder.address.setText(address.getAddress1());
                }
            }

            PersonAttribute attribute = patient.getAttribute("e2e3fd64-1d5f-11e0-b929-000c29ad1d07");
            if (attribute != null) {
                personViewHolder.contact.setText(attribute.getAttribute());
            }

            //personViewHolder.dateCreated;

            personViewHolder.createHTC.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

            personViewHolder.details.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        detailsSection = !detailsSection;
                        if (!detailsSection) {
                            personViewHolder.moreDetailsLyt.setVisibility(View.VISIBLE);
                            personViewHolder.divider.setBackgroundColor(context.getResources().getColor(R.color.person_item_back));
                            ViewUtil.collapse(personViewHolder.moreDetailsLyt);
                            personViewHolder.details.animate().setDuration(200).rotation(0);
                        }
                        else {
                            personViewHolder.moreDetailsLyt.setVisibility(View.GONE);
                            personViewHolder.divider.setBackgroundColor(context.getResources().getColor(R.color.person_item_divider));
                            ViewUtil.expand(personViewHolder.moreDetailsLyt);
                            personViewHolder.details.animate().setDuration(200).rotation(180);
                        }
                }
            });


        }else
        if (holder instanceof LoadingViewHolder){
            showLoadingView((LoadingViewHolder) holder, position);
        }
    }

    public void setLoadMoreListener(LoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }

    public LoadMoreListener getLoadMoreListener() {
        return loadMoreListener;
    }

    public HTCMainActivity getActivity() {
        return activity;
    }

    @Override
    public void onQueryTaskStarted() {

    }

    @Override
    public void onQueryTaskFinish() {

    }

    @Override
    public void onQueryTaskCancelled() {

    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {

    }

    public class PersonViewHolder extends RecyclerView.ViewHolder{

        ImageView sex;
        TextView name;
        TextView identifier;
        TextView age;
        TextView dateCreated;
        TextView contact;
        TextView address;

        ImageButton createHTC;
        ImageButton details;
        View divider;

        LinearLayout moreDetailsLyt;


        public PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            sex = itemView.findViewById(R.id.person_sex);
            name = itemView.findViewById(R.id.name);
            identifier = itemView.findViewById(R.id.identifier);
            age = itemView.findViewById(R.id.person_age);
            //dateCreated = itemView.findViewById(R.id.date_created);
            contact = itemView.findViewById(R.id.contact);
            address = itemView.findViewById(R.id.address);
            createHTC = itemView.findViewById(R.id.create_htc);
            details = itemView.findViewById(R.id.details);
            divider = itemView.findViewById(R.id.divider);
            moreDetailsLyt = itemView.findViewById(R.id.person_more_details);
        }
    }

    public class LoadingViewHolder extends RecyclerView.ViewHolder {

        ProgressBar progressBar;
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);

        }
    }

    protected void showLoadingView(PersonSearchAdapter.LoadingViewHolder viewHolder, int position) {}
}
package mz.org.csaude.emuzima.view.cohort;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import mz.org.csaude.emuzima.R;
import mz.org.csaude.emuzima.adapters.cohort.CohortsPagerAdapter;
import mz.org.csaude.emuzima.model.events.CohortSearchEvent;
import mz.org.csaude.emuzima.model.events.CohortsDownloadedEvent;
import mz.org.csaude.emuzima.model.events.DestroyActionModeEvent;
import mz.org.csaude.emuzima.scheduler.MuzimaJobScheduleBuilder;
import mz.org.csaude.emuzima.utils.Constants.DataSyncServiceConstants;
import mz.org.csaude.emuzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import mz.org.csaude.emuzima.utils.LanguageUtil;
import mz.org.csaude.emuzima.utils.StringUtils;
import mz.org.csaude.emuzima.utils.ThemeUtils;
import mz.org.csaude.emuzima.view.custom.ActivityWithBottomNavigation;
import org.greenrobot.eventbus.EventBus;

public class CohortPagerActivity extends ActivityWithBottomNavigation {
    private ViewPager viewPager;
    private EditText searchCohorts;
    private final LanguageUtil languageUtil = new LanguageUtil();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ActionMenuItemView refreshMenuActionView;
    private ActionMenuItemView syncReportMenuActionView;
    private Drawable syncReportMenuIconDrawable;
    private Animation refreshIconRotateAnimation;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onSyncReceived(context, intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this, true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_cohort_pager);
        loadBottomNavigation();

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        CohortsPagerAdapter cohortsPager = new CohortsPagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(cohortsPager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                searchCohorts.setText(StringUtils.EMPTY);
                if (EventBus.getDefault().hasSubscriberForEvent(DestroyActionModeEvent.class)) {
                    EventBus.getDefault().post(new DestroyActionModeEvent());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        searchCohorts = findViewById(R.id.search_cohorts);
        searchCohorts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                EventBus.getDefault().post(new CohortSearchEvent(charSequence.toString(), viewPager.getCurrentItem()));
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        refreshIconRotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
        refreshIconRotateAnimation.setRepeatCount(Animation.INFINITE);
        refreshMenuActionView = findViewById(R.id.menu_load);
        syncReportMenuActionView = findViewById(R.id.menu_sync_report);

        refreshMenuActionView.setOnClickListener(view -> processSync(refreshIconRotateAnimation));
        syncReportMenuActionView.setOnClickListener(view -> showBackgroundSyncProgressDialog(this));
        syncReportMenuActionView.setVisibility(View.GONE);

        Toolbar toolbar = findViewById(R.id.cohort_pager_toolbar);
        syncReportMenuIconDrawable = toolbar.getMenu().findItem(R.id.menu_sync_report).getIcon();
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setTitle(StringUtils.EMPTY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
        if (isDataSyncRunning()) {
            refreshMenuActionView.startAnimation(refreshIconRotateAnimation);
        } else {
            refreshMenuActionView.clearAnimation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MESSAGE_SENT_ACTION);
        filter.addAction(PROGRESS_UPDATE_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    protected void onSyncReceived(Context context, Intent intent) {
        new Thread(() -> {
            int syncStatus = intent.getIntExtra(DataSyncServiceConstants.SYNC_STATUS, SyncStatusConstants.UNKNOWN_ERROR);
            int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);
            int downloadCount = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, 0);
            String msg = "";

            if (syncStatus == SyncStatusConstants.SUCCESS) {
                switch (syncType) {
                    case DataSyncServiceConstants.SYNC_COHORTS_METADATA:
                        msg = getString(R.string.info_new_cohort_download, downloadCount);
                        break;
                    case DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_FULL_DATA:
                        int downloadCountSec = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
                        msg = getString(R.string.info_cohort_new_patient_download, downloadCount, downloadCountSec);
                        break;
                    case DataSyncServiceConstants.SYNC_ENCOUNTERS:
                        msg = getString(R.string.info_new_encounter_download, downloadCount);
                        EventBus.getDefault().post(new CohortsDownloadedEvent(true));
                        break;
                }
            } else {
                msg = getString(R.string.info_download_complete, syncStatus);
            }

            String finalMsg = msg;
            mainHandler.post(() -> Toast.makeText(CohortPagerActivity.this, finalMsg, Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void processSync(Animation rotation) {
        if (!isDataSyncRunning()) {
            mainHandler.post(() -> {
                Toast.makeText(getApplicationContext(), getString(R.string.info_muzima_sync_service_in_progress), Toast.LENGTH_LONG).show();
                refreshMenuActionView.startAnimation(rotation);
                syncReportMenuActionView.setVisibility(View.GONE);
                showBackgroundSyncProgressDialog(CohortPagerActivity.this);
            });

            new Thread(() -> {
                mainHandler.post(() -> new MuzimaJobScheduleBuilder(getApplicationContext()).schedulePeriodicBackgroundJob(1000, true));
                notifySyncStarted();
            }).start();
        } else {
            mainHandler.post(() -> showBackgroundSyncProgressDialog(CohortPagerActivity.this));
        }
    }


    @Override
    protected int getBottomNavigationMenuItemId() {
        return R.id.action_cohorts;
    }
}

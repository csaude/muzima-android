package mz.org.csaude.muzimamobile.scheduler;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import mz.org.csaude.muzimamobile.MuzimaApplication;
import mz.org.csaude.muzimamobile.R;

import static android.content.Context.JOB_SCHEDULER_SERVICE;
import static mz.org.csaude.muzimamobile.utils.Constants.DataSyncServiceConstants.MuzimaJobSchedulerConstants.MESSAGE_SYNC_JOB_ID;
import static mz.org.csaude.muzimamobile.utils.Constants.DataSyncServiceConstants.MuzimaJobSchedulerConstants.MUZIMA_JOB_PERIODIC;

public class MuzimaJobScheduleBuilder {
    private final MuzimaApplication muzimaApplication;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());  // Ensures UI tasks run on the main thread

    public MuzimaJobScheduleBuilder(Context context) {
        this.muzimaApplication = (MuzimaApplication) context.getApplicationContext();
        this.context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void schedulePeriodicBackgroundJob(int delay, boolean isManualSync) {
        if (isManualSync) {
            mainHandler.postDelayed(() -> {
                if (!isJobAlreadyScheduled(context)) {
                    showToast(context.getResources().getString(R.string.info_muzima_sync_service_in_progress));
                    handleScheduledPeriodicDataSyncJob();
                } else {
                    showToast(context.getResources().getString(R.string.general_sync_service_already_running));
                }
            }, delay);
        } else {
            if (muzimaApplication.getMuzimaSettingController().isRealTimeSyncEnabled()) {
                mainHandler.postDelayed(() -> {
                    if (!isJobAlreadyScheduled(context)) {
                        handleScheduledPeriodicDataSyncJob();
                    }
                    mainHandler.postDelayed(this::handleScheduledPeriodicDataSyncJob, MUZIMA_JOB_PERIODIC);
                }, delay);
            }
        }
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static boolean isJobAlreadyScheduled(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (scheduler != null) {
            for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
                if (jobInfo.getId() == MESSAGE_SYNC_JOB_ID) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleScheduledPeriodicDataSyncJob() {
        ComponentName componentName = new ComponentName(context, MuzimaJobScheduler.class);
        JobInfo jobInfo = new JobInfo.Builder(MESSAGE_SYNC_JOB_ID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(MUZIMA_JOB_PERIODIC)
                .build();

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.schedule(jobInfo);
        }
    }
}

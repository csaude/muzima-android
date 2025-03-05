package mz.org.csaude.emuzima.controller;

import com.muzima.api.model.MuzimaCohortExecutionStatus;
import com.muzima.api.service.MuzimaCohortExecutionStatusService;

import java.io.IOException;
import java.util.List;

import mz.org.csaude.emuzima.MuzimaApplication;
import mz.org.csaude.emuzima.utils.Constants;

public class MuzimaCohortExecutionStatusController {

    private MuzimaCohortExecutionStatusService cohortExecutionStatusService;

    public MuzimaCohortExecutionStatusController(MuzimaApplication muzimaApplication) throws IOException {
        this. cohortExecutionStatusService = muzimaApplication.getMuzimaContext().getMuzimaCohortExecutionStatusService();
    }

    public int[] cohortInExecution() throws MuzimaCohortExecutionStatusFetchException {
        try {
            int[] result = new int[1];
            result[0] = -1;
            List<MuzimaCohortExecutionStatus> cohortExecutionStatuses = cohortExecutionStatusService.downloadCurrentCohortExecutionStatuses();
            for (MuzimaCohortExecutionStatus cohortExecutionStatus : cohortExecutionStatuses) {
                if (cohortExecutionStatus.getExecutionStatus().equals(Constants.CohortExecution.RUNNING)) {
                    result[0] = 1;
                    return result;
                }
            }
            return result;
        } catch (IOException e) {
            throw new MuzimaCohortExecutionStatusFetchException(e);
        }
    }

    public static class MuzimaCohortExecutionStatusFetchException extends Throwable {
        public MuzimaCohortExecutionStatusFetchException(Throwable throwable) {
            super(throwable);
        }
    }
}

package mz.org.csaude.emuzima.controller;

import android.util.Log;

import com.muzima.api.model.HTCPerson;
import com.muzima.api.service.HTCPersonService;
import com.muzima.api.service.MuzimaHtcFormService;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mz.org.csaude.emuzima.MuzimaApplication;
import mz.org.csaude.emuzima.service.MuzimaLoggerService;
import mz.org.csaude.emuzima.utils.Constants;

public class HTCPersonController {
    private static final String TAG = HTCPersonController.class.getSimpleName();
    private final HTCPersonService htcPersonService;
    private final MuzimaHtcFormService htcFormService;
    private final MuzimaApplication muzimaApplication;

    public HTCPersonController(HTCPersonService htcPersonService, MuzimaHtcFormService htcFormService, MuzimaApplication muzimaApplication) {
        this.htcPersonService = htcPersonService;
        this.htcFormService = htcFormService;
        this.muzimaApplication = muzimaApplication;
    }

    public HTCPerson getHTCPerson(String uuid) {
        try {
            return htcPersonService.getHTCPersonByUuid(uuid);
        } catch (IOException e) {
            Log.e(TAG, "Error retrieving HTCPerson with UUID: " + uuid, e);
            return null;
        }
    }

    public List<HTCPerson> searchPersonOnServer(String name) {
        try {
            return htcPersonService.downloadPersonByName(name);
        } catch (IOException e) {
            Log.e(TAG, "Error searching for person on the server by name: " + name, e);
            return new ArrayList<>();
        }
    }

    public void saveHTCPerson(HTCPerson htcPerson) {
        try {
            htcPerson.setSyncStatus(Constants.STATUS_COMPLETE);
            htcPersonService.saveHTCPerson(htcPerson);
        } catch (IOException e) {
            Log.e(TAG, "Error saving HTCPerson with UUID: " + htcPerson.getUuid(), e);
        }
    }

    public List<HTCPerson> searchPersons(String parameter) {
        try {
            return htcPersonService.search(parameter, false);
        } catch (IOException | ParseException e) {
            Log.e(TAG, "Error searching for persons with parameter: " + parameter, e);
            return new ArrayList<>();
        }
    }

    public List<HTCPerson> getLatestHTCPersons() {
        try {
            return htcPersonService.getAllHTCPersons();
        } catch (IOException | ParseException e) {
            Log.e(TAG, "Error retrieving latest HTCPersons.", e);
            return new ArrayList<>();
        }
    }

    public void updateHTCPerson(HTCPerson htcPerson) {
        try {
            htcPersonService.updateHTCPerson(htcPerson);
        } catch (IOException e) {
            Log.e(TAG, "Error updating HTCPerson with UUID: " + htcPerson.getUuid(), e);
        }
    }

    public List<HTCPerson> downloadHtcPersonsOfProvider(String providerUuid) {
        try {
            return htcPersonService.downloadHtcPersonsOfProvider(providerUuid);
        } catch (IOException e) {
            Log.e(TAG, "Error downloading HTCPersons for provider UUID: " + providerUuid, e);
            return new ArrayList<>();
        }
    }

    public void saveHtcPersons(List<HTCPerson> htcPersonList) {
        try {
            htcPersonService.saveHTCPersons(htcPersonList);
        } catch (IOException e) {
            Log.e(TAG, "Error saving list of HTCPersons.", e);
        }
    }

    public boolean uploadAllPendingHtcData() throws UploadHtcDataException {
        boolean result = false;
        try {
            List<HTCPerson> htcPersonList = htcPersonService.getBySyncStatus(Constants.STATUS_COMPLETE);
            for (HTCPerson person : htcPersonList) {
                person.setHtcForm(htcFormService.getHTCFormByHTCPersonUuid(person.getUuid()));
                if (htcPersonService.syncHtcData(person)) {
                    person.setSyncStatus(Constants.STATUS_UPLOADED);
                    htcPersonService.updateHTCPerson(person);
                    result = true;
                    MuzimaLoggerService.log(muzimaApplication, "SYNCED_HTC_DATA", "{\"htcPersonUuid\":\"" + person.getUuid() + "\"}");
                } else {
                    result = false;
                }
            }
        } catch (IOException | ParseException e) {
            throw new UploadHtcDataException(e);
        }
        return result;
    }

    public void deleteHtcPersonPendingDeletion() {
        // Implementation for deleting HTCPersons pending deletion
        // Placeholder: Add actual deletion logic
    }

    // Custom Exception Classes

    public static class HTCPersonDownloadException extends Exception {
        public HTCPersonDownloadException(Throwable cause) {
            super(cause);
        }
    }

    public static class HTCPersonSaveException extends Exception {
        public HTCPersonSaveException(Throwable cause) {
            super(cause);
        }
    }

    public static class HTCPersonFetchException extends Exception {
        public HTCPersonFetchException(Throwable cause) {
            super(cause);
        }
    }

    public static class HTCPersonDeleteException extends Exception {
        public HTCPersonDeleteException(Throwable cause) {
            super(cause);
        }
    }

    public static class ParseHTCPersonException extends Exception {
        public ParseHTCPersonException(Throwable cause) {
            super(cause);
        }

        public ParseHTCPersonException(String message) {
            super(message);
        }
    }

    public static class UploadHtcDataException extends Exception {
        public UploadHtcDataException(Throwable cause) {
            super(cause);
        }
    }

    public static class HtcFetchException extends Exception {
        public HtcFetchException(Throwable cause) {
            super(cause);
        }
    }
}

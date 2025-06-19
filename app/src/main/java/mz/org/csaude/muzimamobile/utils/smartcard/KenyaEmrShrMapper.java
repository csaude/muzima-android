/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package mz.org.csaude.muzimamobile.utils.smartcard;

import android.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.org.csaude.muzimamobile.MuzimaApplication;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Location;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonName;

import mz.org.csaude.muzimamobile.controller.ConceptController;
import mz.org.csaude.muzimamobile.controller.EncounterController;
import mz.org.csaude.muzimamobile.controller.FormController;
import mz.org.csaude.muzimamobile.controller.ObservationController;

import mz.org.csaude.muzimamobile.model.observation.EncounterWithObservations;
import mz.org.csaude.muzimamobile.model.observation.Encounters;
import mz.org.csaude.muzimamobile.model.shr.kenyaemr.ExternalPatientId;
import mz.org.csaude.muzimamobile.model.shr.kenyaemr.HIVTest;
import mz.org.csaude.muzimamobile.model.shr.kenyaemr.Immunization;
import mz.org.csaude.muzimamobile.model.shr.kenyaemr.InternalPatientId;
import mz.org.csaude.muzimamobile.model.shr.kenyaemr.KenyaEmrSHRModel;
import mz.org.csaude.muzimamobile.model.shr.kenyaemr.PatientIdentification;
import mz.org.csaude.muzimamobile.model.shr.kenyaemr.ProviderDetails;
import mz.org.csaude.muzimamobile.service.HTMLFormObservationCreator;
import mz.org.csaude.muzimamobile.utils.Constants;
import mz.org.csaude.muzimamobile.utils.PatientIdentifierUtils;
import mz.org.csaude.muzimamobile.utils.StringUtils;
import mz.org.csaude.muzimamobile.utils.DateUtils;
import mz.org.csaude.muzimamobile.utils.LocationUtils;

import mz.org.csaude.muzimamobile.view.forms.HTMLPatientJSONMapper;

import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class KenyaEmrShrMapper {

    /**
     * Converts an SHR model from JSON representation to KenyaEmrSHRModel
     * @param jsonSHRModel the JSON representation of the SHR model
     * @return Representation of the JSON input as KenyaEmrSHRModel object
     * @throws IOException
     */
    public static KenyaEmrSHRModel createSHRModelFromJson(String jsonSHRModel) throws ShrParseException {
        ObjectMapper objectMapper = new ObjectMapper();
        KenyaEmrSHRModel SHRModel = null;
        try {
            SHRModel = objectMapper.readValue(jsonSHRModel,KenyaEmrSHRModel.class);
        } catch (IOException e) {
            throw new ShrParseException(e);
        }
        return SHRModel;
    }

    /**
     * Extracts a Patient object from a JSON SHR model
     * @param SHRModel the JSON representation of the SHR model
     * @return Patient object extracted from SHR model
     * @throws IOException
     */
    public static Patient extractPatientFromSHRModel(MuzimaApplication muzimaApplication,String SHRModel) throws ShrParseException{
        KenyaEmrSHRModel kenyaEmrSHRModel = createSHRModelFromJson(SHRModel);
        if(kenyaEmrSHRModel.getCardDetails().getLastUpdated() != null && kenyaEmrSHRModel.getPatientIdentification().getDateOfBirth() != null)
            return extractPatientFromSHRModel(muzimaApplication, kenyaEmrSHRModel);
        return null;
    }

    /**
     * Extracts a Patient Object from a KenyaEmrSHRModel Object of SHR model
     * @param SHRModel the KenyaEmrSHRModel Object representation of the SHR model
     * @return Patient object extracted from SHR model
     * @throws IOException
     */
    private static Patient extractPatientFromSHRModel(MuzimaApplication muzimaApplication, KenyaEmrSHRModel SHRModel) throws ShrParseException{
        try {
            Patient patient = new Patient();
            PatientIdentification identification = SHRModel.getPatientIdentification();
            List<PersonName> names = extractPatientNamesFromSHRModel(SHRModel);
            patient.setNames(new ArrayList<>(names));

            //set Identifiers
            List<PatientIdentifier> identifiers = extractPatientIdentifiersFromSHRModel(muzimaApplication, SHRModel);
            if(!identifiers.isEmpty()) {
                patient.setIdentifiers(new ArrayList<>(identifiers));
            }

            //date of birth
            Date dob = DateUtils.parseDateByPattern(identification.getDateOfBirth(),"yyyyMMdd");
            patient.setBirthdate(dob);

            //Gender
            String gender = identification.getSex();
            if(gender.equalsIgnoreCase("M")){
                patient.setGender("M");
            } else if (gender.equalsIgnoreCase("F")){
                patient.setGender("F");
            } else {
                throw new ShrParseException("Could not determine gender from SHR model");
            }
            return patient;
        } catch (ParseException e){
            throw new ShrParseException(e);
        }
    }

    public static void createAndSaveRegistrationPayloadForPatient(MuzimaApplication muzimaApplication, final Patient patient){
        try {
            FormData formData = new FormData() {{
                setUuid(UUID.randomUUID().toString());
                setPatientUuid(patient.getUuid());
                setUserUuid("userUuid");
                setStatus(Constants.STATUS_COMPLETE);
                setTemplateUuid(Constants.Shr.KenyaEmr.REGISTRATION.FORM.FORM_UUID);
                setDiscriminator(Constants.FORM_JSON_DISCRIMINATOR_SHR_REGISTRATION);
            }};

            formData.setJsonPayload(new HTMLPatientJSONMapper().map(muzimaApplication, patient, formData, true, StringUtils.EMPTY));
            muzimaApplication.getFormController().saveFormData(formData);
        } catch (Throwable E){
            Log.e("Kenya EMR Shr", "Could not create and save registration payload");
        }
    }

    /**
     *
     * @param SHRModel
     * @return
     */
    private static List<PersonName> extractPatientNamesFromSHRModel(KenyaEmrSHRModel SHRModel) throws ShrParseException {
        PatientIdentification identification = SHRModel.getPatientIdentification();
        if(identification != null && identification.getPatientName()!= null) {
            final PersonName personName = new PersonName();
            personName.setFamilyName(identification.getPatientName().getFirstName());
            personName.setGivenName(identification.getPatientName().getLastName());
            personName.setMiddleName(identification.getPatientName().getMiddleName());

            return new ArrayList<PersonName>(){{
                add(personName);
            }};
        } else {
            throw new ShrParseException("Could not find patient names");
        }
    }

    private static List<PatientIdentifier> extractPatientIdentifiersFromSHRModel(MuzimaApplication muzimaApplication, KenyaEmrSHRModel shrModel){
        List<PatientIdentifier> identifiers = new ArrayList<>();
        try {

            PatientIdentification identification = shrModel.getPatientIdentification();
            PatientIdentifier patientIdentifier;
            //External ID
            ExternalPatientId externalPatientId = identification.getExternalPatientId();
            if(!externalPatientId.lacksMandatoryValues()) {
                patientIdentifier = PatientIdentifierUtils.getOrCreateKenyaEmrIdentifier(muzimaApplication, externalPatientId.getID(),
                        externalPatientId.getIdentifierType(), externalPatientId.getAssigningFacility());
                identifiers.add(patientIdentifier);
            }

            //Internal IDs
            List<InternalPatientId> internalPatientIds = identification.getInternalPatientIds();
            for (InternalPatientId internalPatientId : internalPatientIds) {
                if(!internalPatientId.lacksMandatoryValues()) {
                    patientIdentifier = PatientIdentifierUtils.getOrCreateKenyaEmrIdentifier(muzimaApplication, internalPatientId.getID(),
                            internalPatientId.getIdentifierType(), internalPatientId.getAssigningFacility());
                    identifiers.add(patientIdentifier);
                }
            }

        } catch (Exception e){
            Log.e("KenyaEmrShrMapper","Could not create Kenyaemr identifier",e);
        }

        return identifiers;
    }

    public static void createNewObservationsAndEncountersFromShrModel(MuzimaApplication muzimaApplication, KenyaEmrSHRModel shrModel, final Patient patient)
            throws ShrParseException {
        Log.e("KenyaEmrShrMapper","Saving encounters data ");
        List<String> payloads = createJsonEncounterPayloadFromShrModel(muzimaApplication, shrModel, patient);
        for(final String payload:payloads) {
            Log.e("KenyaEmrShrMapper","Saving payload data ");
            final String newFormDataUuid = UUID.randomUUID().toString();
            HTMLFormObservationCreator htmlFormObservationCreator = new HTMLFormObservationCreator(muzimaApplication, false, false);
            htmlFormObservationCreator.createObservationsAndRelatedEntities(payload, newFormDataUuid);

            List<Concept> newConcepts = new ArrayList(htmlFormObservationCreator.getNewConceptList());
            if(!newConcepts.isEmpty()){
                ConceptController conceptController = muzimaApplication.getConceptController();
                try {
                    conceptController.saveConcepts(newConcepts);
                } catch (ConceptController.ConceptSaveException e) {
                    Log.e("ShrMapper","Could not save new Concepts",e);
                }
            }

            Encounter encounter = htmlFormObservationCreator.getEncounter();
            EncounterController encounterController = muzimaApplication.getEncounterController();
            try {
                encounterController.saveEncounter(encounter);
            } catch (EncounterController.SaveEncounterException e) {
                Log.e("ShrMapper","Could not save Encounter",e);
            }

            List<Observation> observations = htmlFormObservationCreator.getObservations();
            ObservationController observationController = muzimaApplication.getObservationController();
            try {
                observationController.saveObservations(observations);
            } catch (ObservationController.SaveObservationException e) {
                Log.e("ShrMapper","Could not save Observations",e);
            }

            final FormData formData = new FormData( ) {{
                setUuid(newFormDataUuid);
                setPatientUuid(patient.getUuid( ));
                setUserUuid("userUuid");
                setStatus(Constants.STATUS_COMPLETE);
                setTemplateUuid(StringUtils.defaultString(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.FORM.FORM_UUID));
                setDiscriminator(Constants.FORM_JSON_DISCRIMINATOR_SHR_ENCOUNTER);
                setJsonPayload(payload);
            }};

            FormController formController = muzimaApplication.getFormController();
            try {
                Log.e("KenyaEmrShrMapper","Saving form data ");
                formController.saveFormData(formData);
            } catch (FormController.FormDataSaveException e) {
                Log.e("ShrMapper","Could not save Form Data",e);
            }
        }
    }

    private static List<String> createJsonEncounterPayloadFromShrModel(MuzimaApplication muzimaApplication, KenyaEmrSHRModel shrModel, Patient patient) throws ShrParseException {
        try {
            Log.e("KenyaEmrShrMapper","Obtaining payloads ");

            List<String> encounterPayloads = new ArrayList<>();
            List<HIVTest> hivTests = shrModel.getHivTests();
            List<Immunization> immunizations = shrModel.getImmunizations();
            if(hivTests != null || immunizations != null) {
                Encounters encountersWithObservations = muzimaApplication.getObservationController().getEncountersWithObservations(patient.getUuid());

                List<HIVTest> existingHivTests = new ArrayList<>();
                List<Immunization> existingImmunizations = new ArrayList<>();
                for (EncounterWithObservations encounterWithObservations : encountersWithObservations) {
                    HIVTest hivTest = getHivTestFromEncounter(encounterWithObservations);
                    if(hivTest != null && !hivTest.lacksMandatoryValues()){
                        existingHivTests.add(hivTest);
                    }

                    Immunization immunization = getImmunizationFromEncounter(encounterWithObservations);
                    if(immunization != null && !immunization.lacksMandatoryValues()){
                        existingImmunizations.add(immunization);
                    }
                }

                if (hivTests != null) {
                    for (HIVTest hivTest : hivTests) {
                        if (!hivTest.lacksMandatoryValues()) {
                            boolean testExists = false;
                            for(HIVTest existingHivTest: existingHivTests){
                                if(hivTest.equals(existingHivTest)){
                                    testExists = true;
                                    break;
                                }
                            }

                            if(!testExists) {
                                encounterPayloads.add(createJsonEncounterPayloadFromHivTest(muzimaApplication, hivTest, patient));
                            }
                        }
                    }
                } else {
                    Log.e("KenyaEmrShrMapper", "No HIV Tests found");
                }

                if (immunizations != null) {
                    for (Immunization immunization : immunizations) {
                        if (!immunization.lacksMandatoryValues()) {
                            boolean immunizationExists = false;
                            for(Immunization existingImmunization : existingImmunizations){
                                if(immunization.equals(existingImmunization)){
                                    immunizationExists = true;
                                    break;
                                }
                            }

                            if(!immunizationExists) {
                                encounterPayloads.add(createJsonEncounterPayloadFromImmunization(muzimaApplication, immunization, patient));
                            }
                        }
                    }
                } else {
                    Log.e("KenyaEmrShrMapper", "No Immunizations found");
                }
            }
            return encounterPayloads;
        } catch(ObservationController.LoadObservationException e){
            throw new ShrParseException("Could not load observations",e);
        }
    }

    private static String createJsonEncounterPayloadFromHivTest(MuzimaApplication muzimaApplication, HIVTest hivTest, Patient patient) throws ShrParseException {
        JSONObject encounterJSON = new JSONObject();
        JSONObject patientDetails = new JSONObject();
        JSONObject observationDetails = new JSONObject();
        JSONObject encounterDetails = new JSONObject();

        Log.e("KenyaEmrShrMapper","Processing HIV test ");

        try {
            encounterDetails.put("encounter.provider_id", hivTest.getProviderDetails().getId());
            Location location = LocationUtils.getOrCreateDummyLocationByKenyaEmrMasterFacilityListCode(muzimaApplication, hivTest.getFacility());
            encounterDetails.put("encounter.location_id", location.getId());

            String user_system_id = muzimaApplication.getAuthenticatedUser( ).getSystemId( );
            encounterDetails.put("encounter.user_system_id", user_system_id);

            Date encounterDateTime = DateUtils.parseDateByPattern(hivTest.getDate(), "yyyyMMdd");
            encounterDetails.put("encounter.encounter_datetime", DateUtils.getFormattedDate(encounterDateTime));

            encounterDetails.put("encounter.form_uuid", StringUtils.defaultString(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.FORM.FORM_UUID));
            encounterJSON.put("encounter", encounterDetails);

            patientDetails.put("patient.medical_record_number", StringUtils.defaultString(patient.getIdentifier()));
            patientDetails.put("patient.given_name", StringUtils.defaultString(patient.getGivenName()));
            patientDetails.put("patient.middle_name", StringUtils.defaultString(patient.getMiddleName()));
            patientDetails.put("patient.family_name", StringUtils.defaultString(patient.getFamilyName()));
            patientDetails.put("patient.sex", StringUtils.defaultString(patient.getGender()));
            patientDetails.put("patient.uuid", StringUtils.defaultString(patient.getUuid()));
            if (patient.getBirthdate() != null) {
                patientDetails.put("patient.birth_date", DateUtils.getFormattedDate(patient.getBirthdate()));
            }

            encounterJSON.put("patient", patientDetails);

            //Test Result
            String answer = null;
            String testResult = hivTest.getResult();
            switch (testResult) {
                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.POSITIVE.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.POSITIVE.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.POSITIVE.name + "^" + "99DCT";
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.NEGATIVE.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.NEGATIVE.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.NEGATIVE.name + "^" + "99DCT";
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.INCONCLUSIVE.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.INCONCLUSIVE.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.INCONCLUSIVE.name + "^" + "99DCT";
            }
            if (!StringUtils.isEmpty(answer)) {
                String conceptQuestion = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.concept_id + "^"
                        + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.name + "^" + "99DCT";
                observationDetails.put(conceptQuestion, answer);
            }

            //Test Type
            answer = null;
            String testType = hivTest.getType();
            switch (testType) {
                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.name + "^" + "99DCT";
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.name + "^" + "99DCT";
            }
            if (!StringUtils.isEmpty(answer)) {
                String conceptQuestion = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.concept_id + "^"
                        + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.name + "^" + "99DCT";
                observationDetails.put(conceptQuestion, answer);
            }

            //Test Strategy
            answer = null;
            String testStrategy = hivTest.getStrategy();
            switch (testStrategy) {
                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HP.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HP.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HP.name + "^" + "99DCT";
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.NP.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.NP.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.NP.name + "^" + "99DCT";
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VI.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VI.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VI.name + "^" + "99DCT";
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VS.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VS.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VS.name + "^" + "99DCT";
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HB.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HB.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HB.name + "^" + "99DCT";
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.MO.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.MO.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.MO.name + "^" + "99DCT";
                    break;
            }
            if (!StringUtils.isEmpty(answer)) {
                String conceptQuestion = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.concept_id + "^"
                        + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.name + "^" + "99DCT";
                observationDetails.put(conceptQuestion, answer);
            }

            //Test Facility
            String facility = hivTest.getFacility();
            if (!StringUtils.isEmpty(facility)) {
                String conceptQuestion = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_FACILITY.concept_id + "^"
                        + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_FACILITY.name + "^" + "99DCT";
                observationDetails.put(conceptQuestion, facility);
            }

            //Test Details
            ProviderDetails providerDetails = hivTest.getProviderDetails();
            if (providerDetails != null) {
                String conceptQuestion = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.NAME.concept_id + "^"
                        + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.NAME.name + "^" + "99DCT";
                observationDetails.put(conceptQuestion, providerDetails.getName());

                conceptQuestion = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.ID.concept_id + "^"
                        + Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.ID.name + "^" + "99DCT";
                observationDetails.put(conceptQuestion, providerDetails.getId());
            }

            encounterJSON.put("patient", patientDetails);
            encounterJSON.put("observation", observationDetails);
            encounterJSON.put("encounter", encounterDetails);

            return encounterJSON.toString();
        } catch (Exception e){
            throw new ShrParseException(e);
        }
    }
    private static String createJsonEncounterPayloadFromImmunization(MuzimaApplication muzimaApplication, Immunization immunization, Patient patient) throws ShrParseException {
        try {
            JSONObject encounterJSON = new JSONObject();
            JSONObject patientDetails = new JSONObject();
            JSONObject observationDetails = new JSONObject();
            JSONObject encounterDetails = new JSONObject();

            Log.e("KenyaEmrShrMapper", "Processing Immunization ");


            encounterDetails.put("encounter.provider_id", Constants.Shr.KenyaEmr.DEFAULT_SHR_USER.id);
            Location location = LocationUtils.getOrCreateDummyLocationByKenyaEmrMasterFacilityListCode(muzimaApplication, Constants.Shr.KenyaEmr.DEFAULT_SHR_FACILITY.MFL_CODE);
            encounterDetails.put("encounter.location_id", location.getId());

            String user_system_id = muzimaApplication.getAuthenticatedUser( ).getSystemId( );
            encounterDetails.put("encounter.user_system_id", user_system_id);
            encounterDetails.put("encounter.location_id", location.getId());

            Date encounterDateTime = DateUtils.parseDateByPattern(immunization.getDateAdministered(), "yyyyMMdd");
            encounterDetails.put("encounter.encounter_datetime", DateUtils.getFormattedDate(encounterDateTime));

            encounterDetails.put("encounter.form_uuid", StringUtils.defaultString(Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.FORM.FORM_UUID));
            encounterJSON.put("encounter", encounterDetails);

            patientDetails.put("patient.medical_record_number", StringUtils.defaultString(patient.getIdentifier()));
            patientDetails.put("patient.given_name", StringUtils.defaultString(patient.getGivenName()));
            patientDetails.put("patient.middle_name", StringUtils.defaultString(patient.getMiddleName()));
            patientDetails.put("patient.family_name", StringUtils.defaultString(patient.getFamilyName()));
            patientDetails.put("patient.sex", StringUtils.defaultString(patient.getGender()));
            patientDetails.put("patient.uuid", StringUtils.defaultString(patient.getUuid()));
            if (patient.getBirthdate() != null) {
                patientDetails.put("patient.birth_date", DateUtils.getFormattedDate(patient.getBirthdate()));
            }

            encounterJSON.put("patient", patientDetails);

            JSONObject vaccineJson = new JSONObject();

            String answer = null;
            int sequence = -1;
            String vaccine = immunization.getName();
            switch (vaccine) {
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.BCG.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.BCG.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.BCG.name + "^" + "99DCT";
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES6.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES6.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES6.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES6.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES9.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES9.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES9.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES9.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES18.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES18.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES18.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES18.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV1.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV1.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV1.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV1.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV2.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV2.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV2.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV2.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV3.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV3.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV3.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV3.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV_AT_BIRTH.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV_AT_BIRTH.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV_AT_BIRTH.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV_AT_BIRTH.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_1.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_1.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_1.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_1.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_2.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_2.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_2.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_2.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_3.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_3.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_3.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_3.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA1.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA1.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA1.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA1.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA2.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA2.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA2.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA2.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA3.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA3.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA3.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA3.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA1.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA1.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA1.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA1.sequence;
                    break;
                case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA2.name:
                    answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA2.concept_id + "^"
                            + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA2.name + "^" + "99DCT";
                    sequence = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA2.sequence;
            }
            if (sequence != -1) {
                String conceptQuestion = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.concept_id + "^"
                        + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.name + "^" + "99DCT";
                vaccineJson.put(conceptQuestion, answer);
            }
            if (!StringUtils.isEmpty(answer)) {
                String conceptQuestion = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.concept_id + "^"
                        + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.name + "^" + "99DCT";
                vaccineJson.put(conceptQuestion, answer);

                String groupConceptQuestion = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.GROUP.concept_id + "^"
                        + Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.GROUP.name + "^" + "99DCT";
                observationDetails.put(groupConceptQuestion, vaccineJson);
                encounterJSON.put("observation", observationDetails);
            }

            encounterJSON.put("patient", patientDetails);
            encounterJSON.put("encounter", encounterDetails);

            Log.e("KenyaEmrShrMapper", "IMMUNIZATION PAYLOAD: " + encounterJSON.toString());

            return encounterJSON.toString();
        } catch (Exception e){
            throw new ShrParseException(e);
        }
    }

    private static HIVTest getHivTestFromEncounter(EncounterWithObservations encounterWithObservations) throws ShrParseException {
        List<Observation> observations = encounterWithObservations.getObservations();
        HIVTest hivTest = new HIVTest();
        ProviderDetails providerDetails = null;
        boolean isHivEncounter = false;
        for(Observation observation:observations){
            Concept answerConcept;
            String shrAnswer = null;
            switch(observation.getConcept().getConceptid()){
                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.concept_id:
                    isHivEncounter = true;
                    answerConcept = observation.getValueCoded();
                    if(answerConcept!= null) {
                        switch (answerConcept.getConceptid()) {
                            case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.POSITIVE.concept_id:
                                shrAnswer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.POSITIVE.name;
                                break;
                            case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.NEGATIVE.concept_id:
                                shrAnswer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.NEGATIVE.name;
                                break;
                            case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.INCONCLUSIVE.concept_id:
                                shrAnswer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.INCONCLUSIVE.name;
                                break;
                        }
                        if(!StringUtils.isEmpty(shrAnswer)){
                            hivTest.setResult(shrAnswer);
                        }
                    }
                    break;

                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.concept_id:
                    isHivEncounter = true;
                    answerConcept = observation.getValueCoded();
                    if(answerConcept!= null) {
                        switch (answerConcept.getConceptid()) {
                            case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.concept_id:
                                shrAnswer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.name;
                                break;
                            case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.concept_id:
                                shrAnswer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.name;
                                break;
                        }
                        if(!StringUtils.isEmpty(shrAnswer)){
                            hivTest.setType(shrAnswer);
                        }
                    }
                    break;

                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.concept_id:
                    isHivEncounter = true;
                    answerConcept = observation.getValueCoded();
                    if(answerConcept!= null) {
                        switch (answerConcept.getConceptid()) {
                            case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.NP.concept_id:
                                shrAnswer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.NP.name;
                                break;
                            case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HB.concept_id:
                                shrAnswer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HB.name;
                                break;
                            case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HP.concept_id:
                                shrAnswer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HP.name;
                                break;
                            case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.MO.concept_id:
                                shrAnswer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.MO.name;
                                break;
                            case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VI.concept_id:
                                shrAnswer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VI.name;
                                break;
                            case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VS.concept_id:
                                shrAnswer = Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VS.name;
                                break;
                        }
                        if(!StringUtils.isEmpty(shrAnswer)){
                            hivTest.setStrategy(shrAnswer);
                        }
                    }
                    break;

                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_FACILITY.concept_id:
                    hivTest.setFacility(observation.getValueAsString());
                    break;

                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.ID.concept_id:
                    if(providerDetails == null){
                        providerDetails = new ProviderDetails();
                    }
                    providerDetails.setId(observation.getValueAsString());
                    break;

                case Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.NAME.concept_id:
                    if(providerDetails == null){
                        providerDetails = new ProviderDetails();
                    }
                    providerDetails.setName(observation.getValueAsString());
                    break;
            }
        }
        if(!isHivEncounter){
            return null;
        }

        if(StringUtils.isEmpty(hivTest.getFacility())){
            Location location = encounterWithObservations.getEncounter().getLocation();
            String facility = LocationUtils.getKenyaEmrMasterFacilityListCode(location);
            if(!StringUtils.isEmpty(facility)) {
                hivTest.setFacility(facility);
            } else {
                hivTest.setFacility(Constants.Shr.KenyaEmr.DEFAULT_SHR_FACILITY.MFL_CODE);
                //throw new ShrParseException("Cannot get Facility MFL code from encounter location");
            }
        }

        if(providerDetails == null){
            providerDetails = new ProviderDetails();
            Person provider = encounterWithObservations.getEncounter().getProvider();
            if(provider != null){
                providerDetails.setId(provider.getMiddleName());
                providerDetails.setName(provider.getFamilyName() + " " + provider.getGivenName());
            }
        }

        if(providerDetails != null){
            hivTest.setProviderDetails(providerDetails);
        }

        Date encounterDate = encounterWithObservations.getEncounter().getEncounterDatetime();
        if(encounterDate != null) {
            String date = DateUtils.getFormattedDate(encounterDate, "yyyyMMdd");
            hivTest.setDate(date);
        } else {
            throw new ShrParseException("Cannot get encounter date from encounter");
        }

        String date = DateUtils.getFormattedDate(encounterWithObservations.getEncounter().getEncounterDatetime(),"yyyyMMdd");
        hivTest.setDate(date);

        return hivTest;
    }

    private static Immunization getImmunizationFromEncounter(EncounterWithObservations encounterWithObservations){
        List<Observation> observations = encounterWithObservations.getObservations();
        Immunization immunization = new Immunization();
        boolean isImmunizationEncounter = false;
        for(Observation observation:observations){
            String answer = null;
            Concept concept = observation.getConcept();
            Concept valueCoded = observation.getValueCoded();
            if(concept.getConceptid() == Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.concept_id && valueCoded!= null) {
                isImmunizationEncounter = true;
                switch (valueCoded.getConceptid()) {
                    case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.BCG.concept_id:
                        answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.BCG.name;
                        break;
                    case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.concept_id:
                        answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.name;
                        break;
                    case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES6.concept_id:
                        answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES6.name;
                        break;
                    case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES9.concept_id:
                        answer = valueCoded.getName();
                        break;
                    case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV2.concept_id:
                        answer = Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.name;
                        break;
                    case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_1.concept_id:
                        answer = valueCoded.getName();
                        break;
                    case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA1.concept_id:
                        answer = valueCoded.getName();
                        break;
                    case Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA1.concept_id:
                        answer = valueCoded.getName();
                }
                if(!StringUtils.isEmpty(answer)){
                    immunization.setName(answer);
                    Date obsDateTime = observation.getObservationDatetime();
                    String dateAdministered = DateUtils.getFormattedDate(obsDateTime, "yyyyMMdd");
                    immunization.setDateAdministered(dateAdministered);
                    break;
                }
            }
        }
        if(!isImmunizationEncounter){
            return null;
        }
        return immunization;
    }


    public static class ShrParseException extends Throwable {
        ShrParseException(Throwable throwable) {
            super(throwable);
        }
        ShrParseException(String message) {
            super(message);
        }
        ShrParseException(String message, Throwable e) {
            super(message,e);
        }
    }
}

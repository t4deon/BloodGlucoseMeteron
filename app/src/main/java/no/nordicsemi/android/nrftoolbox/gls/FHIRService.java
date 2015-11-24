package no.nordicsemi.android.nrftoolbox.gls;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Device;
import ca.uhn.fhir.model.dstu2.resource.DeviceMetric;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.DeviceMetricCategoryEnum;
import ca.uhn.fhir.model.dstu2.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.ServerValidationModeEnum;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class FHIRService extends AsyncTask<SparseArray<GlucoseRecord>, Void, Throwable> {

    private final static String TAG = FHIRService.class.getName();
    public final static String SERVER_URL = "https://fhir.iap.hs-heilbronn.de/baseDstu2";

    private final static String SYSTEM_URL = "http://mi.hs-heilbronn.de/fhir/pmk/android";

    FhirContext ctx;
    IGenericClient client;

    private Context context;
    private DeviceInformation deviceInfo;
    private no.nordicsemi.android.nrftoolbox.gls.Patient patient;

    @Override
    protected Throwable doInBackground(SparseArray<GlucoseRecord>... params) {
        return this.fhir(params[0]);
    }


    public FHIRService(String url, Context context, DeviceInformation deviceInfo, no.nordicsemi.android.nrftoolbox.gls.Patient p) {
        this.context = context;
        this.deviceInfo = deviceInfo;
        this.patient = p;

        // initialize context and client
        ctx = FhirContext.forDstu2();
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        client = ctx.newRestfulGenericClient(url);

    }

    public Throwable fhir(SparseArray<GlucoseRecord> records) {

        try {
            Patient p = getPatient();
            Log.d(TAG, "converting glucoserecrods to observations now");

            Device d = initializeDevice(p);
            // do conditional update here so the device is updated if needed

            String ident = getConditionalUrl(d, d.getIdentifierFirstRep());
            String a = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(d);
            IdDt id = (IdDt) client.update().resource(d).conditionalByUrl(ident)
                    .execute().getId();
            d.setId(id);

            DeviceMetric metric = initializeDeviceMetric(d);
            metric.setId(client.create().resource(metric).conditionalByUrl(getConditionalUrl(metric, metric.getIdentifier())).execute().getId());

            for (int i = 0; i < records.size(); i++) {
                int j = records.keyAt(i);
                GlucoseRecord r = records.get(j);

                Log.d(TAG, "creating quantity");

                QuantityDt qdt = initializeQuantityDt(r);

                Observation o = initializeObservation(p, metric, r, qdt);

                Log.d(TAG, "submitting to server...");
                //client.update().resource(o).conditionalByUrl("Observation?identifier=http://...%7Cid").encodedJson().execute();
                IdDt did = (IdDt) client.update().resource(o).conditionalByUrl(getConditionalUrl(o, o.getIdentifierFirstRep())).execute().getId();

                o.setId(did);

                String s = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(o);


                Log.d(TAG, "... done");
            }
        } catch (Throwable e) {
            return e;
        }

        return null;
    }

    private DeviceMetric initializeDeviceMetric(Device d) {
        log("Initializing device metric");

        DeviceMetric metric = new DeviceMetric();
        CodeableConceptDt metricType = new CodeableConceptDt();
        // TODO is this the right code and display?
        metricType.addCoding().setCode("160020").setSystem("https://rtmms.nist.gov").setDisplay("MDC_CONC_GLU_GEN");
        metric.setType(metricType);
        metric.setSource(newResourceReference(d.getId()));
        metric.setText(getNarrative(metric));
        metric.setCategory(DeviceMetricCategoryEnum.MEASUREMENT);
        CodeableConceptDt unitConcept = new CodeableConceptDt();
        unitConcept.addCoding().setDisplay("MDC_DIM_MILLI_MOLE_PER_L").setSystem("https://rtmms.nist.gov").setCode("266866");
        metric.setUnit(unitConcept);
        // TODO what is the identifier here??? especially the value
        metric.setIdentifier(new IdentifierDt().setSystem(SYSTEM_URL + "/metric/").setValue("glucosemillimolperliter"));

        log("Initializing device metric done.");
        return metric;
    }

    private Observation initializeObservation(Patient p, DeviceMetric metric, GlucoseRecord r, QuantityDt qdt) {
        log("Initializing observation");

        Observation o = newObservation(qdt);
        o.setCode(createCode(r.unit));
        o.setSubject(newResourceReference(p.getId()));
        o.setDevice(newResourceReference(metric.getId()));

        // set date and time for observation
        DateTimeDt dt = new DateTimeDt();
        dt.setValue(r.time.getTime());
        o.setEffective(dt);
        o.setIdentifier(Arrays.asList(getIdentifier("/devices/".concat("" + deviceInfo.getSysID()), "" + r.getSequenceNumber())));

        // set narrative
        o.setText(getNarrative(o));

        log("Initializing observation done");
        return o;
    }

    private QuantityDt initializeQuantityDt(GlucoseRecord r) {
        log("Initializing quantity");
        QuantityDt qdt = new QuantityDt();
        // TODO mmol/l hier oder mol/l ??
        qdt.setUnit(new StringDt("mol/L"));
        qdt.setSystem("http://unitsofmeasure.org");
        qdt.setCode("mol/l");
        qdt.setValue(r.glucoseConcentration);

        log("Initializing quantity done");
        return qdt;
    }

    private Device initializeDevice(Patient p) {
        log("Initializing device");

        Device d = newDevice();
        d.setManufacturer(deviceInfo.getManufacturerName());
        d.setModel(deviceInfo.getModelNumber());
        d.setVersion(deviceInfo.getFirmwareRevision());
        NarrativeDt n = getNarrative(d);
        n.setDiv(n.getDiv().getValueAsString().substring(5, n.getDiv().getValueAsString().length() - 6).concat("<strong>Serial Number: </strong>").concat(deviceInfo.getSerialNumber())
                .concat("<br /><strong>Name: </strong>").concat(deviceInfo.getName())
                .concat("<br /><strong>MAC Address: </strong>").concat(deviceInfo.getAddress()));
        d.setText(n);
        CodeableConceptDt type = new CodeableConceptDt();
        // TODO find valid url
        type.addCoding().setCode("15-102").setDisplay("Glukose-Analysegerät").setSystem("http://umdns.org");
        d.setType(type);

        d.setPatient(newResourceReference(p.getId()));
        d.setIdentifier(Arrays.asList(getIdentifier("/devices/", "" + deviceInfo.getSysID())));

        log("Initializing device done");

        return d;
    }

    private IdentifierDt getIdentifier(String relativeUrl, String value) {
        log("Initializing identifier");
        log("Initializing identifier done");
        return new IdentifierDt().setValue(value).setSystem(SYSTEM_URL + relativeUrl);
    }

    private NarrativeDt getNarrative(final IResource resource) {
        log("get narrative");
        String url = "http://fhir2.healthintersections.com.au/open/$generate-narrative";
        final NarrativeDt result = new NarrativeDt();

        final String resAsJson = this.ctx.newJsonParser().encodeResourceToString(resource);

        AsyncHttpClient httpClient = new SyncHttpClient();

        StringEntity body = null;
        try {
            body = new StringEntity(resAsJson);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        httpClient.post(this.context, url, body, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                // whole resource in json is returned here. just extract the narrative
                String json = new String(responseBody);
                try {
                    JSONObject obj = new JSONObject(json);
                    String html = obj.getJSONObject("text").getString("div");
                    result.setDiv(html);
                } catch (JSONException e) {
                    e.printStackTrace();
                    // TODO handle exception
                }

                result.setStatus(NarrativeStatusEnum.GENERATED);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e(TAG, "Get narrativ exit with http code: " + statusCode);
//                if (resource instanceof Observation) {
//                    Observation o = (Observation) resource;
//                    result.setDiv("GObservation" + o.getValue().toString());
//                }
//                if (resource instanceof Patient) {
//                    // TODO
//                }
//                if (resource instanceof Device) {
//// TODO
//                }
//                if (resource instanceof DeviceMetric) {
//                    // TODO
//                }
                result.setDiv("Grahame's server is down");
            }
        });
        log("get narrative done");

        return result;
    }

    private CodeableConceptDt createCode(int unit) {
        log("get code done");
        CodeableConceptDt codeableConceptDt = new CodeableConceptDt();
        CodingDt coding = new CodingDt();

        String code = "", display = "";
        switch (unit) {
            case GlucoseRecord.UNIT_kgpl:
                code = "2339-0";
                display = "Glucose [Mass/volume] in Blood";
                break;
            case GlucoseRecord.UNIT_molpl:
                code = "15074-8";
                display = "Glucose [Moles/volume] in Blood";
                break;
        }

        codeableConceptDt.addCoding().setSystem("http://loinc.org").setCode(code).setDisplay(display);
        log("get code done");
        return codeableConceptDt;
    }

    /**
     * gets Patient from the server if existing. else creates a new one.
     *
     * @return
     */
    private Patient getPatient() {
        Log.d(TAG, "looking for patient");
        Patient p = this.newPatient(patient.getLastname(), patient.getFirstName(), patient.getDateOfBirth());
        p.setText(getNarrative(p));
        p.setIdentifier(Arrays.asList(
                new IdentifierDt().setSystem(SYSTEM_URL.concat("/patients/"))
                .setValue(patient.getLastname().concat(patient.getFirstName())
                        .concat(SimpleDateFormat.getDateInstance(DateFormat.SHORT).format(patient.getDateOfBirth())))));

        IdDt patientId = (IdDt) client.create().resource(p).conditionalByUrl(
                getConditionalUrl(p, p.getIdentifierFirstRep())
        ).execute().getId();
        p.setId(patientId);

        return p;
    }

    private Observation newObservation(QuantityDt value) {
        Log.d(TAG, "creating new observation");
        Observation o = new Observation();
        o.setValue(value);
        o.getResourceMetadata().put(ResourceMetadataKeyEnum.PROFILES,
                "http://hl7.org/fhir/StructureDefinition/devicemetricobservation");
        o.setStatus(ObservationStatusEnum.FINAL);
        return o;
    }

    private Device newDevice() {
        Log.d(TAG, "creating new device");
        Device d = new Device();

        return d;
    }

    private Patient newPatient(String familyName, String givenName, Date date) {
        Log.d(TAG, "creating new patient");
        Patient p = new Patient();
        p.addName().addFamily(familyName).addGiven(givenName);
        p.setBirthDate(new DateDt(date));

        return p;
    }

    private ResourceReferenceDt newResourceReference(IdDt id) {
        Log.d(TAG, "creating new reference");
        ResourceReferenceDt ref = new ResourceReferenceDt();
        ref.setReference(id);

        return ref;
    }

    private String getConditionalUrl(IResource resource, IdentifierDt identifier) {
        return resource.getResourceName() + "?identifier=".concat(identifier.getSystem()).concat("%7C").concat(identifier.getValue());
    }

    private void log(String message) {
        Log.d(TAG, message);
    }
}

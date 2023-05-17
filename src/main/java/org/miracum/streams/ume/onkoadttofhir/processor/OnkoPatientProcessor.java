package org.miracum.streams.ume.onkoadttofhir.processor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.hl7.fhir.r4.model.*;
import org.miracum.streams.ume.onkoadttofhir.FhirProperties;
import org.miracum.streams.ume.onkoadttofhir.model.MeldungExport;
import org.miracum.streams.ume.onkoadttofhir.model.MeldungExportList;
import org.miracum.streams.ume.onkoadttofhir.serde.MeldungExportListSerde;
import org.miracum.streams.ume.onkoadttofhir.serde.MeldungExportSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class OnkoPatientProcessor extends OnkoProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(OnkoPatientProcessor.class);

  @Value("${app.version}")
  private String appVersion;

  @Value("#{new Boolean('${app.enableCheckDigitConversion}')}")
  private boolean checkDigitConversion;

  protected OnkoPatientProcessor(FhirProperties fhirProperties) {
    super(fhirProperties);
  }

  @Bean
  @Profile("patient")
  public Function<KTable<String, MeldungExport>, KStream<String, Bundle>>
      getMeldungExportPatientProcessor() {
    return stringOnkoMeldungExpTable ->
        stringOnkoMeldungExpTable
            .filter(
                (key, value) ->
                    value
                            .getXml_daten()
                            .getMenge_Patient()
                            .getPatient()
                            .getMenge_Meldung()
                            .getMeldung()
                            .getMenge_Tumorkonferenz()
                        == null) // ignore tumor conferences
            .groupBy(
                (key, value) ->
                    KeyValue.pair(
                        "Struct{REFERENZ_NUMMER="
                            + value.getReferenz_nummer()
                            + ",TUMOR_ID="
                            + getTumorIdFromAdt(value)
                            + "}",
                        value),
                Grouped.with(Serdes.String(), new MeldungExportSerde()))
            .aggregate(
                MeldungExportList::new,
                (key, value, aggregate) -> aggregate.addElement(value),
                (key, value, aggregate) -> aggregate.removeElement(value),
                Materialized.with(Serdes.String(), new MeldungExportListSerde()))
            .mapValues(this.getOnkoToOPatientBundleMapper())
            .filter((key, value) -> value != null)
            .toStream();
  }

  public ValueMapper<MeldungExportList, Bundle> getOnkoToOPatientBundleMapper() {
    return meldungExporte -> {
      List<MeldungExport> meldungExportList =
          prioritiseLatestMeldungExports(
              meldungExporte,
              Arrays.asList("behandlungsende", "statusaenderung", "diagnose", "tod"));

      return extractOnkoResourcesFromReportingReason(meldungExportList);
    };
  }

  public Bundle extractOnkoResourcesFromReportingReason(List<MeldungExport> meldungExportList) {

    if (meldungExportList.isEmpty()) {
      return null;
    }

    var meldungExport = meldungExportList.get(0);

    LOG.debug("Mapping Meldung {} to {}", getReportingIdFromAdt(meldungExport), "patient");

    var patient = new Patient();

    // id
    var patId = meldungExport.getReferenz_nummer();
    var pid = patId;
    if (checkDigitConversion) {
      pid = convertId(patId);
    }
    var id = this.getHash("Patient", pid);
    patient.setId(id);

    // meta.source
    var senderInfo = meldungExport.getXml_daten().getAbsender();
    patient
        .getMeta()
        .setSource(
            generateProfileMetaSource(
                senderInfo.getAbsender_ID(), senderInfo.getSoftware_ID(), appVersion));

    // meta.profile
    patient
        .getMeta()
        .setProfile(
            Collections.singletonList(
                new CanonicalType(fhirProperties.getProfiles().getMiiPatientPseudonymisiert())));

    // MII identifier
    var pseudonym = new Identifier();
    pseudonym
        .getType()
        .addCoding(new Coding(fhirProperties.getSystems().getObservationValue(), "PSEUDED", null))
        .addCoding(
            new Coding(
                fhirProperties.getSystems().getIdentifierType(), "MR", "Medical·record·number"));
    pseudonym.setSystem(fhirProperties.getSystems().getPatientId()).setValue(pid);
    patient.addIdentifier(pseudonym);

    var patData =
        meldungExport.getXml_daten().getMenge_Patient().getPatient().getPatienten_Stammdaten();

    // gender
    var genderMap =
        new HashMap<String, Enumerations.AdministrativeGender>() {
          {
            put("W", Enumerations.AdministrativeGender.FEMALE);
            put("M", Enumerations.AdministrativeGender.MALE);
            put("D", Enumerations.AdministrativeGender.OTHER); // TODO set genderExtension
            put("U", Enumerations.AdministrativeGender.UNKNOWN);
          }
        };

    patient.setGender(genderMap.getOrDefault(patData.getPatienten_Geschlecht(), null));

    if (patData.getPatienten_Geburtsdatum() != null) {
      patient.setBirthDateElement(
          new DateType(getBirthDateYearMonthString(patData.getPatienten_Geburtsdatum())));
    }

    var reportingReason =
        meldungExport
            .getXml_daten()
            .getMenge_Patient()
            .getPatient()
            .getMenge_Meldung()
            .getMeldung()
            .getMeldeanlass();

    // deceased
    if (Objects.equals(reportingReason, "tod")) {
      var mengeVerlauf =
          meldungExport
              .getXml_daten()
              .getMenge_Patient()
              .getPatient()
              .getMenge_Meldung()
              .getMeldung()
              .getMenge_Verlauf();

      if (mengeVerlauf != null && mengeVerlauf.getVerlauf() != null) {

        var death = mengeVerlauf.getVerlauf().getTod();

        if (death.getSterbedatum() != null) {
          patient.setDeceased(extractDateTimeFromADTDate(death.getSterbedatum()));
        }
      }
    }

    // address
    var address = new Address();
    var patAddess = patData.getMenge_Adresse().getAdresse().get(0);
    if (patAddess.getPatienten_PLZ() != null && patAddess.getPatienten_PLZ().length() >= 2) {
      address
          .setPostalCode(patAddess.getPatienten_PLZ().substring(0, 2))
          .setType(Address.AddressType.BOTH);
      if (patAddess.getPatienten_Land() != null
          && patAddess.getPatienten_Land().matches("[a-zA-Z]{2,3}")) {
        address.setCountry(patAddess.getPatienten_Land().toUpperCase());
      } else {
        address
            .addExtension()
            .setUrl(fhirProperties.getExtensions().getDataAbsentReason())
            .setValue(new CodeType("unknown"));
      }
    }
    patient.addAddress(address);

    var bundle = new Bundle();
    bundle.setType(Bundle.BundleType.TRANSACTION);
    bundle = addResourceAsEntryInBundle(bundle, patient);

    return bundle;
  }

  private static String getBirthDateYearMonthString(String gebdatum) {

    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy").withLocale(Locale.GERMANY);
    LocalDate localBirthDate = LocalDate.parse(gebdatum, formatter);

    var quarterMonth = ((localBirthDate.getMonthValue() - 1) / 3 + 1) * 3 - 2;

    return YearMonth.of(localBirthDate.getYear(), quarterMonth).toString();
  }
}
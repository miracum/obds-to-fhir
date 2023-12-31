package org.miracum.streams.ume.obdstofhir.processor;

import java.util.*;
import java.util.function.BiFunction;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.hl7.fhir.r4.model.Bundle;
import org.miracum.streams.ume.obdstofhir.FhirProperties;
import org.miracum.streams.ume.obdstofhir.mapper.*;
import org.miracum.streams.ume.obdstofhir.model.MeldungExport;
import org.miracum.streams.ume.obdstofhir.model.MeldungExportList;
import org.miracum.streams.ume.obdstofhir.serde.MeldungExportListSerde;
import org.miracum.streams.ume.obdstofhir.serde.MeldungExportSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObdsProcessor extends ObdsToFhirMapper {

  private static final Logger LOG = LoggerFactory.getLogger(ObdsProcessor.class);

  @Value("${spring.profiles.active}")
  private String profile;

  private final ObdsMedicationStatementMapper onkoMedicationStatementMapper;
  private final ObdsObservationMapper onkoObservationMapper;
  private final ObdsProcedureMapper onkoProcedureMapper;
  private final ObdsPatientMapper onkoPatientMapper;
  private final ObdsConditionMapper onkoConditionMapper;

  @Autowired
  protected ObdsProcessor(
      FhirProperties fhirProperties,
      ObdsMedicationStatementMapper onkoMedicationStatementMapper,
      ObdsObservationMapper onkoObservationMapper,
      ObdsProcedureMapper onkoProcedureMapper,
      ObdsPatientMapper onkoPatientMapper,
      ObdsConditionMapper onkoConditionMapper) {
    super(fhirProperties);
    this.onkoMedicationStatementMapper = onkoMedicationStatementMapper;
    this.onkoObservationMapper = onkoObservationMapper;
    this.onkoProcedureMapper = onkoProcedureMapper;
    this.onkoPatientMapper = onkoPatientMapper;
    this.onkoConditionMapper = onkoConditionMapper;
  }

  @Bean
  public BiFunction<
          KTable<String, MeldungExport>, KTable<String, Bundle>, KStream<String, Bundle>[]>
      getMeldungExportObdsProcessor() {

    return (stringOnkoMeldungExpTable, stringOnkoObsBundles) -> {
      // return (stringOnkoMeldungExpTable) ->
      var output =
          stringOnkoMeldungExpTable
              // only process adt v2.x.x
              .filter((key, value) -> value.getXml_daten().getSchema_Version().matches("^2\\..*"))
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
                              + getPatIdFromAdt(value)
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
              .toStream()
              .split(Named.as("out-"))
              .branch((k, v) -> v != null, Branched.as("Aggregate"))
              .noDefaultBranch();

      if (Objects.equals(profile, "patient")) {
        return new KStream[] {
          output
              .get("out-Aggregate")
              .mapValues(this.getOnkoToMedicationStatementBundleMapper())
              .filter((key, value) -> value != null),
          output
              .get("out-Aggregate")
              .mapValues(this.getOnkoToObservationBundleMapper())
              .filter((key, value) -> value != null),
          output
              .get("out-Aggregate")
              .mapValues(this.getOnkoToProcedureBundleMapper())
              .filter((key, value) -> value != null),
          output
              .get("out-Aggregate")
              .leftJoin(stringOnkoObsBundles, Pair::of)
              .mapValues(this.getOnkoToConditionBundleMapper())
              .filter((key, value) -> value != null),
          output
              .get("out-Aggregate")
              .mapValues(this.getOnkoToPatientBundleMapper())
              .filter((key, value) -> value != null)
        };
      } else {
        return new KStream[] {
          output
              .get("out-Aggregate")
              .mapValues(this.getOnkoToMedicationStatementBundleMapper())
              .filter((key, value) -> value != null),
          output
              .get("out-Aggregate")
              .mapValues(this.getOnkoToObservationBundleMapper())
              .filter((key, value) -> value != null),
          output
              .get("out-Aggregate")
              .mapValues(this.getOnkoToProcedureBundleMapper())
              .filter((key, value) -> value != null),
          output
              .get("out-Aggregate")
              .leftJoin(stringOnkoObsBundles, Pair::of)
              .mapValues(this.getOnkoToConditionBundleMapper())
              .filter((key, value) -> value != null)
        };
      }
    };
  }

  public ValueMapper<MeldungExportList, Bundle> getOnkoToMedicationStatementBundleMapper() {
    return meldungExporte -> {
      List<MeldungExport> meldungExportList =
          prioritiseLatestMeldungExports(
              meldungExporte,
              Arrays.asList(
                  fhirProperties.getReportingReason().getTreatmentEnd(),
                  fhirProperties.getReportingReason().getTreatmentStart()),
              List.of(
                  fhirProperties.getReportingReason().getTreatmentEnd(),
                  fhirProperties.getReportingReason().getTreatmentStart()));

      return onkoMedicationStatementMapper.mapOnkoResourcesToMedicationStatement(meldungExportList);
    };
  }

  public ValueMapper<MeldungExportList, Bundle> getOnkoToProcedureBundleMapper() {
    return meldungExporte -> {
      List<MeldungExport> meldungExportList =
          prioritiseLatestMeldungExports(
              meldungExporte,
              Arrays.asList(
                  fhirProperties.getReportingReason().getTreatmentEnd(),
                  fhirProperties.getReportingReason().getTreatmentStart()),
              List.of(
                  fhirProperties.getReportingReason().getTreatmentEnd(),
                  fhirProperties.getReportingReason().getTreatmentStart()));
      return onkoProcedureMapper.mapOnkoResourcesToProcedure(meldungExportList);
    };
  }

  public ValueMapper<MeldungExportList, Bundle> getOnkoToObservationBundleMapper() {
    return meldungExporte -> {
      List<MeldungExport> meldungExportList =
          prioritiseLatestMeldungExports(
              meldungExporte,
              Arrays.asList(
                  fhirProperties.getReportingReason().getTreatmentEnd(),
                  fhirProperties.getReportingReason().getStatusChange(),
                  fhirProperties.getReportingReason().getDiagnosis(),
                  fhirProperties.getReportingReason().getDeath()),
              null);

      return onkoObservationMapper.mapOnkoResourcesToObservation(meldungExportList);
    };
  }

  public ValueMapper<MeldungExportList, Bundle> getOnkoToPatientBundleMapper() {
    return meldungExporte -> {
      List<MeldungExport> meldungExportList =
          prioritiseLatestMeldungExports(
              meldungExporte,
              Arrays.asList(
                  fhirProperties.getReportingReason().getDeath(),
                  fhirProperties.getReportingReason().getTreatmentEnd(),
                  fhirProperties.getReportingReason().getStatusChange(),
                  fhirProperties.getReportingReason().getDiagnosis()),
              null);

      return onkoPatientMapper.mapOnkoResourcesToPatient(meldungExportList);
    };
  }

  public ValueMapper<Pair<MeldungExportList, Bundle>, Bundle> getOnkoToConditionBundleMapper() {
    return meldungPair -> {
      List<MeldungExport> meldungExportList =
          prioritiseLatestMeldungExports(
              meldungPair.getLeft(),
              Arrays.asList(
                  fhirProperties.getReportingReason().getDiagnosis(),
                  fhirProperties.getReportingReason().getTreatmentEnd(),
                  fhirProperties.getReportingReason().getStatusChange()),
              null);

      return onkoConditionMapper.mapOnkoResourcesToCondition(
          meldungExportList, meldungPair.getRight());
    };
  }
}

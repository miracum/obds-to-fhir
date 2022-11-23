package org.miracum.streams.ume.onkoadttofhir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import java.io.Serializable;
import java.util.List;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ADT_GEKID implements Serializable {

  @JsonProperty private String Schema_Version;

  @JsonProperty private Absender Absender;

  @JsonProperty private Menge_Patient Menge_Patient;

  @Data
  public static class Absender {
    @JsonProperty private String Absender_Bezeichnung;

    @JsonProperty private String Absender_Ansprechpartner;

    @JsonProperty private String Absender_Anschrift;
  }

  @Data
  public static class Menge_Patient {

    @JsonProperty private Patient Patient;

    @Data
    public static class Patient {

      @JsonProperty private Patienten_Stammdaten Patienten_Stammdaten;

      @JsonProperty private Menge_Meldung Menge_Meldung;

      @Data
      public static class Patienten_Stammdaten {

        @JsonProperty private String KrankenversichertenNr;
      }

      @Data
      public static class Menge_Meldung {

        @JsonProperty private Meldung Meldung;

        @Data
        public static class Meldung {
          @JsonProperty private String Meldung_ID;

          @JsonProperty private String Meldedatum;
          @JsonProperty private String Meldeanlass;

          @JsonProperty private Tumorzuordnung Tumorzuordnung;

          @JsonProperty private Diagnose Diagnose;
          @JsonProperty private Menge_OP Menge_OP;

          @JsonProperty private Menge_Verlauf Menge_Verlauf;

          @JsonProperty private Menge_Tumorkonferenz Menge_Tumorkonferenz;

          @Data
          public static class Tumorzuordnung {
            @JsonProperty private String Tumor_ID;
          }

          @Data
          public static class Diagnose {

            @JsonProperty private String Tumor_ID;
            @JsonProperty private String Primaertumor_ICD_Code;
            @JsonProperty private String Primaertumor_ICD_Version;
            // @JsonProperty private String ICD_O_Topographie_Code;  nicht sicher ob notwendig, aber
            // wenn's schon da ist, why not
            // @JsonProperty private String ICD_O_Topographie_Version;
            @JsonProperty private String Diagnosedatum;
            @JsonProperty private String Seitenlokalisation;

            @JsonProperty
            private Menge_Histologie
                Menge_Histologie;

            @JsonProperty private cTNM cTNM;
            @JsonProperty private pTNM pTNM;

            @JsonProperty private Menge_Weitere_Klassifikation Menge_Weitere_Klassifikation;

            @Data
            public static class Menge_Histologie {
              @JacksonXmlElementWrapper(useWrapping = false)
              private List<Histologie> Histologie;

              @Data
              @EqualsAndHashCode(callSuper = true)
              public static class Histologie extends ADT_GEKID.HistologieAbs {

                @JsonProperty private String Histologie_ID;
                @JsonProperty private String Tumor_Histologiedatum;
                @JsonProperty private String Morphologie_Code; // Morphologie_ICD_O_3_Code
                @JsonProperty private String Morphologie_ICD_O_Version; //
                @JsonProperty private String Grading;
                @JsonProperty private String Morphologie_Freitext;
              }
            }

            @Data
            @EqualsAndHashCode(callSuper = true)
            public static class cTNM extends ADT_GEKID.CTnmAbs {
              @JsonProperty private String TNM_ID;
              @JsonProperty private String TNM_Datum;
              @JsonProperty private String TNM_Version;
              @JsonProperty private String TNM_c_p_u_Praefix_T;
              // brauchen wir nur bei OP
              @JsonProperty private String TNM_T;
              @JsonProperty private String TNM_c_p_u_Praefix_N;
              @JsonProperty private String TNM_N;
              @JsonProperty private String TNM_c_p_u_Praefix_M;
              @JsonProperty private String TNM_M;
              @JsonProperty private String TNM_y_Symbol;
              @JsonProperty private String TNM_r_Symbol;
              @JsonProperty private String TNM_m_Symbol;
            }

            @Data
            @EqualsAndHashCode(callSuper = true)
            public static class pTNM extends ADT_GEKID.PTnmAbs {
              @JsonProperty private String TNM_ID;
              @JsonProperty private String TNM_Datum;
              @JsonProperty private String TNM_Version;
              @JsonProperty private String TNM_c_p_u_Praefix_T;
              @JsonProperty private String TNM_T;
              @JsonProperty private String TNM_c_p_u_Praefix_N;
              @JsonProperty private String TNM_N;
              @JsonProperty private String TNM_c_p_u_Praefix_M;
              @JsonProperty private String TNM_M;
              @JsonProperty private String TNM_y_Symbol;
              @JsonProperty private String TNM_r_Symbol;
              @JsonProperty private String TNM_m_Symbol;
            }

            @Data
            public static class Menge_Weitere_Klassifikation {

              @JsonProperty private Weitere_Klassifikation Weitere_Klassifikation;

              @Data
              public static class Weitere_Klassifikation {
                @JsonProperty private String Name;
                @JsonProperty private String Datum;
                @JsonProperty private String Stadium;
              }
            }
          } // Diagnose Ende

          @Data
          public static class Menge_OP {
            @JsonProperty private OP OP;

            @Data
            public static class OP {
              @JsonProperty private Histologie Histologie;

              @JsonProperty private TNM TNM;

              @Data
              @EqualsAndHashCode(callSuper = true)
              public static class Histologie extends ADT_GEKID.HistologieAbs {
                @JsonProperty private String Histologie_ID;
                @JsonProperty private String Tumor_Histologiedatum;
                @JsonProperty private String Morphologie_Code; // Morphologie_ICD_O_3_Code
                @JsonProperty private String Morphologie_ICD_O_Version; //
                @JsonProperty private String Grading;
                @JsonProperty private String Morphologie_Freitext;
              }

              @Data
              @EqualsAndHashCode(callSuper = true)
              public static class TNM extends ADT_GEKID.PTnmAbs {
                @JsonProperty private String TNM_ID;
                @JsonProperty private String TNM_Datum;
                @JsonProperty private String TNM_Version;

                @JsonProperty private String TNM_L;
                @JsonProperty private String TNM_V;
                @JsonProperty private String TNM_Pn;

                @JsonProperty private String TNM_c_p_u_Praefix_T;
                @JsonProperty private String TNM_T;
                @JsonProperty private String TNM_c_p_u_Praefix_N;
                @JsonProperty private String TNM_N;
                @JsonProperty private String TNM_c_p_u_Praefix_M;
                @JsonProperty private String TNM_M;
                @JsonProperty private String TNM_y_Symbol;
                @JsonProperty private String TNM_r_Symbol;
                @JsonProperty private String TNM_m_Symbol;
              }
            }
          }

          @Data
          public static class Menge_Verlauf {

            @JsonProperty private Verlauf Verlauf;

            @Data
            public static class Verlauf {

              @JsonProperty private String Verlauf_ID;

              @JsonProperty private Histologie Histologie;

              @JsonProperty private TNM TNM;

              @Data
              @EqualsAndHashCode(callSuper = true)
              public static class Histologie extends ADT_GEKID.HistologieAbs {
                @JsonProperty private String Histologie_ID;
                @JsonProperty private String Tumor_Histologiedatum;
                @JsonProperty private String Morphologie_Code; // Morphologie_ICD_O_3_Code
                @JsonProperty private String Morphologie_ICD_O_Version; //
                @JsonProperty private String Grading;
                @JsonProperty private String Morphologie_Freitext;
              }

              @Data
              @EqualsAndHashCode(callSuper = true)
              public static class TNM extends ADT_GEKID.PTnmAbs {
                @JsonProperty private String TNM_ID;
                @JsonProperty private String TNM_Datum;
                @JsonProperty private String TNM_Version;

                @JsonProperty private String TNM_L;
                @JsonProperty private String TNM_V;
                @JsonProperty private String TNM_Pn;

                @JsonProperty private String TNM_c_p_u_Praefix_T;
                @JsonProperty private String TNM_T;
                @JsonProperty private String TNM_c_p_u_Praefix_N;
                @JsonProperty private String TNM_N;
                @JsonProperty private String TNM_c_p_u_Praefix_M;
                @JsonProperty private String TNM_M;
                @JsonProperty private String TNM_y_Symbol;
                @JsonProperty private String TNM_r_Symbol;
                @JsonProperty private String TNM_m_Symbol;
              }
            }
          }

          @Data
          public static class Menge_Tumorkonferenz {}
        }
      }
    }
  }

  @Data
  public abstract static class HistologieAbs {
    public String Histologie_ID;
    public String Tumor_Histologiedatum;
    public String Morphologie_Code; // Morphologie_ICD_O_3_Code
    public String Morphologie_ICD_O_Version; //
    public String Grading;
    public String Morphologie_Freitext;
  }

  @Data
  public abstract static class CTnmAbs {
    public String TNM_ID;
    public String TNM_Datum;
    public String TNM_Version;
    public String TNM_c_p_u_Praefix_T;
    public String TNM_T;
    public String TNM_c_p_u_Praefix_N;
    public String TNM_N;
    public String TNM_c_p_u_Praefix_M;
    public String TNM_M;
    public String TNM_y_Symbol;
    public String TNM_r_Symbol;
    public String TNM_m_Symbol;
  }

  @Data
  public abstract static class PTnmAbs {
    public String TNM_ID;
    public String TNM_Datum;
    public String TNM_Version;
    public String TNM_c_p_u_Praefix_T;
    public String TNM_T;
    public String TNM_c_p_u_Praefix_N;
    public String TNM_N;
    public String TNM_c_p_u_Praefix_M;
    public String TNM_M;
    public String TNM_y_Symbol;
    public String TNM_r_Symbol;
    public String TNM_m_Symbol;
  }
}

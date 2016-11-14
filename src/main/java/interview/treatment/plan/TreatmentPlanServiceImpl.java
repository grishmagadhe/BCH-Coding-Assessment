package interview.treatment.plan;


import interview.treatment.plan.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Your task is to implement the below service to solve the following problem:
 * given a Patient, what is the appropriate TreatmentPlan?
 * <p>
 * A Patient has a name, date of birth, weight, list of symptoms, list of medication allergies,
 * and MRN (Medical Record Number). We have also provided a list of Diseases, Medications, and Clinics
 * for use in this problem in our test suite.
 * <p>
 * A Disease has a name, list of symptoms (which suggest a patient has the disease if a patient has the
 * symptoms in the list), and a list of possible treatments for the disease. Each possible treatment for
 * a disease is a combination of medications with dosage amounts given in mg/kg.
 * <p>
 * A Medication has a name and a cost per mg.
 * <p>
 * A Clinic has a name, a range of ages (in months) that the clinic is open to, and a list of diseases
 * the clinic specializes in treating.
 * <p>
 * Using this information and the provided classes and interface, implement the TreatmentPlanServiceImpl
 * class. Each method in the interface includes exact specifications for what it should return. You can validate
 * that you are returning the correct information using the provided JUnit Test Suite. We will test your answers
 * against additional tests upon your submission of your code.
 * <p>
 * The "Init" method will be called before each test to set up the lists of Disease, Medications, and Clinics. We
 * may test your solution against different lists of Diseases, Medications, and Clinics.
 */
public class TreatmentPlanServiceImpl implements TreatmentPlanService {


    private static final LocalDate SEP_1_2016 = LocalDate.of(2016, 9, 1);

    // Do not modify the lists below.
    private List<Disease> diseases = new ArrayList<>();
    private List<Medication> medications = new ArrayList<>();
    private List<Clinic> clinics = new ArrayList<>();

    // TODO Optionally Implement any additional data structures here....

    // TODO .... to here.

    @Override
    public void init(List<Disease> diseases, List<Clinic> clinics, List<Medication> medications) {

        this.diseases = diseases;
        this.clinics = clinics;
        this.medications = medications;

        // TODO Optionally implement any additional init items below here ....

        // TODO ... to here.
    }

    @Override
    public Integer ageInYears(Patient patient) {
    	LocalDate currentDate=LocalDate.of(2016, Month.SEPTEMBER, 1);
    	return Period.between(patient.getDateOfBirth(), currentDate).getYears();
    }

    @Override
    public Integer ageInMonths(Patient patient) {
        Period period = Period.between(patient.getDateOfBirth(), SEP_1_2016);
        return period.getYears() * 12 + period.getMonths();
    }

    @Override
    public List<Clinic> clinicsBasedOnAgeAndDiseases(Patient patient) {
        List<Clinic> clinicsForPatient = new ArrayList<>(1);
        int patientAgeInMonths = this.ageInMonths(patient);
        List<String> patientSymptoms = patient.getSymptoms();
        for (Clinic clinic : clinics) {
            Integer clinicMinAge = clinic.getMinAgeInMonths();
            Integer clinicMaxAge = clinic.getMaxAgeInMonths();
            if (patientAgeInMonths >= clinicMinAge && (clinicMaxAge == null || patientAgeInMonths <= clinicMaxAge)) {
                for (String disease : clinic.getDiseases()) {
                    List<String> diseaseSymptoms = getDiseaseSymptoms(disease);
                    List<String> matchedSymptoms = patientSymptoms.stream().filter(diseaseSymptoms::contains).collect(Collectors.toList());
                    if (matchedSymptoms.size() * 1.0 / diseaseSymptoms.size() >= .7) {
                        clinicsForPatient.add(clinic);
                        break;
                    }
                }
            }
        }
        return clinicsForPatient;
    }

    @Override
    public Map<Disease, BigDecimal> diseaseLikelihoods(Patient patient) {
        Map<Disease, BigDecimal> diseaseLikelihoods = new HashMap<>(1);
        List<String> patientSymptoms = patient.getSymptoms();
        for (Disease disease : diseases) {
            List<String> diseaseSymptoms = disease.getSymptoms();
            
            //matches patient and each disease symptoms and returns matched in a new list
            List<String> matchedSymptoms = patientSymptoms.stream().filter(diseaseSymptoms::contains).collect(Collectors.toList());
            BigDecimal diseaseLikelihood = BigDecimal.valueOf(matchedSymptoms.size()).divide(BigDecimal.valueOf(diseaseSymptoms.size()), 2, RoundingMode.HALF_UP);
            diseaseLikelihoods.put(disease, diseaseLikelihood);
        }
        return diseaseLikelihoods;
    }

    @Override
    public Map<Medication, BigDecimal> medicationsForDisease(Patient patient, Disease disease) {

        Map<Medication, BigDecimal> bestPriceMedication = new HashMap<>(1);
        Map<Medication, BigDecimal> medicationOption;
        List<Map<String, BigDecimal>> medicationCombinations = disease.getMedicationCombinations();
        BigDecimal totalMedicationPrice;
        BigDecimal patientWeight = patient.getWeight();
        BigDecimal leastPrice = null;

        Map<String, BigDecimal> medicationPriceMap = medications.stream().collect(Collectors.toMap(Medication::getName, Medication::getCostPerMg));

        for (Map<String, BigDecimal> medicationCombination : medicationCombinations) {
            Set<String> prescribedMedications = medicationCombination.keySet();
            if (Collections.disjoint(prescribedMedications, patient.medicationAllergies())) {
                totalMedicationPrice = new BigDecimal(0);
                medicationOption = new HashMap<>(prescribedMedications.size());
                for (String prescribedMedication : prescribedMedications) {
                    BigDecimal pricePerMg = medicationPriceMap.get(prescribedMedication);
                    BigDecimal dosage = medicationCombination.get(prescribedMedication);
                    BigDecimal dosageInMg = dosage.multiply(patientWeight);
                    BigDecimal dosagePrice = pricePerMg.multiply(dosageInMg);
                    totalMedicationPrice = totalMedicationPrice.add(dosagePrice);
                    medicationOption.put(new Medication(prescribedMedication, pricePerMg), dosageInMg);
                }

                if (leastPrice == null || leastPrice.compareTo(totalMedicationPrice) > 0) {
                    bestPriceMedication = medicationOption;
                    leastPrice = totalMedicationPrice;
                }

            }
        }

        return bestPriceMedication;
    }

    @Override
    public TreatmentPlan treatmentPlanForPatient(Patient patient) {
        TreatmentPlan treatmentPlan = new TreatmentPlan();

        treatmentPlan.setClinics(clinicsBasedOnAgeAndDiseases(patient));
        treatmentPlan.setAgeYearPortion(this.ageInYears(patient));
        treatmentPlan.setAgeMonthPortion(Period.between(patient.getDateOfBirth(), SEP_1_2016).getMonths());

        Map<Medication, BigDecimal> treatmentPalnMedications = new HashMap<>(1);

        Map<Disease, BigDecimal> diseaseLikelihoods = diseaseLikelihoods(patient);

        //gets list of diseases of a patient by filtering only those diseases which have 70% or more likelihood
        List<Disease> patientDiseases = diseaseLikelihoods.keySet().stream().filter(d -> diseaseLikelihoods.get(d).compareTo(new BigDecimal(.70)) >= 0).collect(Collectors.toList());

        for (Disease disease : patientDiseases) {
            Map<Medication, BigDecimal> medicationsForDisease = medicationsForDisease(patient, disease);
            for (Medication medication : medicationsForDisease.keySet()) {
                BigDecimal totalMedicationCost = treatmentPalnMedications.get(medication);
                if (totalMedicationCost == null) {
                    totalMedicationCost = medicationsForDisease.get(medication);
                } else {
                    totalMedicationCost = totalMedicationCost.add(medicationsForDisease.get(medication));
                }
                treatmentPalnMedications.put(medication, totalMedicationCost);
            }
        }

        treatmentPlan.setMedications(treatmentPalnMedications);

        return treatmentPlan;
    }


    /**
     * gets the symptoms of the disease given the disease name
     *
     * @param diseaseName
     * @return list of symptoms
     */
    private List<String> getDiseaseSymptoms(String diseaseName) {
        for (Disease disease : diseases) {
            if (diseaseName.equals(disease.getName())) {
                return disease.getSymptoms();
            }
        }
        return null;
    }

}

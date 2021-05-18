/**
 * This class represents a stratum of the population,
 * some of which receive a given treatment and some of which do not.
 *
 * @author Jake Shannin and Babette A. Brumback, Ph.D.
 * @version 7 January 2021
 */
public class Stratum {
    final double controlRisk, treatmentRisk;
    public Stratum(double controlRisk, double treatmentRisk) {
        this.controlRisk = controlRisk;
        this.treatmentRisk = treatmentRisk;
    }

    interface EffectMeasure {
        double getEffectMeasure();
    }

    EffectMeasure[] effectMeasures = new EffectMeasure[] { this::getRelativeRisk, this::getOddsRatio,
            this::getRiskDifference, this::getOtherRR, this::getHazardRatio, this::getOtherHR};

    public double getRelativeRisk() {
        return treatmentRisk / controlRisk;
    }

    public double getOtherRR() {
        return (1 - controlRisk) / (1 - treatmentRisk);
    }

    public double getOddsRatio() {
        return getRelativeRisk() * getOtherRR();
    }

    public double getRiskDifference() {
        return treatmentRisk - controlRisk;
    }

    public double getHazardRatio() { return Math.log(1 - treatmentRisk) / Math.log(1 - controlRisk); }

    public double getOtherHR() { return Math.log(controlRisk) / Math.log(treatmentRisk); }
}
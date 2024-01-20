import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class closely estimates the probability that subsets of
 * {RR, RR*, HR, HR*, RD, OR}
 * agree or disagree given a uniform probability distribution with specified upper and lower bounds.
 *
 * To run this code, ensure Stratum.java is in the runtime environment and that
 * https://github.com/heberleh/interactivenn/blob/master/web/diagrams/6/diagramas%20base/6waydiagram.svg
 * [34] (Heberle et al.) is downloaded to a Documentation folder in the environment.
 *
 * @author Jake Shannin and Babette A. Brumback, Ph.D. Correspondence to jshannin@ufl.edu.
 * @version 7 January 2021
 */
public class MonteCarloSimulation {
    //Figure 1 uses UPPER_BOUND = 1.0. Figure 2 uses UPPER_BOUND = 0.1.
    private static final double LOWER_BOUND = 0.0, UPPER_BOUND = 1.0;
    static final int PRECISION = 1000000; //number of simulations
    static final int N = (new Stratum(0, 0)).effectMeasures.length;
    static final boolean TENT = true; //set to true in Appendix D to model dependence of p2 on p1

    private static boolean[] isEffectStrongerInSecondStratum(Stratum s1, Stratum s2) {
        return new boolean[]{s2.getRelativeRisk() > s1.getRelativeRisk(), s2.getOddsRatio() > s1.getOddsRatio(),
                s2.getRiskDifference() > s1.getRiskDifference(), s2.getOtherRR() > s1.getOtherRR(),
                s2.getHazardRatio() > s1.getHazardRatio(), s2.getOtherHR() > s1.getOtherHR()};
    }

    static double randRisk() { return LOWER_BOUND + (UPPER_BOUND - LOWER_BOUND) * Math.random(); }
//hifromAnna
    static double tentRisk(double controlRisk) {
        double cdf = Math.random();
        double risk = (UPPER_BOUND - LOWER_BOUND) / 2.0;
        for (double increment = risk / 2.0; increment > 1.0 / PRECISION; increment /= 2) {
            double currentCDF;
            if (risk <= controlRisk) {
                currentCDF = risk * risk - 2 * LOWER_BOUND * risk + LOWER_BOUND * LOWER_BOUND;
                currentCDF /= controlRisk - LOWER_BOUND;
                currentCDF /= UPPER_BOUND - LOWER_BOUND;
            } else {
                currentCDF = risk * risk + controlRisk * (UPPER_BOUND - 2 * risk);
                currentCDF -= UPPER_BOUND * (2 * risk + LOWER_BOUND - 3 * controlRisk);
                currentCDF /= UPPER_BOUND - LOWER_BOUND;
                currentCDF /= UPPER_BOUND - controlRisk;
            }
            if (currentCDF == cdf) {
                return risk;
            }
            if (currentCDF > cdf) {
                risk -= increment;
            } else {
                risk += increment;
            }
        }
        return risk;
    }

    public static boolean[] allAgreement(Stratum s1, Stratum s2) {
        int inclusionDex = -1;
        boolean[] agreement = new boolean[(int)(Math.pow(2, N))];
        while (++inclusionDex < agreement.length) {
            boolean[] objections = new boolean[N];
            String binaryInclusionDex = Integer.toBinaryString(inclusionDex);
            binaryInclusionDex = String.format("%0" + N + "d", Integer.parseInt(binaryInclusionDex));
            for (int i = 0; i < N; i++) {
                objections[i] = binaryInclusionDex.charAt(i) == '1';
            }
            boolean[] comparisons = isEffectStrongerInSecondStratum(s1, s2);
            List<Boolean> votes = IntStream.range(0, N).filter(i -> objections[i]).mapToObj(i -> comparisons[i]).collect(
                    Collectors.toCollection(() -> new ArrayList<>(N)));
            agreement[inclusionDex] = !(votes.contains(true) && votes.contains(false));
        }
        return agreement;
    }

    /**
     * Specific to {RR, RR*, HR, HR*, RD, OR}
     * @param code some subset of abcdef indicating which of the above EMs to include
     * @param totals the 64 tallies of how many times a given set of EMs agreed out of the PRECISION trials
     *               these correspond to the {RR, OR, RD, RR*, HR, HR*} arrangement
     * @return the total corresponding to the code, divided by 1 million
     */
    private static double convertCode(String code, int[] totals) {
        int totalDex = 0;
        if (code.contains("a")) {
            totalDex += 32;
        }
        if (code.contains("d")) {
            totalDex += 4;
        }
        if (code.contains("f")) {
            totalDex += 2;
        }
        if (code.contains("c")) {
            totalDex++;
        }
        if (code.contains("e")) {
            totalDex += 8;
        }
        if (code.contains("b")) {
            totalDex += 16;
        }
        return ((double)totals[totalDex]) / MonteCarloSimulation.PRECISION;
    }

    /**
     * Prints 6-way Venn Diagrams, like Figure 1 and Figure 2, to the console
     * To view diagrams, copy the output to any SVG viewer
     * @param args none
     */
    public static void main(String[] args) {
        int[] agreementTotals = new int[(int) Math.pow(2, MonteCarloSimulation.N)];

        if (TENT) {
            for (int i = 0; i < PRECISION; i++) {
                double p1 = randRisk();
                double p2 = tentRisk(p1);
                double p3 = randRisk();
                double p4 = tentRisk(p3);
                boolean[] simulation = allAgreement(new Stratum(p1, p2), new Stratum(p3, p4));
                IntStream.range(0, simulation.length).filter(j -> simulation[j]).forEachOrdered(j -> agreementTotals[j]++);
            }
        }
        else {
            for (int i = 0; i < PRECISION; i++) {
                boolean[] simulation = allAgreement(new Stratum(randRisk(),
                        randRisk()), new Stratum(randRisk(), randRisk()));
                IntStream.range(0, simulation.length).filter(j -> simulation[j]).forEachOrdered(j -> agreementTotals[j]++);
            }
        }

        //This File is available at https://github.com/heberleh/interactivenn/blob/master/web/diagrams/6/diagramas%20base/6waydiagram.svg
        //Citation [34] (Heberle et al.) in Main Document
        File venn = new File("Documentation/6waydiagram.svg");
        BufferedReader inVenn;
        try {
            inVenn = Files.newBufferedReader(venn.toPath());
            String inLine;
            while ((inLine = inVenn.readLine()) != null) {
                if (inLine.matches("(.*)y=\"[0-9]+\\.[0-9]+\">[a-f]+(.*)")) {
                    String letterCode = "";
                    for (char letter : new char[]{'a', 'b', 'c', 'd', 'e', 'f'}) {
                        int letterDex = inLine.indexOf(letter);
                        int slashDex = inLine.indexOf('/');
                        if (letterDex > 6 && letterDex < slashDex) {
                            letterCode = inLine.substring(letterDex, slashDex - 1);
                            break;
                        }
                    }
                    assert !letterCode.isEmpty();
                    double replacement = convertCode(letterCode, agreementTotals);
                    System.out.print(inLine.substring(0, inLine.indexOf(letterCode)));
                    System.out.print(replacement);
                    System.out.println(inLine.substring(inLine.indexOf('<')));
                }
                else {
                    System.out.println(inLine);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package es.uma.algorithms;

import es.uma.problem.TrainingWeightsProblem;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

abstract public class AbstractAlgorithm extends AbstractAlgorithmRunner {

    private AlgorithmConfiguration algorithmConfiguration;
    private TrainingWeightsProblem problem;
    private List<DoubleSolution> sols;


    public AbstractAlgorithm(AlgorithmConfiguration algorithmConfiguration){
        setAlgorithmConfiguration(algorithmConfiguration);
    }

    abstract public void run();

    public void test(){
        try {

            PrintWriter solf = new PrintWriter("sols.txt");
            PrintWriter fitf = new PrintWriter("fit.txt");
            for(DoubleSolution s: getSols()) {
                solf.println(s.getVariables());
                getProblem().test(s);
                fitf.println(s.getObjective(0) + " " + s.getObjective(1));
            }
            solf.close();
            fitf.close();

        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        }
    }

    private DoubleSolution getBestSolution() {
        DoubleSolution best = null;
        Double best_fit = null;
        for(DoubleSolution s: getSols()){
            double fit = 0;
            for(double d: s.getObjectives()){
                fit += d;
            }
            if(best_fit == null || fit < best_fit){
                best_fit = fit;
                best = s;
            }
        }
        return best;
    }

    public ArrayList<ArrayList<Integer>> getPrediction(){
        return getProblem().getPrediction(getBestSolution());
    }

    public AlgorithmConfiguration getAlgorithmConfiguration() {
        return algorithmConfiguration;
    }

    public void setAlgorithmConfiguration(AlgorithmConfiguration algorithmConfiguration) {
        this.algorithmConfiguration = algorithmConfiguration;
    }

    public TrainingWeightsProblem getProblem() {
        return problem;
    }

    public void setProblem(TrainingWeightsProblem problem) {
        this.problem = problem;
    }

    public List<DoubleSolution> getSols() {
        return sols;
    }

    public void setSols(List<DoubleSolution> sols) {
        this.sols = sols;
    }
}


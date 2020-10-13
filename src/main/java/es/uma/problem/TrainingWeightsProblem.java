package es.uma.problem;

import es.uma.algorithms.AlgorithmConfiguration;
import es.uma.algorithms.RecurrentNeuralNetwork;
import es.uma.data.SimpleDataSet;
import org.uma.jmetal.problem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;

import java.io.IOException;
import java.util.ArrayList;

abstract public class TrainingWeightsProblem extends AbstractDoubleProblem {

    // Variables for the network and dataset
    private SimpleDataSet ds;
    private RecurrentNeuralNetwork net;
    private AlgorithmConfiguration algorithmConfiguration;
    private static Integer DAY = 24*60/4;

    /**
     * Constructor.
     * Creates a default instance of the TrainingWeightsProblem problem.
     */
    public TrainingWeightsProblem(AlgorithmConfiguration algorithmConfiguration) {
        this.setAlgorithmConfiguration(algorithmConfiguration);
        setDs(new SimpleDataSet(getAlgorithmConfiguration().getDatapath()+"/",
                                getAlgorithmConfiguration().getDatafile(), true));

        try {
            getDs().generateFiles(getAlgorithmConfiguration().getPeriod());
        } catch (IOException e) {
            e.printStackTrace();
        }
        getDs().setTrainingSize(getAlgorithmConfiguration().getTraining());

        setNet(new RecurrentNeuralNetwork(getAlgorithmConfiguration().getLayers()));

        getNet().build(40);
    }

    /** Evaluate() method */
    abstract public void evaluate(DoubleSolution solution);

    abstract public void test(DoubleSolution solution);

    public ArrayList<ArrayList<Integer>> getPrediction(DoubleSolution solution){
        ArrayList<ArrayList<Integer>> a = null;
        Integer number = null;
        switch(getAlgorithmConfiguration().getPrediction()){
            case LARGE: number = 30*DAY;
            case MEDIUM: number = 7*DAY;
            case SHORT: number = DAY;
        }
        double[] x = new double[getNumberOfVariables()];
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            x[i] = solution.getVariableValue(i) ;
        }

        getNet().setParameters(x);
        try {
            a = getNet().predict(getDs().getTestData(), number);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return a;
    }

    public SimpleDataSet getDs() {
        return ds;
    }

    public void setDs(SimpleDataSet ds) {
        this.ds = ds;
    }

    public RecurrentNeuralNetwork getNet() {
        return net;
    }

    public void setNet(RecurrentNeuralNetwork net) {
        this.net = net;
    }

    public AlgorithmConfiguration getAlgorithmConfiguration() {
        return algorithmConfiguration;
    }

    public void setAlgorithmConfiguration(AlgorithmConfiguration algorithmConfiguration) {
        this.algorithmConfiguration = algorithmConfiguration;
    }
}
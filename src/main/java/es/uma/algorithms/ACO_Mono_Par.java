package es.uma.algorithms;

import es.uma.problem.TrainingWeightsProblem_Mono;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.crossover.SBXCrossover;
import org.uma.jmetal.operator.impl.mutation.UniformMutation;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.MultithreadedSolutionListEvaluator;
import org.uma.jmetal.util.neighborhood.Neighborhood;


public class ACO_Mono_Par extends AbstractAlgorithm {

    public ACO_Mono_Par(AlgorithmConfiguration algorithmConfiguration){
        super(algorithmConfiguration);
    }

    public void run()  {
        SolutionListEvaluator<DoubleSolution> evaluation;
        BoundedArchive<DoubleSolution> archive;
        Neighborhood<DoubleSolution> neighborhood;
        CrossoverOperator crossover;
        UniformMutation mutation;
        SelectionOperator selectionOperator;
        ACO algorithm;

        setProblem(new TrainingWeightsProblem_Mono(getAlgorithmConfiguration()));
        evaluation = new MultithreadedSolutionListEvaluator<>(getAlgorithmConfiguration().getNumberOfThreads(), getProblem());
        mutation = new UniformMutation( getAlgorithmConfiguration().getMutationProbability(),
                                        getAlgorithmConfiguration().getPerturbation());
        crossover = new SBXCrossover(getAlgorithmConfiguration().getCrossoverProbability(),
                                    getAlgorithmConfiguration().getCrossoverDistributionIndex());
        selectionOperator = new BinaryTournamentSelection();

        algorithm = new ACO(getProblem(), getAlgorithmConfiguration().getMaxIterations(),
                             getAlgorithmConfiguration().getPopSize(), crossover, mutation,
                             selectionOperator, evaluation, 10);

        algorithm.run();

        setSols(algorithm.getResult());
    }

}


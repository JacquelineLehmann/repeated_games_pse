package loop.model.simulator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import loop.model.UserConfiguration;
import loop.model.simulationengine.Configuration;
import loop.model.simulationengine.IterationResult;
import loop.model.simulationengine.SimulationEngine;
import loop.model.simulator.exception.ConfigurationException;
import loop.model.simulator.exception.SimulationEngineException;

/**
 * An implementation of the {@link Simulator} interface. Executes the iterations
 * in parallel using a thread pool.
 *
 * @author Christian Schorr
 */
public class ThreadPoolSimulator implements Simulator {

    private static final int DEFAULT_MAX_THREAD_COUNT = 4;

    private ThreadPoolExecutor threadPool;

    private LinkedList<SimulatorTask> runningSimulations;
    private ArrayList<SimulationResult> finishedSimulations;

    private static int nextSimulationId = 1;

    /**
     * Creates a new ThreadPoolSimulator .
     */
    public ThreadPoolSimulator() {
        this(DEFAULT_MAX_THREAD_COUNT);
    }

    /**
     * Creates a new ThreadPoolSimulator with the given maximum amount of running
     * threads.
     *
     * @param maxThreads the maximum amount of running threads in the thread pool
     */
    public ThreadPoolSimulator(int maxThreads) {
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
        runningSimulations = new LinkedList<>();
        finishedSimulations = new ArrayList<>();
    }

    /**
     * Increments the SimulationId by 1
     */
    public void incrementSimulationId() {
        nextSimulationId++;
    }

    /**
     * Returns the Id of the next Simulation
     *
     * @return Id of the next Simulation
     */
    public int getSimulationId() {
        return nextSimulationId;
    }

    /**
     * Stops all Simulations and shuts down the thread pool
     */
    public void stopSimulator() throws InterruptedException {
        threadPool.shutdown();
        threadPool.awaitTermination(100000, TimeUnit.MILLISECONDS); // wait for 10s in this case
        threadPool.shutdownNow();
        threadPool = null;
    }

    @Override
    public SimulationResult startSimulation(UserConfiguration config) throws ConfigurationException {
        return startSimulation(config, (res) -> {
        });
    }

    @Override
    public SimulationResult startSimulation(UserConfiguration config, Consumer<SimulationResult> action)
            throws ConfigurationException {
        SimulationResult simResult = new SimulationResult(config, nextSimulationId++);
        ConfigurationBuffer configBuffer = new ConfigurationBuffer(config);

        SimulatorTask task = new SimulatorTask(simResult, configBuffer, action);
        runningSimulations.add(task);
        task.runningIterations.add(CompletableFuture.supplyAsync(() -> {
            simResult.setStatus(SimulationStatus.RUNNING);
            return null;
        }, threadPool));
        final int iterationCount = simResult.getTotalIterations();
        for (int i = 0; i < iterationCount - 1; i++) {
            task.runningIterations.add(runIteration(task, false));
        }
        task.runningIterations.add(runIteration(task, true));
        return simResult;
    }

    private Future<IterationResult> runIteration(SimulatorTask task, boolean last) {

        Future<IterationResult> future = threadPool.submit(() -> {
            ConfigurationBuffer.ConfigNumber configNum = task.getNextConfiguration();
            try {
                SimulationEngine engine = new SimulationEngine();
                engine.executeIteration(configNum.config);
                IterationResult result = new ConcreteIterationResultCreator().createIterationResult(engine, task.simResult.getUserConfiguration());
                task.simResult.addIterationResult(result, configNum.index);

                if (last) {
                    finishedSimulations.add(task.simResult);
                    runningSimulations.remove(task);
                    task.finishedHandler.accept(task.simResult);
                }
                return result;
            } catch (Exception ex) {
                task.simResult.addSimulationEngineException(new SimulationEngineException());
                if (last) {
                    finishedSimulations.add(task.simResult);
                    runningSimulations.remove(task);
                }
                return null;
            }
        });
        return future;
    }

    @Override
    public boolean stopSimulation(SimulationResult sim) {
        SimulatorTask task = runningSimulations.stream()
                .filter((tsk) -> tsk.simResult.equals(sim))
                .findFirst().orElse(null);
        if (task == null) return false;
        for (Future<IterationResult> future : task.runningIterations)
            future.cancel(true);
        runningSimulations.remove(task);
        task.simResult.setStatus(SimulationStatus.CANCELED);
        return true;
    }

    @Override
    public boolean stopSimulation(int id) {
        SimulatorTask task = runningSimulations.stream()
                .filter((tsk) -> tsk.simResult.getId() == id)
                .findFirst().orElse(null);
        if (task == null)
            return false;
        return stopSimulation(task.simResult);
    }

    @Override
    public void stopAllSimulations() {
        for (SimulatorTask tsk : runningSimulations)
            stopSimulation(tsk.simResult);
    }

    @Override
    public SimulationResult getSimulation(int id) {
        SimulationResult result = runningSimulations.stream()
                .filter((tsk) -> tsk.simResult.getId() == id)
                .map((tsk) -> tsk.simResult)
                .findFirst().orElse(null);
        return result;
    }

    /**
     * Returns the amount of currently executed iterations.
     *
     * @return the amount of currently executed iterations
     */
    public int getRunningIterationCount() {
        int count = 0;
        for (SimulatorTask task : runningSimulations) {
            count += task.runningIterations.size();
        }
        return count;
    }

    /**
     * Returns the amount of iterations currently waiting for execution.
     *
     * @return the amount of iterations currently waiting for execution
     */
    public int getQueuedIterationCount() {
        int count = 0;
        for (SimulatorTask task : runningSimulations) {
            count += task.totalIterationsLeft;
        }
        return count;
    }

    private class SimulatorTask {

        private SimulationResult simResult;
        private ConfigurationBuffer buffer;
        private Consumer<SimulationResult> finishedHandler;

        private int totalIterationsLeft;
        private ArrayList<Integer> iterationsLeft;
        private ArrayList<Future<IterationResult>> runningIterations;

        public SimulatorTask(SimulationResult result, ConfigurationBuffer buffer,
                             Consumer<SimulationResult> finishedHandler) {
            simResult = result;
            this.buffer = buffer;
            this.finishedHandler = finishedHandler;
            runningIterations = new ArrayList<>();
            iterationsLeft = new ArrayList<>();

            List<Configuration> configs = buffer.peekAllConfigurations();
            for (Configuration config : configs) {
                totalIterationsLeft += config.getTotalIterations();
                iterationsLeft.add(config.getTotalIterations());
            }
            simResult.setTotalIterations(totalIterationsLeft);
        }

        private synchronized ConfigurationBuffer.ConfigNumber getNextConfiguration()
                throws ConfigurationException {
            if (totalIterationsLeft <= 0)
                return null;
            totalIterationsLeft--;
            for (int i = 0; i < iterationsLeft.size(); i++) {
                if (iterationsLeft.get(i) > 0) {
                    iterationsLeft.set(i, iterationsLeft.get(i) - 1);
                    return buffer.getConfiguration(i);
                }
            }
            return null;
        }
    }

    private class ConfigurationBuffer {

        private UserConfiguration config;
        private ArrayList<Queue<Configuration>> buffer;

        public ConfigurationBuffer(UserConfiguration config) throws ConfigurationException {
            this.config = config;

            // initialize the buffer
            buffer = new ArrayList<>();
            List<Configuration> configs = ConfigurationCreator.generateConfigurations(config);
            for (Configuration engineConfig : configs) {
                Queue<Configuration> configBuffer = new LinkedList<>();
                configBuffer.add(engineConfig);
                buffer.add(configBuffer);
            }

            // fill the buffer with some copies
            for (int i = 0; i <= buffer.get(0).peek().getTotalIterations(); i++) {
                configs = ConfigurationCreator.generateConfigurations(config);
                for (int j = 0; j < configs.size(); j++) {
                    buffer.get(j).add(configs.get(j));
                }
            }
        }

        private synchronized ConfigNumber getConfiguration(int index) throws ConfigurationException {
            // generate new configuration if buffer is empty (should never happen)
            if (buffer.get(index).isEmpty()) {
                List<Configuration> configs = ConfigurationCreator.generateConfigurations(config);
                for (int j = 0; j < configs.size(); j++) {
                    buffer.get(j).add(configs.get(j));
                }
            }
            return new ConfigNumber(index, buffer.get(index).remove());
        }

        private List<Configuration> peekAllConfigurations() {
            return buffer.stream().map((q) -> q.peek()).collect(Collectors.toList());
        }

        private class ConfigNumber {
            private int index;
            private Configuration config;

            private ConfigNumber(int index, Configuration config) {
                this.index = index;
                this.config = config;
            }
        }
    }
}

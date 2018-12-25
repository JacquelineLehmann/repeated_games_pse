package loop.model.simulationengine;

import java.util.ArrayList;
import java.util.List;

import loop.model.simulationengine.distributions.UniformFiniteDistribution;

/**
 * Realises the random pairing of agents.
 * 
 * @author Peter Koepernik
 *
 */
public class RandomPairBuilder implements PairBuilder {

    @Override
    public List<AgentPair> buildPairs(final List<Agent> agents, final SimulationHistory history) {
        List<AgentPair> pairs = new ArrayList<AgentPair>(agents.size() / 2);
        UniformFiniteDistribution<Agent> agentDist = new UniformFiniteDistribution<Agent>(agents);
        
        while (2 * pairs.size() < agents.size() - 1) {
            pairs.add(new ConcreteAgentPair(agentDist.pickAndRemove(), agentDist.pickAndRemove()));
        }
        return pairs;
    }

}

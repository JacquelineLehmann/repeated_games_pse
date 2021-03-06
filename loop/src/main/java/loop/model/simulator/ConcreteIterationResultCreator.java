package loop.model.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import loop.model.Group;
import loop.model.UserConfiguration;
import loop.model.repository.CentralRepository;
import loop.model.simulationengine.IterationResult;
import loop.model.simulationengine.SimulationEngine;

/**
 * An implementation of the {@link IterationResultCreator} interface.
 * 
 * @author Peter Koepernik
 *
 */
public class ConcreteIterationResultCreator implements IterationResultCreator {

    @Override
    public IterationResult createIterationResult(SimulationEngine engine, UserConfiguration configuration) {
        
        if (!engine.isFinished()) {
            throw new IllegalArgumentException("Given engine has not executed an iteration yet.");
        }
        
        boolean equilibriumReached = engine.equilibriumReached();
        double efficiency = engine.getEfficiency();
        int adapts = engine.getAdapts();
        List<String> strategyNames = engine.getStrategyNames();
        
        //used for strategy portions and group capitals
        List<String> groupNames = CentralRepository.getInstance().getPopulationRepository()
                .getEntityByName(configuration.getPopulationName()).getGroupNames();
        List<Group> groups = groupNames.stream().map(
                name -> CentralRepository.getInstance().getGroupRepository().getEntityByName(name)).collect(Collectors.toList());
        
        //strategy portions
        Map<Integer, List<double[]>> strategyPortionsByGroupId = engine.getStrategyPortions();
        Map<String, List<double[]>> strategyPortionsByGroupName = new HashMap<String, List<double[]>>();
        if (strategyPortionsByGroupId.containsKey(-1)) {
            strategyPortionsByGroupName.put("Groupless Agents", strategyPortionsByGroupId.get(-1));
        }
        strategyPortionsByGroupId.keySet().stream().filter(id -> id != -1).forEach(id ->
            strategyPortionsByGroupName.put(groupNames.get(id), strategyPortionsByGroupId.get(id)));
        
        //group capitals
        Map<String, List<Integer>> groupCapitals = new HashMap<String, List<Integer>>();
        
        groups.stream().filter(group -> group.isCohesive()).forEach((group) -> groupCapitals.put(group.getName(), new ArrayList<Integer>()));
        if (groups.stream().anyMatch(group -> !group.isCohesive()))
            groupCapitals.put("Groupless Agents", new ArrayList<Integer>());

        //cohesive groups
        engine.getAgents().stream().filter(a -> a.getGroupId() != -1).forEach(
                a -> groupCapitals.get(groups.get(a.getGroupId()).getName()).add(a.getCapital()));
        //groupless agents
        engine.getAgents().stream().filter(a -> a.getGroupId() == -1).forEach(
                a -> groupCapitals.get("Groupless Agents").add(a.getCapital()));
        
        return new IterationResult(equilibriumReached, efficiency, adapts, strategyNames, strategyPortionsByGroupName, groupCapitals);
    }

}

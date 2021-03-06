package loop.model.simulationengine.strategies;

import loop.model.simulationengine.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class represents a pure strategy. It is uniquely determined by a condition on the
 * enemy agent based on the history of the simulation, represented as
 * {@link BiPredicate<AgentPair ,SimulationHistory>}.
 * <p>
 * It also provides static access to some standard strategies such as grim, tit-for-tat,
 * always cooperate or never cooperate
 *
 * @author Peter Koepernik
 */
public class PureStrategy implements Strategy, java.io.Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 7070470743541221391L;
    private String name;
    private String description;
    private BiPredicate<AgentPair, SimulationHistory> condition;

    public PureStrategy(final String name, final String description, final BiPredicate<AgentPair, SimulationHistory> condition) {
        this.name = name;
        this.description = description;
        this.condition = condition;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public boolean isCooperative(Agent player, Agent opponent, SimulationHistory history) {
        return condition.test(new ConcreteAgentPair(player, opponent), history);
    }

    @Override
    public double getCooperationProbability(Agent player, Agent opponent, SimulationHistory history) {
        return (condition.test(new ConcreteAgentPair(player, opponent), history)) ? 1 : 0;
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the tit-for-tat strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the tit-for-tat strategy
     */
    public static PureStrategy titForTat() {
        return new PureStrategy(
                "tit-for-tat", "A player using tit-for-tat will cooperate in the first game. In every following game, he will"
                        + " cooperate if and only if the opponent cooperated in the previous game."
                , (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) -> {
            Agent opponent = pair.getSecondAgent();
            for (GameResult result : history.getResultsByAgent(pair.getFirstAgent())) {
                // first hit is also latest result; maybe not so pretty but efficient
                if (result.hasAgent(opponent)) {
                    return result.hasCooperated(opponent);
                }
            }
            return true;
        });
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the tit-for-tat strategy, where instead
     * of looking at the last game between the player and the opponent the last game between the opponent and an
     * agent of the same (cohesive) group as the player is considered. if the player is part of a non-cohesive group,
     * this strategy leads to the same results as the common tit-for-tat strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the group tit-for-tat strategy
     */
    public static PureStrategy groupTitForTat() {

        return new PureStrategy(
                "group tit-for-tat", "A player using group tit-for-tat uses the tit-for-tat strategy, where instead of looking "
                + "at the last game between the player and the opponent the last game between the opponent and any agent "
                + "of the same (cohesive) group as the player is considered. If the player is part of a non-cohesive group, "
                + "this strategy leads to the same results as the common tit-for-tat strategy.",
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) -> {
                    Agent agent = pair.getFirstAgent();
                    Agent opponent = pair.getSecondAgent();
                    for (GameResult result : history.getResultsByAgent(opponent)) {
                        if (result.getOtherAgent(opponent).isGroupAffiliated(agent)) {
                            return result.hasCooperated(opponent);
                        }
                    }
                    return true;
                }
        );
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the grim strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the grim strategy
     */
    public static PureStrategy grim() {
        return new PureStrategy(
                "grim", "A player using grim will cooperate in the first game. In the following games, he cooperates if and only if the opponent"
                        + " cooperated in all previous games.",
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) -> {
                    for (GameResult result : history.getResultsByAgent(pair.getFirstAgent())) {
                        if (result.hasAgent(pair.getSecondAgent()) && !result.hasCooperated(pair.getSecondAgent()))
                            return false;
                    }
                    return true;
                });
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the grim strategy, where instead
     * of looking at the last game between the player and the opponent the last game between the opponent and an
     * agent of the same (cohesive) group as the player is considered. if the player is part of a non-cohesive group,
     * this strategy leads to the same results as the common grim strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the group grim strategy
     */
    public static PureStrategy groupGrim() {

        return new PureStrategy(
                "group grim", "A player using group grim uses the grim strategy, where instead of looking at the last game between"
                + " the player and the opponent the last game between the opponent and any agent of the same (cohesive) group"
                + " as the player is considered. If the player is part of a non-cohesive group, this strategy leads to the same results"
                + " as the common grim strategy.",
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) -> {
                    Agent agent = pair.getFirstAgent();
                    Agent opponent = pair.getSecondAgent();
                    for (GameResult result : history.getResultsByAgent(opponent)) {
                        if (result.getOtherAgent(opponent).isGroupAffiliated(agent)) {
                            if (!result.hasCooperated(opponent)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
        );
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "always cooperate" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "always cooperate" strategy
     */
    public static PureStrategy alwaysCooperate() {
        return new PureStrategy("always cooperate", "A player using this strategy will cooperate in every game.",
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) -> true);
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "never cooperate" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "never cooperate" strategy
     */
    public static PureStrategy neverCooperate() {
        return new PureStrategy("never cooperate", "A player using this strategy won't cooperate in any game.",
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) -> false);
    }

    public enum TimeAdverb {
        ALWAYS, NEVER, ATLEASTONCE, LASTTIME;
    }

    public enum AgentEntity {
        AGENT, SAME_GROUP, SIM_CAPITAL;
    }

    //percentage ignored if coopWithWhom != SIM_CAPITAL
    public static PureStrategy stratBuilderStrategy(AgentEntity cooperatedWithWhom, TimeAdverb when, double percentage) {
        BiPredicate<AgentPair, GameResult> relevantResult;
        switch (cooperatedWithWhom) {
            case AGENT:
                relevantResult = (BiPredicate<AgentPair, GameResult> & Serializable)
                        (pair, result) -> result.hasAgent(pair.getFirstAgent());
                break;
            case SAME_GROUP:
                relevantResult = (BiPredicate<AgentPair, GameResult> & Serializable)
                        (pair, result) -> pair.getFirstAgent().isGroupAffiliated(result.getOtherAgent(pair.getSecondAgent()));
                break;
            case SIM_CAPITAL:
                relevantResult = (BiPredicate<AgentPair, GameResult> & Serializable)
                        (pair, result) -> hasSimilarCapital(pair.getFirstAgent(), result.getOtherAgent(pair.getSecondAgent()), percentage);
                break;
            default:
                relevantResult = null;
        }

        BiPredicate<AgentPair, SimulationHistory> condition;
        switch (when) {
            case ALWAYS:
                condition = (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) ->
                        history.getResultsByAgent(pair.getSecondAgent()).stream()
                                .filter((result) -> relevantResult.test(pair, result))
                                .allMatch((result) -> result.hasCooperated(pair.getSecondAgent()));
                break;
            case NEVER:
                condition = (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) ->
                        !history.getResultsByAgent(pair.getSecondAgent()).stream()
                                .filter((result) -> relevantResult.test(pair, result))
                                .anyMatch((result) -> result.hasCooperated(pair.getSecondAgent()));
                break;
            case ATLEASTONCE:
                condition = (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) ->
                        history.getResultsByAgent(pair.getSecondAgent()).stream()
                                .filter((result) -> relevantResult.test(pair, result))
                                .anyMatch((result) -> result.hasCooperated(pair.getSecondAgent()));
                break;
            case LASTTIME:
                condition = (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) ->
                        toStream(history.getLatestWhere((result) -> result.hasAgent(pair.getSecondAgent()) && relevantResult.test(pair, result))).allMatch(
                                (result) -> result == null || result.hasCooperated(pair.getSecondAgent()));
                break;
            default:
                condition = null;
        }

        return new PureStrategy("stratbuilder strategy", "opponent cooperated with " + cooperatedWithWhom.toString() + ", " + when.toString(),
                condition);
    }

    public static boolean hasSimilarCapital(Agent a1, Agent a2, double percentage) {
        if (a1.getCapital() > a2.getCapital())
            return ((double) a2.getCapital() / (double) a1.getCapital()) >= 1 - percentage;
        else
            return ((double) a1.getCapital() / (double) a2.getCapital()) >= 1 - percentage;
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "The opponent always cooperated" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "The opponent always cooperated" strategy
     */
    public static PureStrategy opponentAlwaysCooperated() {
        BiPredicate<AgentPair, SimulationHistory> condition =
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, hist) -> {
                    Agent opponent = pair.getSecondAgent();
                    for (GameResult res : hist.getResultsByAgent(pair.getFirstAgent())) {
                        if (res.hasAgent(opponent) && !res.hasCooperated(opponent))
                            return false;
                    }
                    return true;
                };

        return new PureStrategy("The opponent always cooperated", "A player using this strategy will first cooperate, afterwards he refer to the previous actions of the opponent."
                + " If the opponent previously was always cooperative, the agent is cooperative. If the opponent was at least one time "
                + "not cooperative, the agent is from now on not cooperative to that opponent.", condition);
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "The opponent cooperated at least once" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "The opponent cooperated at least once" strategy
     */
    public static PureStrategy opponentCooperatedAtLeastOnce() {
        BiPredicate<AgentPair, SimulationHistory> condition =
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, hist) -> {
                    Agent opponent = pair.getSecondAgent();
                    for (GameResult res : hist.getResultsByAgent(pair.getFirstAgent())) {
                        if (res.hasAgent(opponent) && res.hasCooperated(opponent))
                            return true;
                    }
                    return false;

                };

        return new PureStrategy("The opponent cooperated at least once", "A player using this strategy will not cooperate first, afterwards he refer to the previous actions of the opponent."
                + " If the opponent previously has one time cooperated with the player, the player is cooperative. If the opponent was never"
                + " cooperative, the player isn't cooperative to that opponent.", condition);
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "The opponent cooperated last time" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "The opponent cooperated last time" strategy
     */
    public static PureStrategy opponentCooperatedLastTime() {
        BiPredicate<AgentPair, SimulationHistory> condition =
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) -> {
                    Agent opponent = pair.getSecondAgent();
                    for (GameResult res : history.getResultsByAgent(pair.getFirstAgent())) {
                        if (res.hasAgent(opponent))
                            return res.hasCooperated(opponent);
                    }
                    return true;
                };

        return new PureStrategy("The opponent cooperated last time", "A player using this strategy will first cooperate, afterwards he replicate the opponent's previous action."
                + " If the opponent previously was cooperative, the player is cooperative. If the opponent previously wasn't cooperative,"
                + " the player is not cooperative.", condition);
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "The opponent never cooperated" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "The opponent never cooperated" strategy
     */
    public static PureStrategy opponentCooperatedNever() {
        BiPredicate<AgentPair, SimulationHistory> condition =
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, hist) -> {
                    Agent opponent = pair.getSecondAgent();
                    for (GameResult res : hist.getResultsByAgent(pair.getFirstAgent())) {
                        if (res.hasAgent(opponent) && res.hasCooperated(opponent))
                            return false;
                    }
                    return true;
                };

        return new PureStrategy("The opponent never cooperated", "A player using this strategy will first cooperate, afterwards he refer to the previous actions of the opponent."
                + " If the opponent previously was never cooperative, the player is cooperative. If the opponent was at least one time "
                + " cooperative, the player is from now on not cooperative to this opponent.", condition);
    }

    //////////////

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "I always cooperated" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "I always cooperated" strategy
     */
    public static PureStrategy currAgentAlwaysCooperated() {
        BiPredicate<AgentPair, SimulationHistory> condition =
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, hist) -> {
                    Agent currAgent = pair.getFirstAgent();
                    Agent opponent = pair.getSecondAgent();
                    for (GameResult result : hist.getResultsByAgent(currAgent)) {
                        if (result.hasAgent(opponent) && !result.hasCooperated(currAgent))
                            return false;
                    }
                    return true;
                };

        return new PureStrategy("I always cooperated", "A player using this strategy will first cooperate, afterwards he refer to his previous actions."
                + " If the player previously was always cooperative to this opponent, the player is cooperative. If the player was at least one time "
                + "not cooperative to this opponent, the player is from now on not cooperative to this opponent.", condition);
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "I cooperated at least once" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "I cooperated at least once" strategy
     */
    public static PureStrategy currAgentCooperatedAtLeastOnce() {
        BiPredicate<AgentPair, SimulationHistory> condition =
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, hist) -> {
                    Agent currAgent = pair.getFirstAgent();
                    Agent opponent = pair.getSecondAgent();
                    for (GameResult result : hist.getResultsByAgent(currAgent)) {
                        if (result.hasAgent(opponent) && result.hasCooperated(currAgent))
                            return true;
                    }
                    return false;
                };

        return new PureStrategy("I cooperated at least once", "A player using this strategy will not cooperate first, afterwards he refer to his previous actions."
                + " If the player previously was at least one time cooperative to this opponent, the player is cooperative. If the player was never "
                + " cooperative to this opponent, the player is not cooperative to this opponent.", condition);
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "I cooperated last time" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "I cooperated last time" strategy
     */
    public static PureStrategy currAgentCooperatedLastTime() {
        BiPredicate<AgentPair, SimulationHistory> condition =
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, hist) -> {
                    Agent currAgent = pair.getFirstAgent();
                    Agent opponent = pair.getSecondAgent();
                    for (GameResult result : hist.getResultsByAgent(currAgent)) {
                        if (result.hasAgent(opponent))
                            return result.hasCooperated(currAgent);
                    }
                    return true;
                };

        return new PureStrategy("I cooperated last time", "A player using this strategy will first cooperate, afterwards he refer to his previous action."
                + " If the player previously was the last time cooperative to this opponent, the player is cooperative. If the player was the last time "
                + "  not cooperative to this opponent, the player is not cooperative to this opponent.", condition);
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "I never cooperated" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "I never cooperated" strategy
     */
    public static PureStrategy currAgentCooperatedNever() {
        BiPredicate<AgentPair, SimulationHistory> condition =
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, hist) -> {
                    Agent currAgent = pair.getFirstAgent();
                    Agent opponent = pair.getSecondAgent();
                    for (GameResult result : hist.getResultsByAgent(currAgent)) {
                        if (result.hasAgent(opponent) && result.hasCooperated(currAgent))
                            return false;
                    }
                    return true;
                };

        return new PureStrategy("I never cooperated", "A player using this strategy will first cooperate, afterwards he refer to his previous actions."
                + " If the player previously was never cooperative to this opponent, the player is cooperative. If the player was at least one time "
                + " cooperative to this opponent, the player is not cooperative to this opponent.", condition);
    }

    ////////////

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "The opponent has a higher capital" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "The opponent has a higher capital" strategy
     */
    public static PureStrategy opponentHasHigherCapital() {
        BiPredicate<AgentPair, SimulationHistory> condition =
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, hist) ->
                        pair.getFirstAgent().getCapital() < pair.getSecondAgent().getCapital();

        return new PureStrategy("The opponent has a higher capital", "A player using this strategy will be cooperative if the opponent "
                + "has a higher capital than the player else he won't be cooperative.", condition);
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "The opponent has a lower capital" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "The opponent has a lower capital" strategy
     */
    public static PureStrategy opponentHasLowerCapital() {
        BiPredicate<AgentPair, SimulationHistory> condition =
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, hist) ->
                        pair.getFirstAgent().getCapital() > pair.getSecondAgent().getCapital();

        return new PureStrategy("The opponent has a lower capital", "A player using this strategy will be cooperative if the opponent "
                + "has a lower capital than the player else he won't be cooperative.", condition);
    }

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "The opponent has a similar capital" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "The opponent has a similar capital" strategy
     */
    public static PureStrategy opponentHasSimilarCapital(double percentage) {
        BiPredicate<AgentPair, SimulationHistory> condition =
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, hist) ->
                        hasSimilarCapital(pair.getFirstAgent(), pair.getSecondAgent(), percentage);

        return new PureStrategy("The opponent has a similar capital", "A player using this strategy will be cooperative if the opponent "
                + "has a similar capital than the player else he won't be cooperative.", condition);
    }

    //////

    /**
     * Returns an instance of the {@link PureStrategy} class representing the "The opponent is in the same group" strategy.
     *
     * @return an instance of the {@link PureStrategy} class representing the "The opponent is in the same group" strategy
     */
    public static PureStrategy opponentIsInTheSameGroup() {
        BiPredicate<AgentPair, SimulationHistory> condition =
                (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, hist) ->
                        pair.getFirstAgent().isGroupAffiliated(pair.getSecondAgent());

        return new PureStrategy("The opponent is in the same group", "A player using this strategy will be cooperative if the opponent "
                + "is in the same group else the player is not cooperative.", condition);
    }


    public static PureStrategy groupMemberCooperation(TimeAdverb when) {
        BiPredicate<AgentPair, GameResult> relevantResult = (pair, result) ->
                pair.getFirstAgent().isGroupAffiliated(result.getOtherAgent(pair.getSecondAgent()));


        BiPredicate<AgentPair, SimulationHistory> condition;

        switch (when) {
            case ALWAYS:
                condition = (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) -> {
                    List<GameResult> results = history.getResultsByAgent(pair.getSecondAgent()).stream()
                            .filter(res -> relevantResult.test(pair, res)).collect(Collectors.toList());
                    return results.stream()
                            .map((result) -> result.getOtherAgent(pair.getSecondAgent()))
                            .anyMatch((agent) -> results.stream()
                                    .filter((result) -> result.hasAgent(agent))
                                    .allMatch((result) -> result.hasCooperated(agent)));
                };
                break;
            case NEVER:
                condition = (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) -> {
                    List<GameResult> results = history.getResultsByAgent(pair.getSecondAgent()).stream()
                            .filter(res -> relevantResult.test(pair, res)).collect(Collectors.toList());
                    return results.stream()
                            .map((result) -> result.getOtherAgent(pair.getSecondAgent()))
                            .anyMatch((agent) -> !results.stream()
                                    .filter((result) -> result.hasAgent(agent))
                                    .anyMatch((result) -> result.hasCooperated(agent)));
                };
                break;
            case ATLEASTONCE:
                condition = (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) ->
                        history.getResultsByAgent(pair.getSecondAgent()).stream()
                                .filter((result) -> relevantResult.test(pair, result))
                                .anyMatch((result) -> result.hasCooperated(result.getOtherAgent(pair.getSecondAgent())));
                break;
            case LASTTIME:
                condition = (BiPredicate<AgentPair, SimulationHistory> & Serializable) (pair, history) -> {
                    List<GameResult> results = history.getResultsByAgent(pair.getSecondAgent()).stream()
                            .filter((result) -> relevantResult.test(pair, result)).collect(Collectors.toList());
                    return results.stream()
                            .map((result) -> result.getOtherAgent(pair.getSecondAgent()))
                            .anyMatch((agent) -> results.stream()
                                    .filter(res -> res.hasAgent(agent))
                                    .findFirst().orElse(null)
                                    .hasCooperated(agent));
                };
                break;
            default:
                condition = null;
        }

        String adv = " ";
        switch (when) {
            case NEVER:
                adv = "never";
                break;
            case ALWAYS:
                adv = "always";
                break;
            case LASTTIME:
                adv = "last time";
                break;
            case ATLEASTONCE:
                adv = "at least once";
                break;
        }
        return new PureStrategy("An agent of the same group has cooperated " + adv, "", condition);
    }


    private static <T> Stream<T> toStream(T t) {
        List<T> list = new ArrayList<T>();
        list.add(t);
        return list.stream();
    }

}

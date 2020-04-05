package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.StripsEnum;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static edu.cwru.sepia.agent.planner.actions.StripsEnum.*;
import static edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import static edu.cwru.sepia.environment.model.state.ResourceType.GOLD;
import static edu.cwru.sepia.environment.model.state.ResourceType.WOOD;

/*
 * Essentially we tried to cut out as much data-redundancy as we could while
 * not breaking our puny brains. GameState handles all state mutations from
 * the initial GameState. All state contemplation is done in AStar.
 */

// GameState only takes a snapshot of either peasants at the town hall or at
// resources. Thus, the isAvailable() schema in unit tracker doesn't make sense.
// Revert back to ready() sets. However, generalize it to essentially a state
// transition diagram so that it can extend to moving "workers" between a
// variety of pools.
//
public class GameState implements Comparable<GameState>
{
    private UnitTracker unitTracker;
    private ResourceTracker resourceTracker;
    private double cost;
    private double heuristicCost;
    private boolean buildPeasants;
    private GameState cameFrom;

    /**
     * Construct a GameState from a stateview object. This is used to
     * construct the initial search node. All other nodes should be
     * constructed from the another constructor you create or by factory
     * functions that you create.
     *
     * @param state         The current stateview at the time the plan is
     *                      being created
     * @param playernum     The player number of agent that is planning
     * @param requiredGold  The goal amount of gold (e.g. 200 for the small
     *                      scenario)
     * @param requiredWood  The goal amount of wood (e.g. 200 for the small
     *                      scenario)
     * @param buildPeasants True if the BuildPeasant action should be considered
     */
    public GameState(StateView state,
                     int playernum,
                     int requiredGold,
                     int requiredWood,
                     boolean buildPeasants)
    {
        this.unitTracker = createUnitTracker(state);
        this.resourceTracker = createResourceTracker(state,
                                                     playernum,
                                                     requiredGold,
                                                     requiredWood);
        this.cost = 0;
        this.heuristicCost = requiredGold + requiredGold;
        this.cameFrom = null;
        this.buildPeasants = buildPeasants;
    }

    // Copy constructor. Used most of the time.
    public GameState(GameState gameState)
    {
        this.resourceTracker = gameState.getResourceTracker();
        this.unitTracker = gameState.getUnitTracker();
        this.cost = gameState.getCost();
        this.heuristicCost = gameState.getHeuristicCost();
        this.cameFrom = gameState;
    }

    public UnitTracker createUnitTracker(StateView state)
    {
        List<UnitView> peasantUnits = state.getAllUnits();
        peasantUnits.removeIf(u -> u.equals(getTownHall(state)));
        List<Unit> units = new ArrayList<>();
        for (UnitView u : peasantUnits)
            units.add(new UnitBuilder().initialAction(IDLE)
                                       .validActions(IDLE, GATHER, DEPOSIT)
                                       .goldCostToProduce(u.getTemplateView()
                                                           .getGoldCost())
                                       .woodCostToProduce(u.getTemplateView()
                                                           .getWoodCost())
                                       .build());
        units.add(new UnitBuilder().initialAction(IDLE)
                                   .validActions(IDLE, PRODUCE)
                                   .build());
        return new UnitTrackerBuilder().units(units).build();
    }

    public ResourceTracker createResourceTracker(StateView state,
                                                 int playerNum,
                                                 int requiredGold,
                                                 int requiredWood)
    {
        ResourceTrackerBuilder b = new ResourceTrackerBuilder();
        return b.currentAmount(GOLD, state.getResourceAmount(playerNum, GOLD))
                .currentAmount(WOOD, state.getResourceAmount(playerNum, WOOD))
                .resources(getResources(state, Type.GOLD_MINE, Type.TREE))
                .goal(new GoalBuilder<Integer>().specify("gold", requiredGold)
                                                .specify("wood", requiredWood)
                                                .build())
                .build();
    }

    // Constructor helper method
    public Map<Resource, Integer> getResources(StateView state, Type... types)
    {
        UnitView townHall = getTownHall(state);
        Map<Resource, Integer> resources = new HashMap<>();
        for (Type t : types)
            for (ResourceView r : state.getResourceNodes(t))
            {
                Resource resource;
                resource = new Resource(r.getID(),
                                        Type.getResourceType(r.getType()),
                                        distanceToTownHall(townHall, r),
                                        r.getAmountRemaining());
                resources.put(resource, resource.getRemaining());
            }
        return resources;
    }

    // Constructor helper method
    private UnitView getTownHall(StateView state)
    {
        Optional<UnitView> townHall;
        townHall = state.getAllUnits()
                        .stream()
                        .filter(u -> u.getTemplateView()
                                      .getName()
                                      .equalsIgnoreCase("townhall"))
                        .findFirst();
        if (townHall.isPresent())
            return townHall.get();
        else
            throw new NoSuchElementException("No town hall exists!");
    }

    // Constructor helper method
    private double distanceToTownHall(UnitView townHall, ResourceView resource)
    {
        Position townHallLoc = Position.locate(townHall.getXPosition(),
                                               townHall.getYPosition());
        Position resourceLoc = Position.locate(resource.getXPosition(),
                                               resource.getYPosition());
        return townHallLoc.euclideanDistance(resourceLoc);
    }

    /**
     * Unlike in the first A* assignment there are many possible goal states.
     * As long as the wood and gold requirements are met the peasants can be
     * at any location and the capacities of the resource locations can be
     * anything. Use this function to check if the goal conditions are met
     * and return true if they are.
     *
     * @return true if the goal conditions are met in this instance of game
     * state.
     */
    public boolean isGoal()
    {
        return getResourceTracker().isGoalSatisfied();
    }

    /*
    All present Strips.StripsEnum at the beginning of GameState instantiation
    are the result of being set in the parent GameState. That is, upon
    considering possible SEPIA actions, the current GameState describes what
    is possible for the current state, as opposed to describing what will be
    possible for the proceeding children states.
     */

    /**
     * The branching factor of this search graph are much higher than the
     * planning. Generate all of the possible successor states and their
     * associated actions in this method.
     *
     * @return A list of the possible successor states and their associated
     * actions
     */
    public Set<GameState> generateChildren()
    {
        /*
        Conceptual switch statement:
            switch(StripsEnum):
                case DEPOSIT:
                    1 possible action
                case GATHER:
                    r possible actions (r = #(resources))
                case PRODUCE:
                    1 possible action

        Given p peasants and r resources there is, at most, p^r possible
        actions. This worst-case occurs when all peasants are scheduled to
        gather.

        Define the "initiative" principle as:
            If a peasant is tasked with an action then it always executes
            that action as soon as it is able.
         */
        Set<GameState> children = new HashSet<>();
        return recursiveChildren(this,
                                 new HashSet<>(),
                                 getUnitTracker(),
                                 children);
    }

    /*
    All Strips.StripsEnums that are present at the beginning of GameState
    instantiation are the result of being set in the parent GameState. That is,
    upon considering possible SEPIA actions, the current GameState describes
    what is possible for the current state, as opposed to describing what
    will be possible for the proceeding children states.
     */
    public Set<GameState> recursiveChildren(GameState current,
                                            Set<Entry<Unit, StripsEnum>> tracked,
                                            UnitTracker tracker,
                                            Set<GameState> children)
    {
        /*
        For each Unit, consider all possible actions it can do in the next
        state. For each of its actions, consider all other combinations of
        actions that other Units could make as well. Essentially, generate
        all possible combinations of valid StripsEnum for the current state.
         */
        // TODO How do we merge generation and STRIPS preconditions?
        //  preconditionsMet() and apply() could be generalized to apply a
        //  set of Strips Actions. The process could be as follows:
        //      -> recurse down to last untracked unit
        //      -> at this point, 1 full StripsMap has been generated that
        //         contains the set of actions that must be done to create
        //         the state. This is very much like DirectiveMap but does
        //         not have the downside of coupling.
        //      -> do this for all possible states.
        // TODO Make a Strips class for each of the StripsEnum so that cost,
        //  preconditions and apply can be done for each action. Very similar
        //  to the builder design pattern
        Set<Entry<Unit, StripsEnum>> notTracked = tracker.getItems().entrySet();
        notTracked.removeAll(tracked);
        for (Entry<Unit, StripsEnum> unit : notTracked)
        {
            for (StripsEnum next : StripsEnum.getValidNext(unit.getValue()))
            {
                GameState child = new GameState(current);
                child.getUnitTracker().validateAndTrack(unit.getKey(), next);
                children.add(child);
                UnitTracker childTracker = child.getUnitTracker();
                recursiveChildren(child, tracked, childTracker, children);
            }
            tracked.add(unit);
        }
        return children;
    }

    /**
     * Write your heuristic function here. Remember this must be admissible
     * for the properties of A* to hold. If you can come up with an easy way
     * of computing a consistent heuristic that is even better, but not
     * strictly necessary.
     * <p>
     * All this does is bias the agent against going over the resource goal.
     * Otherwise we only consider the sum of all the distances of previous
     * moves.
     *
     * @return The value estimated remaining cost to reach a goal state from
     * this state.
     */
    public double heuristic()
    {
        if (isGoal())
            return 0;
        return getResourceTracker().getDiffFromGoal();
    }

    // STRIPS action
    public void gather(Unit peasant, Resource resource, int amount)
    {
        getResourceTracker().adjustRemaining(resource, amount);
        getUnitTracker().validateAndTrack(peasant, GATHER);
    }

    // STRIPS action
    public void deposit(Unit peasant, ResourceType resourceType, int amount)
    {
        getResourceTracker().adjustCurrent(resourceType, amount);
        getUnitTracker().validateAndTrack(peasant, DEPOSIT);
    }

    // STRIPS action
    public void produce(Unit townHall, int goldCost, int woodCost)
    {
        getUnitTracker().createAndTrack(IDLE, IDLE, GATHER, DEPOSIT);
        getResourceTracker().adjustCurrent(GOLD, -goldCost);
        getResourceTracker().adjustCurrent(WOOD, -woodCost);
        getUnitTracker().validateAndTrack(townHall, PRODUCE);
    }

    /**
     * Write the function that computes the current cost to get to this node.
     * This is combined with your heuristic to
     * determine which actions/states are better to explore.
     *
     * @return The current cost to reach this goal
     */
    public double getCost()
    {
        return cost;
    }

    /**
     * This is necessary to use your state in the Java priority queue. See
     * the official priority queue and Comparable
     * interface documentation to learn how this function should work.
     *
     * @param gameState The other game state to compare
     * @return 1 if this state costs more than the other, 0 if equal, -1
     * otherwise
     */
    @Override
    public int compareTo(GameState gameState)
    {
        return Double.compare(getHeuristicCost(), gameState.getHeuristicCost());
    }

    /**
     * This will be necessary to use the GameState as a key in a Set or Map.
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
    // TODO Make sure this is efficient -- what is considered equal and
    //  hashed will effect number of GameStates to consider
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GameState gameState = (GameState) o;
        boolean equalResources =
                getResourceTracker().equals(gameState.getResourceTracker());
        boolean equalUnits =
                getUnitTracker().equals(gameState.getUnitTracker());
        return equalResources && equalUnits;
    }

    /**
     * This is necessary to use the GameState as a key in a HashSet or
     * HashMap. Remember that if two objects are equal they should hash to
     * the same value.
     *
     * @return An integer hashcode that is equal for equal states.
     */
    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        String gold = "gold=" + getResourceTracker().getCurrentAmount(GOLD);
        String wood = "wood=" + getResourceTracker().getCurrentAmount(WOOD);
        String peas = "nPeasants = " + (getUnitTracker().getItems().size() - 1);
        String cost = "cost=" + getCost();
        String heuristic = "heuristic=" + getHeuristicCost();
        return "GameState{" + cost + ", " + heuristic + ", " + gold + ", " + wood + ", " + peas + "}";
    }

    public UnitTracker getUnitTracker()
    {
        return unitTracker;
    }

    public ResourceTracker getResourceTracker()
    {
        return resourceTracker;
    }

    public void adjustCost(double cost)
    {
        setCost(getCost() + cost);
    }

    public void setCost(double cost)
    {
        this.cost = cost;
    }

    public double getHeuristicCost()
    {
        return heuristicCost;
    }

    public void setHeuristic()
    {
        setHeuristicCost(heuristic());
    }

    public void setHeuristicCost(double heuristicCost)
    {
        this.heuristicCost = heuristicCost;
    }

    public boolean considerBuildingPeasants()
    {
        return buildPeasants;
    }

    public GameState getCameFrom()
    {
        return cameFrom;
    }

    public static class UnitTrackerBuilder
    {
        private List<Unit> units;
        private List<StripsEnum> statuses;

        public UnitTrackerBuilder()
        {
            this.units = new ArrayList<>();
            this.statuses = new ArrayList<>();
        }

        public UnitTracker build()
        {
            return new UnitTracker(this);
        }

        public UnitTrackerBuilder units(List<Unit> units)
        {
            setUnits(units);
            List<StripsEnum> statuses = units.stream()
                                             .map(Unit::getInitialAction)
                                             .collect(Collectors.toList());
            setStatuses(statuses);
            return this;
        }

        public List<Unit> getUnits()
        {
            return units;
        }

        public void setUnits(List<Unit> units)
        {
            this.units = units;
        }

        public List<StripsEnum> getStatuses()
        {
            return statuses;
        }

        public void setStatuses(List<StripsEnum> statuses)
        {
            this.statuses = statuses;
        }
    }

    public static class UnitTracker extends Tracker<Unit, StripsEnum>
    {
        public UnitTracker(UnitTrackerBuilder builder)
        {
            super(builder.getUnits(), builder.getStatuses());
        }

        // Returns true if validation passed and unit was added
        public void createAndTrack(StripsEnum initialAction,
                                   StripsEnum... validActions)
        {
            Unit newUnit = new UnitBuilder().validActions(validActions)
                                            .initialAction(initialAction)
                                            .build();
            validateAndTrack(newUnit, initialAction);
        }

        // Returns true if validation passed and unit was added
        public void validateAndTrack(Unit unit, StripsEnum status)
        {
            if (unit.getValidActions().contains(status))
                track(unit, status);
        }
    }

    public static class UnitBuilder
    {
        private EnumSet<StripsEnum> validActions;
        private StripsEnum initialAction;
        private int goldCostToProduce;
        private int woodCostToProduce;

        public UnitBuilder()
        {
            this.validActions = EnumSet.noneOf(StripsEnum.class);
            this.initialAction = null;
            this.goldCostToProduce = 0;
            this.woodCostToProduce = 0;
        }

        public Unit build()
        {
            return new Unit(this);
        }

        public UnitBuilder validActions(StripsEnum... validActions)
        {
            EnumSet<StripsEnum> valid = EnumSet.noneOf(StripsEnum.class);
            valid.addAll(Arrays.asList(validActions));
            setValidActions(valid);
            return this;
        }

        public UnitBuilder initialAction(StripsEnum action)
        {
            setInitialAction(action);
            return this;
        }

        public UnitBuilder goldCostToProduce(int goldCost)
        {
            setGoldCostToProduce(goldCost);
            return this;
        }

        public UnitBuilder woodCostToProduce(int woodCost)
        {
            setWoodCostToProduce(woodCost);
            return this;
        }

        public EnumSet<StripsEnum> getValidActions()
        {
            return validActions;
        }

        private void setValidActions(EnumSet<StripsEnum> validActions)
        {
            this.validActions = validActions;
        }

        public StripsEnum getInitialAction()
        {
            return initialAction;
        }

        private void setInitialAction(StripsEnum initialAction)
        {
            this.initialAction = initialAction;
        }

        public int getGoldCostToProduce()
        {
            return goldCostToProduce;
        }

        public void setGoldCostToProduce(int costToProduce)
        {
            this.goldCostToProduce = costToProduce;
        }

        public int getWoodCostToProduce()
        {
            return woodCostToProduce;
        }

        public void setWoodCostToProduce(int woodCostToProduce)
        {
            this.woodCostToProduce = woodCostToProduce;
        }
    }

    // Handles auto generating id. Mapping GameState to SEPIA peasants
    // is handled in PEAgent.
    public static class Unit
    {
        private static int lastAssignedId = 0;
        private final int id;
        private final int goldCostToProduce;
        private final int woodCostToProduce;
        private final Set<StripsEnum> validActions;
        private final StripsEnum initialAction;

        public Unit(UnitBuilder builder)
        {
            this.id = getNewId();
            this.goldCostToProduce = builder.getGoldCostToProduce();
            this.woodCostToProduce = builder.getWoodCostToProduce();
            this.validActions = builder.getValidActions();
            this.initialAction = builder.getInitialAction();
        }

        private static int getNewId()
        {
            int newId = getLastAssignedId() + 1;
            adjustLastAssignedId(newId);
            return newId;
        }

        public int getId()
        {
            return id;
        }

        public int getGoldCostToProduce()
        {
            return goldCostToProduce;
        }

        public int getWoodCostToProduce()
        {
            return woodCostToProduce;
        }

        public Set<StripsEnum> getValidActions()
        {
            return validActions;
        }

        public StripsEnum getInitialAction()
        {
            return initialAction;
        }

        private static void adjustLastAssignedId(int adjust)
        {
            setLastAssignedId(getLastAssignedId() + adjust);
        }

        private static int getLastAssignedId()
        {
            return lastAssignedId;
        }

        public static void setLastAssignedId(int lastAssigned)
        {
            lastAssignedId = lastAssigned;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Unit unit = (Unit) o;
            return getId() == unit.getId();
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(getId());
        }
    }

    private static class ResourceTrackerBuilder
    {
        private Goal<Integer> goal;
        private Map<ResourceType, Integer> currentAmounts;
        private Map<Resource, Integer> resources;

        public ResourceTrackerBuilder()
        {
            this.goal = new GoalBuilder<Integer>().build();
            this.currentAmounts = new EnumMap<>(ResourceType.class);
        }

        public ResourceTrackerBuilder goal(Goal<Integer> goal)
        {
            setGoal(goal);
            return this;
        }

        public ResourceTracker build()
        {
            return new ResourceTracker(this);
        }

        public ResourceTrackerBuilder currentAmount(ResourceType type,
                                                    int amount)
        {
            getCurrentAmounts().put(type, amount);
            return this;
        }

        public ResourceTrackerBuilder resources(Map<Resource, Integer> resources)
        {
            setResources(resources);
            return this;
        }

        public Goal<Integer> getGoal()
        {
            return goal;
        }

        public void setGoal(Goal<Integer> goal)
        {
            this.goal = goal;
        }

        public Map<ResourceType, Integer> getCurrentAmounts()
        {
            return currentAmounts;
        }

        public Map<Resource, Integer> getResources()
        {
            return resources;
        }

        public void setResources(Map<Resource, Integer> resources)
        {
            this.resources = resources;
        }
    }

    // Integer = amount remaining
    public static class ResourceTracker extends Tracker<Resource, Integer>
    {
        private Goal<Integer> goal;
        private Map<ResourceType, Integer> currentAmounts;

        private ResourceTracker(ResourceTrackerBuilder builder)
        {
            super(builder.getResources());
            this.goal = builder.getGoal();
            this.currentAmounts = builder.getCurrentAmounts();
        }

        public boolean isGoalSatisfied()
        {
            Map<String, Integer> test = new HashMap<>();
            test.put("gold", getCurrentAmount(GOLD));
            test.put("wood", getCurrentAmount(WOOD));
            return getGoal().isSatisfied(test);
        }

        public int getDiffFromGoal()
        {
            int goalGold = getGoal().getCriteria().get("gold").getObjective();
            int goalWood = getGoal().getCriteria().get("wood").getObjective();
            int goldDiff = goalGold - getCurrentAmount(GOLD);
            int woodDiff = goalWood - getCurrentAmount(WOOD);
            return goldDiff + woodDiff;
        }

        public boolean isGoalExceeded()
        {
            Map<String, Integer> test = new HashMap<>();
            test.put("gold", getCurrentAmount(GOLD));
            test.put("wood", getCurrentAmount(WOOD));
            return getGoal().getCriteria()
                            .values()
                            .stream()
                            .anyMatch(c -> test.get(c.getId()) > c.getObjective());
        }

        public void adjustRemaining(Resource resource, int amount)
        {
            resource.adjustRemaining(amount);
            getItems().put(resource, resource.getRemaining());
        }

        public boolean hasEnough(ResourceType type, int amount)
        {
            return getCurrentAmount(type) >= amount;
        }

        public Goal<Integer> getGoal()
        {
            return goal;
        }

        public int getCurrentAmount(ResourceType type)
        {
            return getCurrentAmounts().get(type);
        }

        public Map<ResourceType, Integer> getCurrentAmounts()
        {
            return currentAmounts;
        }

        public void adjustCurrent(ResourceType type, int adjust)
        {
            getCurrentAmounts().merge(type, 0, (c, a) -> c - adjust);
        }
    }

    public static class Resource implements Comparable<Resource>
    {
        private final int id;
        private final ResourceType type;
        private final double distanceToTownHall;
        private int remaining;

        private Resource(int id,
                         ResourceType type,
                         double distanceToTownHall,
                         int remaining)
        {
            this.id = id;
            this.type = type;
            this.distanceToTownHall = distanceToTownHall;
            this.remaining = remaining;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Resource resource = (Resource) o;
            return getId() == resource.getId();
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(getId());
        }

        @Override
        public int compareTo(Resource r)
        {
            return Double.compare(getDistanceToTownHall(),
                                  r.getDistanceToTownHall());
        }

        public int getId()
        {
            return id;
        }

        public ResourceType getType()
        {
            return type;
        }

        public double getDistanceToTownHall()
        {
            return distanceToTownHall;
        }

        public int getRemaining()
        {
            return remaining;
        }

        // returns true if remaining <= 0 after adjustment
        public boolean adjustRemaining(int remaining)
        {
            setRemaining(getRemaining() + remaining);
            return getRemaining() <= 0;
        }

        public void setRemaining(int remaining)
        {
            this.remaining = remaining;
        }
    }

    // <T, S> = item type, status
    private static class Tracker<T, S>
    {
        private Map<T, S> items;

        public Tracker(Map<T, S> items)
        {
            this.items = items;
        }

        public Tracker(Iterable<T> items, Iterable<S> statuses)
        {
            Map<T, S> itemMap = new HashMap<>();
            Iterator<T> itemsIter = items.iterator();
            Iterator<S> statusIter = statuses.iterator();
            while (itemsIter.hasNext() && statusIter.hasNext())
                itemMap.put(itemsIter.next(), statusIter.next());
            this.items = itemMap;
        }

        public void track(T item, S statusFirst)
        {
            getItems().put(item, statusFirst);
        }

        public int getCount(S s)
        {
            return Collections.frequency(getItems().values(), s);
        }

        public Map<T, S> getItems()
        {
            return items;
        }
    }

    /**
     * A general goal comprised of Criterion of a certain type T.
     * <p>
     * The criteria are stored in a Map in which the names of the criteria
     * are used as keys and the Criterion object is stored as the value.
     * <p>
     * A Goal is satisfied when all of the Criterion values are satisfied.
     * Given a Map of test values, a Goal is assumed not satisfied if the Map
     * does not contain all of the Criterion. Extraneous test values can be
     * provided, but they are filtered out before testing. Each Criterion is
     * tested against the test value whose name matches the Criterion.
     *
     * @param <T> Type each Criterion implements.
     */
    private static class Goal<T> implements Satisfiable<T>
    {
        private Map<String, Criterion<T>> criteria;

        private Goal(GoalBuilder<T> builder)
        {
            this.criteria = builder.getCriteria();
        }

        @Override
        public boolean isSatisfied(Map<String, T> test)
        {
            if (hasMissingTestValues(test))
                return false;
            removeNonTestValues(test);
            for (Entry<String, T> entry : test.entrySet())
            {
                Map<String, T> single = new HashMap<>();
                single.put(entry.getKey(), entry.getValue());
                if (!getCriteria().get(entry.getKey()).isSatisfied(single))
                    return false;
            }
            return true;
        }

        private boolean hasMissingTestValues(Map<String, T> test)
        {
            return !test.keySet().containsAll(getCriteria().keySet());
        }

        private void removeNonTestValues(Map<String, T> test)
        {
            test.keySet().removeIf(t -> !getCriteria().containsKey(t));
        }

        public Map<String, Criterion<T>> getCriteria()
        {
            return criteria;
        }
    }

    /**
     * Implements the Builder design pattern for constructing Goal instances.
     *
     * @param <T> Type of Criterion that comprises the Goal.
     */
    private static class GoalBuilder<T>
    {
        private Map<String, Criterion<T>> criteria;

        public GoalBuilder()
        {
            this.criteria = new HashMap<>();
        }

        public GoalBuilder<T> specify(String id, T objective)
        {
            Criterion<T> criterion = new CriterionBuilder<T>().label(id)
                                                              .specify(objective)
                                                              .build();
            getCriteria().put(criterion.getId(), criterion);
            return this;
        }

        public Goal<T> build()
        {
            return new Goal<>(this);
        }

        private Map<String, Criterion<T>> getCriteria()
        {
            return this.criteria;
        }
    }

    /**
     * An abstraction of one part of a goal. A goal is comprised of one or
     * more criterion, each with a name and an objective value. Implementing
     * the Satisfiable interface, a Criterion is satisfied if the test value
     * supplied is equal to the objective value.
     * <p>
     *
     * @param <T> Type of the criterion objective.
     */
    private static class Criterion<T> implements Satisfiable<T>
    {
        private final String id;
        private T objective;

        public Criterion(String id, T objective)
        {
            this.id = id;
            this.objective = objective;
        }

        @Override
        public boolean isSatisfied(Map<String, T> test)
        {
            if (test.containsKey(getId()))
                return test.get(getId()).equals(getObjective());
            return false;
        }

        public String getId()
        {
            return id;
        }

        public T getObjective()
        {
            return objective;
        }
    }

    /**
     * Implements the Builder design pattern for constructing Criterion
     * instances.
     *
     * @param <T> Type of objective that defines the Criterion.
     */
    private static class CriterionBuilder<T>
    {
        private String id;
        private T objective;

        public CriterionBuilder()
        {
            this.id = "";
            this.objective = null;
        }

        public CriterionBuilder<T> label(String id)
        {
            setId(id);
            return this;
        }

        public CriterionBuilder<T> specify(T objective)
        {
            setObjective(objective);
            return this;
        }

        public Criterion<T> build()
        {
            return new Criterion<>(getId(), getObjective());
        }

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public T getObjective()
        {
            return objective;
        }

        public void setObjective(T objective)
        {
            this.objective = objective;
        }
    }
}

package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.StripsActionSet;
import edu.cwru.sepia.agent.planner.actions.StripsEnum;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private VisitTracker visitTracker;
    private double cost;
    private double heuristicCost;
    private boolean buildPeasants;
    private GameState cameFrom;
    private StripsActionSet creationActions;

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
        this.visitTracker = new VisitTracker();
        this.cost = 0;
        this.heuristicCost = requiredGold + requiredWood;
        this.buildPeasants = buildPeasants;
        this.cameFrom = null;
        this.creationActions = new StripsActionSet();
    }

    // Copy constructor. Used most of the time.
    private GameState(GameState gameState)
    {
        this.resourceTracker = gameState.getResourceTracker().copy();
        this.unitTracker = gameState.getUnitTracker().copy();
        this.cost = gameState.getCost();
        this.heuristicCost = gameState.getHeuristicCost();
        this.cameFrom = gameState;
    }

    public GameState copy()
    {
        return new GameState(this);
    }

    public UnitTracker createUnitTracker(StateView state)
    {
        List<UnitView> peasantUnits = state.getAllUnits();
        peasantUnits.removeIf(u -> u.equals(getTownHall(state)));
        int goldCost = peasantUnits.get(0).getTemplateView().getGoldCost();
        int woodCost = peasantUnits.get(0).getTemplateView().getWoodCost();
        UnitBuilder u = new UnitBuilder();
        List<Unit> units = new ArrayList<>(u.validActions(GATHER, DEPOSIT)
                                            .goldCostToProduce(goldCost)
                                            .woodCostToProduce(woodCost)
                                            .build(peasantUnits.size()));
        units.add(u.validActions(PRODUCE).build());
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
        List<UnitView> units = state.getAllUnits();
        units.removeIf(u -> !u.getTemplateView()
                              .getName()
                              .equalsIgnoreCase("townhall"));
        if (units.size() > 0)
            return units.get(0);
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
        Set<GameState> children = new HashSet<>();
        Deque<Entry<Unit, StripsEnum>> remaining;
        remaining = new ArrayDeque<>(getUnitTracker().getItems().entrySet());
        Entry<Unit, StripsEnum> first = remaining.pop();
        return recursiveChildren(this,
                                 remaining,
                                 first,
                                 children,
                                 new StripsActionSet());
    }

    /*
    All Strips.StripsEnums that are present at the beginning of GameState
    instantiation are the result of being set in the parent GameState. That is,
    upon considering possible SEPIA actions, the current GameState describes
    what is possible for the current state, as opposed to describing what
    will be possible for the proceeding children states.
     */
    public Set<GameState> recursiveChildren(GameState current,
                                            Deque<Entry<Unit, StripsEnum>> remaining,
                                            Entry<Unit, StripsEnum> currentUnit,
                                            Set<GameState> children,
                                            StripsActionSet actions)
    {
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
        getVisitTracker().track(peasant, resource);
    }

    // STRIPS action
    public void deposit(Unit peasant, Resource resource, int amount)
    {
        getVisitTracker().getItems().get(peasant);
        getResourceTracker().adjustCurrent(resource.getType(), amount);
        getUnitTracker().validateAndTrack(peasant, DEPOSIT);
    }

    // STRIPS action
    public void produce(Unit townHall, int goldCost, int woodCost)
    {
        getUnitTracker().createAndTrack(IDLE, GATHER, DEPOSIT);
        getResourceTracker().adjustCurrent(GOLD, -goldCost);
        getResourceTracker().adjustCurrent(WOOD, -woodCost);
        getUnitTracker().validateAndTrack(townHall, PRODUCE);
    }

    // STRIPS action
    public void idle(Unit unit)
    {
        getUnitTracker().validateAndTrack(unit, IDLE);
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

    public VisitTracker getVisitTracker()
    {
        return visitTracker;
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

    public StripsActionSet getCreationActions()
    {
        return creationActions;
    }

    public static class UnitTracker extends Tracker<Unit, StripsEnum>
    {
        private UnitTracker(UnitTrackerBuilder builder)
        {
            super(builder.getUnits(), builder.getStatuses());
        }

        private UnitTracker(UnitTracker tracker)
        {
            super(new HashMap<>(tracker.getItems()));
        }

        public UnitTracker copy()
        {
            return new UnitTracker(this);
        }

        public void createAndTrack(StripsEnum initialAction,
                                   StripsEnum... validActions)
        {
            Unit newUnit = new UnitBuilder().validActions(validActions)
                                            .initialAction(initialAction)
                                            .build();
            validateAndTrack(newUnit, initialAction);
        }

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
            if (getInitialAction() == null)
                setInitialAction(IDLE);
            getValidActions().add(getInitialAction());
            return new Unit(this);
        }

        public List<Unit> build(int nCopies)
        {
            return IntStream.range(0, nCopies)
                            .mapToObj(i -> build())
                            .collect(Collectors.toList());
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

        private Unit(UnitBuilder builder)
        {
            this.id = getNewId();
            this.goldCostToProduce = builder.getGoldCostToProduce();
            this.woodCostToProduce = builder.getWoodCostToProduce();
            this.validActions = builder.getValidActions();
            this.initialAction = builder.getInitialAction();
        }

        private Unit(Unit unit)
        {
            this.id = unit.getId();
            this.goldCostToProduce = unit.getGoldCostToProduce();
            this.woodCostToProduce = unit.getWoodCostToProduce();
            this.validActions = unit.getValidActions();
            this.initialAction = unit.getInitialAction();
        }

        public Unit copy()
        {
            return new Unit(this);
        }

        private static int getNewId()
        {
            int newId = getLastAssignedId() + 1;
            setLastAssignedId(newId);
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

        private static int getLastAssignedId()
        {
            return lastAssignedId;
        }

        private static void setLastAssignedId(int lastAssigned)
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

        @Override
        public String toString()
        {
            return String.format(
                    "Unit{id=%d, goldCostToProduce=%d, woodCostToProduce=%d, "
                            + "validActions=%s, initialAction=%s}",
                    id,
                    goldCostToProduce,
                    woodCostToProduce,
                    validActions,
                    initialAction);
        }
    }

    private static class VisitTracker extends Tracker<Unit, Resource>
    {
        public VisitTracker(Map<Unit, Resource> items)
        {
            super(items);
        }

        public VisitTracker(VisitTracker visitTracker)
        {
            super(new HashMap<>(visitTracker.getItems()));
        }

        public VisitTracker()
        {
            super(new HashMap<>());
        }

        public VisitTracker copy()
        {
            return new VisitTracker(this);
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

        private ResourceTracker(ResourceTracker resourceTracker)
        {
            super(resourceTracker.getItems());
            this.goal = resourceTracker.getGoal().copy();
            this.currentAmounts = resourceTracker.getCurrentAmounts();
        }

        public ResourceTracker copy()
        {
            return new ResourceTracker(this);
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

        private Resource(Resource resource)
        {
            this(resource.getId(),
                 resource.getType(),
                 resource.getDistanceToTownHall(),
                 resource.getRemaining());
        }

        public Resource copy()
        {
            return new Resource(this);
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

        public void adjustRemaining(int remaining)
        {
            setRemaining(getRemaining() + remaining);
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

        @SafeVarargs
        public final boolean containsAnyValue(S... s)
        {
            return Arrays.stream(s).anyMatch(this::containsValue);
        }

        public boolean containsValue(S s)
        {
            return getItems().containsValue(s);
        }

        public boolean containsKey(T t)
        {
            return getItems().containsKey(t);
        }

        public Map<T, S> getItems()
        {
            return items;
        }

        @Override
        public String toString()
        {
            return "Tracker{" + "items=" + items + '}';
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

        private Goal(Goal<T> goal)
        {
            Map<String, Criterion<T>> copiedCriteria = new HashMap<>();
            Set<Entry<String, Criterion<T>>> toCopy;
            toCopy = goal.getCriteria().entrySet();
            for (Entry<String, Criterion<T>> criterion : toCopy)
                copiedCriteria.put(criterion.getKey(),
                                   criterion.getValue().copy());
            this.criteria = copiedCriteria;
        }

        public Goal<T> copy()
        {
            return new Goal<T>(this);
        }

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

        public Criterion(CriterionBuilder<T> builder)
        {
            this.id = builder.getId();
            this.objective = builder.getObjective();
        }

        public Criterion(Criterion<T> criterion)
        {
            this.id = criterion.getId();
            this.objective = criterion.getObjective();
        }

        public Criterion<T> copy()
        {
            return new Criterion<>(this);
        }

        @Override
        public boolean isSatisfied(Map<String, T> test)
        {
            boolean satisfied = false;
            if (test.containsKey(getId()))
                satisfied = test.get(getId()).equals(getObjective());
            return satisfied;
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
            this.id = null;
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
            return new Criterion<T>(this);
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

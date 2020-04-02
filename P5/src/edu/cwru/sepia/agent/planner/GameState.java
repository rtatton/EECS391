package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.planner.actions.Strips;
import edu.cwru.sepia.agent.planner.actions.Strips.ActionEnum;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
    private EnumSet<ActionEnum> satisfied;
    private double cost;
    private double heuristicCost;
    private boolean buildPeasants;
    private GameState cameFrom;
    private DirectiveMap actions;

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
        this.unitTracker = new UnitTracker(getUnits(state), ActionEnum.IDLE);
        this.resourceTracker = makeResourceTracker(state,
                                                   playernum,
                                                   requiredGold,
                                                   requiredWood);
        this.satisfied = EnumSet.of(ActionEnum.EMPTY);
        this.cost = 0;
        this.heuristicCost = Double.POSITIVE_INFINITY;
        this.cameFrom = null;
        this.actions = new DirectiveMap();
        this.buildPeasants = buildPeasants;
    }

    // Copy constructor. Used most of the time.
    // TODO Add new fields
    public GameState(GameState gameState)
    {
        this.resourceTracker = gameState.getResourceTracker();
        this.unitTracker = gameState.getUnitTracker();
        this.actions = gameState.getActions();
        this.satisfied = EnumSet.of(ActionEnum.EMPTY);
        this.cost = gameState.getCost();
        this.heuristicCost = Double.POSITIVE_INFINITY;
        this.cameFrom = gameState;
        this.actions = null;
    }

    public List<Unit> getUnits(StateView state)
    {
        return state.getAllUnits()
                    .stream()
                    .map(Unit::new)
                    .collect(Collectors.toList());
    }

    public ResourceTracker makeResourceTracker(StateView state,
                                               int playerNum,
                                               int requiredGold,
                                               int requiredWood)
    {
        int currentGold = state.getResourceAmount(playerNum, GOLD);
        int currentWood = state.getResourceAmount(playerNum, WOOD);
        ResourceTracker resourceTracker = new ResourceTracker(requiredGold,
                                                              requiredWood,
                                                              currentGold,
                                                              currentWood);
        Map<Resource, Integer> resourceMap = new HashMap<>();
        for (Resource r : getResources(state, Type.GOLD_MINE, Type.TREE))
            resourceMap.put(r, r.getRemaining());
        resourceTracker.trackAll(resourceMap);
        return resourceTracker;
    }

    // Constructor helper method
    public List<Resource> getResources(StateView state, Type... types)
    {
        UnitView townHall = getTownHall(state);
        List<Resource> resources = new ArrayList<>();
        for (Type t : types)
            for (ResourceView r : state.getResourceNodes(t))
            {
                Resource resource;
                double dist = distanceToTownHall(townHall, r);
                int remaining = r.getAmountRemaining();
                Type type = r.getType();
                resource = new Resource(r.getID(), type, dist, remaining);
                resources.add(resource);
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
        return getResourceTracker().getGoal().isSatisfied();
    }

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
        List<DirectiveMap> possible;
        possible = possibleDirectives(this, getSatisfied());
        return possible.stream()
                       .map(DirectiveMap::getDelta)
                       .collect(Collectors.toSet());
    }

    // TODO Will need to modify
    /*
     *
     * Honestly this is the meat of getChildren.
     * Recursively determines which directives are satisfied and applies them
     * all to one DirectiveMap/GameState (we did this for scalability w
     * multiple peasants later)
     *
     */
    public List<DirectiveMap> possibleDirectives(GameState current,
                                                 EnumSet<ActionEnum> satisfiedActions)
    {
        List<DirectiveMap> possible = new ArrayList<>();
        // contains all our STRIPS logic
        current.evaluate();
        // this is essentially a union operation:
        // satisfiedActions U current.getSatisfied()
        satisfiedActions.removeIf(a -> !current.getSatisfied().contains(a));
        for (ActionEnum actionType : satisfiedActions)
        {
            DirectiveMap action = new DirectiveMap(actionType, current);
            action.add(possibleDirectives(action.getDelta(), satisfiedActions));
            possible.add(action);
        }
        return possible;
    }

    /**
     * Write your heuristic function here. Remember this must be admissible
     * for the properties of A* to hold. If you can come up with an easy way
     * of computing a consistent heuristic that is even better, but not
     * strictly necessary.
     * <p>
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
        if (getResourceTracker().exeededAnyRequired())
            return Double.POSITIVE_INFINITY;
        return getCost();
    }

    /*
     *
     *  GameState mutators : these methods handle all state changes
     *
     */
    // generalized application of "GATHER" directives to state,
    // returns the resource selected to gather from.
    public int gather(ActionEnum gatherAction, Unit peasant)
            throws SchedulingException
    {
        getUnitTracker().track(peasant);
        if (gatherAction == ActionEnum.GATHER_GOLD)
            return gatherGold(peasant);
        else if (gatherAction == ActionEnum.GATHER_WOOD)
            return gatherWood(peasant);
        System.out.println(
                "ERROR. Unit asked to gather unknown resource. gather(), " +
                        "PlannerAgent.");
        return -1;
    }

    // this method is only used to apply multiple actions onto one
    // GameState.
    public void gather(Unit unit, Resource resource) throws SchedulingException
    {
        getUnitTracker().track(unit);
        getResourceTracker(). if (getAvailableGold().contains(resource))
    {
        getReadyToGather().remove(unit);
        getAvailableGold().remove(resource);
        getPendingResources().put(unit, resource);
    }
        if (getAvailableWood().contains(resource))
        {
            getReadyToGather().remove(unit);
            getAvailableWood().remove(resource);
            getPendingResources().put(unit, resource);
        }
    }

    private Integer gatherGold(int unit)
    {
        Integer resourceId = getAvailableGold().poll();
        getPendingResources().put(unit, resourceId);
        return resourceId;
    }

    private Integer gatherWood(int unit)
    {
        Integer resourceId = getAvailableWood().poll();
        getPendingResources().put(unit, resourceId);
        return resourceId;
    }

    // generalized application of "DEPOSIT" directive
    // returns the resourceId that they initially collected from.
    public int deposit(int unit)
    {
        getReadyToGather().add(unit);
        if (getPendingResources().containsKey(unit))
            return depositResource(unit);
        System.out.println("ERROR. Unit asked to deposit when unit " +
                                   "unable" + ". " + "deposit(), " +
                                   "PlannerAgent.");
        return -1;
    }

    private int depositResource(int unit)
    {
        int resourceID = getPendingResources().get(unit);
        adjustResourceRemaining(resourceID, -100);
        if (hasResources(resourceID))
            // TODO availableResources()
            // Still want to sort by distance but need to differentiate
            // between gold and wood. So would it be better to have two
            // different data structures that stores wood and gold, or just
            // have one that efficiently filters by resource type?
            // If we make a resource class that contains distance and
            // remaining and id, then we could use a priority queue
            getAvailableWood().offerFirst(resourceID);
        getPendingResources().remove(unit);
        // TODO How to differentiate wood from gold?
        adjustCurrentWood(100);
        return resourceID;
    }

    // returns one peasant available to gather
    public Integer getPeasantToGather()
    {
        return getReadyToGather().poll();
    }

    // returns one peasant who needs to deposit
    public Integer getPeasantToDeposit()
    {
        return getReadyToDeposit().poll();
    }

    /*
     *
     *  predicates : questions you can ask about a GameState
     *
     */

    // checks if resource has any resources
    public boolean hasResources(int id)
    {
        return getResourceRemaining().get(id) > 0;
    }

    // TODO Set generalization would be lost if Resource class was created
    /*
     *
     *  arguably most important method of this class, encapsulates all
     * STRIPS logic!
     *
     */
    public void evaluate()
    {
        getSatisfied().remove(ActionEnum.EMPTY);
        if (!getPendingResources().isEmpty())
            getSatisfied().add(ActionEnum.DEPOSIT);
        if (!getReadyToGather().isEmpty() && !getAvailableGold().isEmpty())
            getSatisfied().add(ActionEnum.GATHER_GOLD);
        if (!getReadyToGather().isEmpty() && !getAvailableWood().isEmpty())
            getSatisfied().add(ActionEnum.GATHER_WOOD);
        if (getSatisfied().isEmpty())
            getSatisfied().add(ActionEnum.EMPTY);
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
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GameState comp = (GameState) o;
        if (getCurrentGold() != comp.getCurrentGold())
            return false;
        if (getCurrentWood() != comp.getCurrentWood())
            return false;
        if (numberOfPeasants() != comp.numberOfPeasants())
            return false;
        return getCost() == comp.getCost();
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
        return Objects.hash(getCurrentGold(),
                            getCurrentWood(),
                            getCost(),
                            numberOfPeasants());
    }

    @Override
    public String toString()
    {
        String currGold = "Current Gold: " + getCurrentGold();
        String currWood = "Current Wood: " + getCurrentWood();
        String numPeasants = "Number of Peasnts: " + numberOfPeasants();
        String numGathering =
                "Number Gathering: " + getPendingResources().size();
        String cost = "Cost: " + getCost();
        String heuristic = "Heuristic: " + getHeuristicCost();
        return currGold + ", " + currWood + ", " + numPeasants + ", " + numGathering + ", " + cost + ", " + heuristic;
    }

    public UnitTracker getUnitTracker()
    {
        return unitTracker;
    }

    public ResourceTracker getResourceTracker()
    {
        return resourceTracker;
    }

    public EnumSet<ActionEnum> getSatisfied()
    {
        return satisfied;
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

    public GameState getCameFrom()
    {
        return cameFrom;
    }

    public DirectiveMap getActions()
    {
        return actions;
    }

    public void setActions(DirectiveMap actions)
    {
        this.actions = actions;
    }
    //    private class Pool<W> implements Set<W>
    //    {
    //        private Set<W> workers;
    //
    //        public Pool(Set<W> workers)
    //        {
    //            this.workers = workers;
    //        }
    //
    //        public Pool(Collection<W> workers)
    //        {
    //            this.workers = new HashSet<>(workers);
    //        }
    //
    //        public Pool()
    //        {
    //            this(new HashSet<>());
    //        }
    //
    //        public Set<W> getWorkers()
    //        {
    //            return workers;
    //        }
    //
    //        public void setWorkers(Set<W> workers)
    //        {
    //            this.workers = workers;
    //        }
    //
    //        @Override
    //        public boolean equals(Object o)
    //        {
    //            if (this == o)
    //                return true;
    //            if (o == null || getClass() != o.getClass())
    //                return false;
    //            Pool<?> pool = (Pool<?>) o;
    //            return Objects.equals(getWorkers(), pool.getWorkers());
    //        }
    //
    //        @Override
    //        public int hashCode()
    //        {
    //            return Objects.hash(getWorkers());
    //        }
    //
    //        @Override
    //        public int size()
    //        {
    //            return getWorkers().size();
    //        }
    //
    //        @Override
    //        public boolean isEmpty()
    //        {
    //            return getWorkers().isEmpty();
    //        }
    //
    //        @Override
    //        public boolean contains(Object o)
    //        {
    //            return getWorkers().contains(o);
    //        }
    //
    //        @Override
    //        public Iterator<W> iterator()
    //        {
    //            return getWorkers().iterator();
    //        }
    //
    //        @Override
    //        public Object[] toArray()
    //        {
    //            return getWorkers().toArray();
    //        }
    //
    //        @Override
    //        public <T> T[] toArray(T[] a)
    //        {
    //            return getWorkers().toArray(a);
    //        }
    //
    //        @Override
    //        public boolean add(W w)
    //        {
    //            return getWorkers().add(w);
    //        }
    //
    //        @Override
    //        public boolean remove(Object o)
    //        {
    //            return getWorkers().remove(o);
    //        }
    //
    //        @Override
    //        public boolean containsAll(Collection<?> c)
    //        {
    //            return getWorkers().containsAll(c);
    //        }
    //
    //        @Override
    //        public boolean addAll(Collection<? extends W> c)
    //        {
    //            return getWorkers().addAll(c);
    //        }
    //
    //        @Override
    //        public boolean retainAll(Collection<?> c)
    //        {
    //            return getWorkers().retainAll(c);
    //        }
    //
    //        @Override
    //        public boolean removeAll(Collection<?> c)
    //        {
    //            return getWorkers().removeAll(c);
    //        }
    //
    //        @Override
    //        public void clear()
    //        {
    //            getWorkers().clear();
    //        }
    //    }

    public static class UnitTracker extends Tracker<Unit, ActionEnum>
    {
        public UnitTracker(Collection<Unit> units, ActionEnum defaultStatus)
        {
            super(units, defaultStatus);
        }
    }

    // Integer = amount remaining
    private static class ResourceTracker extends Tracker<Resource, Integer>
    {
        private Goal<Integer> goal;
        private int currentGold;
        private int currentWood;

        private ResourceTracker(int requiredGold,
                                int requiredWood,
                                int currentGold,
                                int currentWood)
        {
            this.goal = new GoalBuilder<Integer>().specify("gold", requiredGold)
                                                  .specify("wood", requiredWood)
                                                  .build();
            this.currentGold = currentGold;
            this.currentWood = currentWood;
        }

        public boolean isGoalSatisfied()
        {
            return false;
        }

        public Goal<Integer> getGoal()
        {
            return goal;
        }

        public int getCurrentGold()
        {
            return currentGold;
        }

        public void adjustCurrentGold(int adjust)
        {
            setCurrentGold(getCurrentGold() + adjust);
        }

        public void setCurrentGold(int currentGold)
        {
            this.currentGold = currentGold;
        }

        public int getCurrentWood()
        {
            return currentWood;
        }

        public void adjustCurrentWood(int adjust)
        {
            setCurrentWood(getCurrentWood() + adjust);
        }

        public void setCurrentWood(int currentWood)
        {
            this.currentWood = currentWood;
        }
    }

    // <T, S> = type of item, status
    private static class Tracker<T, S>
    {
        private Map<T, S> items;

        public Tracker(Map<T, S> items)
        {
            this.items = items;
        }

        public Tracker(Collection<T> items, S defaultStatus)
        {
            Map<T, S> itemMap = new HashMap<>();
            items.forEach(i -> itemMap.put(i, defaultStatus));
            this.items = itemMap;
        }

        public Tracker(Tracker<T, S> tracker)
        {
            this.items = tracker.getItems();
        }

        public Tracker()
        {
            this(new HashMap<>());
        }

        public void track(T item, S status)
        {
            getItems().put(item, status);
        }

        public void trackAll(Map<T, S> items)
        {
            getItems().putAll(items);
        }

        public Map<T, S> getItems()
        {
            return items;
        }
    }

    private static class Unit
    {
        private final int id;

        public Unit(int id)
        {
            this.id = id;
        }

        public Unit(UnitView unit)
        {
            this(unit.getID());
        }

        public int getId()
        {
            return id;
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

    private static class Resource implements Comparable<Resource>
    {
        private final int id;
        private final Type type;
        private final double distanceToTownHall;
        private int remaining;

        private Resource(int id,
                         Type type,
                         double distanceToTownHall,
                         int remaining)
        {
            this.id = id;
            this.type = type;
            this.distanceToTownHall = distanceToTownHall;
            this.remaining = remaining;
        }

        public Resource(ResourceView resource, double distanceToTownHall)
        {
            this(resource.getID(),
                 resource.getType(),
                 distanceToTownHall,
                 resource.getAmountRemaining());
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

        public Type getType()
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

    public class DirectiveMap extends Strips
    {
        private Map<Unit, Directive> directives;

        private DirectiveMap(Map<Unit, Directive> directives)
        {
            this.directives = directives;
        }

        public DirectiveMap()
        {
            this(new HashMap<>());
        }

        public Map<Unit, Directive> getMap()
        {
            return this.directives;
        }

        @Override
        public String toString()
        {
            StringBuilder me = new StringBuilder();
            me.append("Actions:\n");
            int i = 1;
            for (Directive action : getMap().values())
            {
                me.append(String.format("   %d.) %s\n", i, action.toString()));
                i++;
            }
            return me.toString();
        }
        // ******** REDESIGN ***************** //

        /*
         * this constructor handles directive implementation AND application
         * to GameState
         *
         */
        public DirectiveMap(ActionEnum type, GameState last)
        {
            this(new HashMap<>(), new GameState(last));
            int unitId;
            int resourceId;
            ActionType action;
            if (type == ActionEnum.EMPTY)
            {
                throw new NullPointerException("Error, EMPTY enum passed to " + "DirectiveMap constructor");
            }
            else if (type == ActionEnum.PRODUCE)
                return;
            if (type == ActionEnum.DEPOSIT)
            {
                unitId = getDelta().getPeasantToDeposit();
                resourceId = getDelta().deposit(unitId);
                action = ActionType.COMPOUNDDEPOSIT;
            }
            else
            {
                unitId = getDelta().getPeasantToGather();
                resourceId = getDelta().gather(type, unitId);
                action = ActionType.COMPOUNDGATHER;
            }
            getMap().put(unitId,
                         new Directive(unitId, resourceId, type, action));
            getDelta().setCost(getDelta().getResourceToTownHall()
                                         .get(resourceId));
            getDelta().setActions(new DirectiveMap(getMap(), null));
        }

        /*
         *
         * The methods below are used to merge DirectiveMaps
         *
         */

        // Highest level of addition
        public boolean add(List<DirectiveMap> actions)
        {
            double actionsCost;
            for (DirectiveMap actionMap : actions)
            {
                if (add(actionMap))
                    return false;
                actionsCost = actionMap.getDelta().getCost();
                if (actionsCost > getDelta().getCost())
                    getDelta().setCost(actionsCost);
            }
            return true;
        }

        // Middle level of addition
        public boolean add(DirectiveMap actions)
        {
            for (Directive action : actions.getMap().values())
                if (add(action))
                    return false;
            double actionsCost = actions.getDelta().getCost();
            if (actionsCost > getDelta().getCost())
                getDelta().setCost(actionsCost);
            return true;
        }

        // Unit level addition
        public boolean add(Directive action)
        {
            if (getMap().containsKey(action.getUnit()))
                return false;
            if (action.getActionType() == ActionType.COMPOUNDGATHER)
                getDelta().gather(action.getUnit(), action.getTarget());
            else if (action.getActionType() == ActionType.COMPOUNDDEPOSIT)
                getDelta().deposit(action.getUnit());
            else
            {
                System.out.println("ERROR MERGING ACTIONS. add(Directive)" +
                                           ", " + "GameState" +
                                           ".DirectiveMap");
                return false;
            }
            getMap().put(action.getUnit(), action);
            return true;
        }
    }

    /*
     *
     * Directive holds all information needed to translate a STRIPS action to
     *  a SEPIA action.
     *
     */
    public class Directive extends Strips
    {
        private int unit;
        private int target;
        private ActionEnum type;
        private ActionType action;

        private Directive(int unit,
                          int target,
                          ActionEnum type,
                          ActionType action)
        {
            this.unit = unit;
            this.target = target;
            this.type = type;
            this.action = action;
        }

        public Action createAction()
        {
            switch (getActionType())
            {
                case COMPOUNDGATHER:
                    return Action.createCompoundGather(getUnit(), getTarget());
                default:
                    return Action.createCompoundDeposit(getUnit(), getTarget());
            }
        }

        public int getUnit()
        {
            return unit;
        }

        public int getTarget()
        {
            return target;
        }

        public ActionEnum getType()
        {
            return type;
        }

        public ActionType getActionType()
        {
            return action;
        }

        @Override
        public String toString()
        {
            String me;
            switch (getType())
            {
                case DEPOSIT:
                    me = "Unit %d deposits resource to TownHall %d.";
                    break;
                case GATHER_GOLD:
                    me = "Unit %d mines gold from resource %d.";
                    break;
                case GATHER_WOOD:
                    me = "Unit %d chops wood from resource %d.";
                    break;
                case PRODUCE:
                    return "TownHall produces one peasant.";
                default:
                    return "ERROR: invalid directive!";
            }
            return String.format(me, getUnit(), getTarget());
        }
    }
}

package action;

import action.ActionGroup.SelectionType;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.*;
import java.util.stream.Collectors;

/**
 * User-facing end of the Action scheduling process. Each {@link ActionPlan}
 * has a list of {@link ActionGroup}s that organize {@link ActionMap}s, which
 * contain individual {@link Action}s. That is, an {@link ActionMap} organizes
 * zero or more {@link Action}s of the same type, {@link ActionGroup}
 * organizes {@link ActionMap}s by how they should be grouped when deciding
 * which {@link Action}s to execute,and the {@link ActionPlan} allows for
 * scheduling {@link ActionGroup}s.
 * <p>
 * The intended flow of operation is (1)
 * {@link ActionPlan#createActionPlan()} instantiates an empty
 * {@link ActionPlan}; (2) use any of the {@code schedule()} methods to imply
 * the {@link ActionGroup}s that should be made; (3) specify the
 * {@link UnitView}s to consider when creating the final plan; (4)
 * {@link #createPlan()} to create the final {@link ActionMap} to run.
 */
public class ActionPlan
{
    private List<ActionGroup> scheduledActions;
    private Set<Integer> unitPool;
    private ActionMap actionPlan;

    public ActionPlan()
    {
        this.actionPlan = new ActionMap();
        this.scheduledActions = new ArrayList<>();
        this.unitPool = new HashSet<>();
    }

    public static ActionPlan createActionPlan()
    {
        return new ActionPlan();
    }

    public void scheduleSequence(ActionMap... actionMaps)
    {
        addToSchedule(new ActionGroup(actionMaps));
    }

    public void scheduleRandom(ActionMap... actionMaps)
    {
        addToSchedule(new ActionGroup(SelectionType.RANDOM, actionMaps));
    }

    public void scheduleAscendingSize(ActionMap... actionMaps)
    {
        addToSchedule(new ActionGroup(
                SelectionType.ASCENDING_SIZE,
                actionMaps
        ));
    }

    public void scheduleDescendingSize(ActionMap... actionMaps)
    {
        addToSchedule(new ActionGroup(
                SelectionType.DESCENDING_SIZE,
                actionMaps
        ));
    }

    public void addToSchedule(ActionGroup... actionGroups)
    {
        getScheduledActions().addAll(Arrays.asList(actionGroups));
    }

    public void addToPlan(ActionMap actionMap)
    {
        getActionPlan().assign(actionMap);
    }

    public void addUnitsToPool(List<UnitView> units)
    {
        Set<Integer> toAdd =
                units.stream().map(UnitView::getID).collect(Collectors.toSet());

        getUnitIdPool().addAll(toAdd);
    }

    public void createPlan()
    {
        for (ActionGroup group : getScheduledActions())
        {
            for (ActionMap actionMap : group.select())
            {
                ActionMap filteredActionMap = ActionMap.createActionsMap();

                actionMap.getActionMap()
                        .stream()
                        .filter(this::isUnassigned)
                        .forEach(filteredActionMap::assign);
                addAndUpdateIfEffective(filteredActionMap);
            }
        }
    }

    public void addAndUpdateIfEffective(ActionMap actionMap)
    {
        if (actionMap.isEffective())
        {
            addToPlan(actionMap);
            removeAssignedUnits(actionMap.getAssignedUnitIds());
        }
    }

    public boolean isUnassigned(Action action)
    {
        return isUnassigned(action.getUnitId());
    }

    public boolean isUnassigned(Integer unitId)
    {
        return getUnitIdPool().contains(unitId);
    }

    public void removeAssignedUnits(List<Integer> assignedUnitIds)
    {
        getUnitIdPool().removeAll(assignedUnitIds);
    }

    public Set<Integer> getUnitIdPool()
    {
        return unitPool;
    }

    public ActionMap getActionPlan()
    {
        return actionPlan;
    }

    public HashMap<Integer, Action> getActionPlanMap()
    {
        return getActionPlan().getMap();
    }

    public List<ActionGroup> getScheduledActions()
    {
        return scheduledActions;
    }

    public String toString()
    {
        return "ActionPlan{" + "size=" + getActionPlan().size() + ", " + getActionPlanMap();
    }
}

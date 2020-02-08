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
    private Set<Integer> unitIdSet;
    private ActionMap actionPlan;

    private ActionPlan()
    {
        this.actionPlan = ActionMap.createActionsMap();
        this.scheduledActions = new ArrayList<>();
        this.unitIdSet = new HashSet<>();
    }

    public static ActionPlan createActionPlan()
    {
        return new ActionPlan();
    }

    public void scheduleSequence(ActionMap... actionMaps)
    {
        addToSchedule(ActionGroup.createActionGroup(actionMaps));
    }

    public void scheduleRandom(ActionMap... actionMaps)
    {
        addToSchedule(ActionGroup.createActionGroup(
                SelectionType.RANDOM,
                actionMaps
        ));
    }

    public void scheduleAscendingSize(ActionMap... actionMaps)
    {
        addToSchedule(ActionGroup.createActionGroup(
                SelectionType.ASCENDING_SIZE,
                actionMaps
        ));
    }

    public void scheduleDescendingSize(ActionMap... actionMaps)
    {
        addToSchedule(ActionGroup.createActionGroup(
                SelectionType.DESCENDING_SIZE,
                actionMaps
        ));
    }

    private void addToSchedule(ActionGroup... actionGroups)
    {
        getScheduledActions().addAll(Arrays.asList(actionGroups));
    }

    private void addToPlan(ActionMap actionMap)
    {
        getActionPlan().assign(actionMap);
    }

    public void addUnitsToSet(List<UnitView> units)
    {
        Set<Integer> toAdd = units
                .stream()
                .map(UnitView::getID)
                .collect(Collectors.toSet());

        getUnitIdSet().addAll(toAdd);
    }

    public void createPlan()
    {
        for (ActionGroup group : getScheduledActions())
        {
            ActionMap filteredActionMap = ActionMap.createActionsMap();

            for (ActionMap actionMap : group.select())
            {
                actionMap.getActions()
                        .stream()
                        .filter(this::isUnassigned)
                        .forEach(filteredActionMap::assign);

                addAndUpdateIfEffective(filteredActionMap);
            }
        }
    }

    private void addAndUpdateIfEffective(ActionMap actionMap)
    {
        if (actionMap.isEffective())
        {
            addToPlan(actionMap);
            removeAssignedUnits(actionMap.getAssignedUnitIds());
        }
    }

    private boolean isUnassigned(Action action)
    {
        return isUnassigned(action.getUnitId());
    }

    private boolean isUnassigned(Integer unitId)
    {
        return getUnitIdSet().contains(unitId);
    }

    private void removeAssignedUnits(List<Integer> assignedUnitIds)
    {
        getUnitIdSet().removeAll(assignedUnitIds);
    }

    public Set<Integer> getUnitIdSet()
    {
        return unitIdSet;
    }

    public ActionMap getActionPlan()
    {
        return actionPlan;
    }

    public HashMap<Integer, Action> getMap()
    {
        return getActionPlan().getMap();
    }

    public List<ActionGroup> getScheduledActions()
    {
        return scheduledActions;
    }

    public String toString()
    {
        return "ActionPlan{" + "size=" + getActionPlan().size() + ", " + getMap();
    }
}

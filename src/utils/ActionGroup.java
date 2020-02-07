package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Organizer for multiple {@link ActionMap}s. Each group is able to be selected from either at random, sequentially,
 * in ascending order (by length) or in descending order (by length). One or more {@link ActionMap}s are scheduled
 * together by the {@link ActionPlan}, which determines the groupings of Actions to be executed.
 *
 * @author Ryan Tatton
 */
public class ActionGroup
{
    private List<ActionMap> actionGroup;
    private SelectionType selectionType;

    public ActionGroup(SelectionType selectionType, List<ActionMap> actionGroup)
    {
        this.actionGroup = actionGroup;
        this.selectionType = selectionType;
    }

    public ActionGroup(SelectionType selectionType)
    {
        this(selectionType, new ArrayList<>());
    }

    public ActionGroup(ActionMap... actionMap)
    {
        this(SelectionType.SEQUENTIAL, new ArrayList<>(Arrays.asList(actionMap)));
    }

    public ActionGroup(SelectionType selectionType, ActionMap... actionMaps)
    {
        this(selectionType, Arrays.asList(actionMaps));
    }

    public ActionGroup(ActionGroup group)
    {
        this.actionGroup = new ArrayList<>(group.getActionGroup());
        this.selectionType = group.getSelectionType();
    }

    public ActionGroup()
    {
        this(SelectionType.SEQUENTIAL, new ArrayList<>());
    }

    public void addToGroup(ActionMap... actionMaps)
    {
            getActionGroup().addAll(Arrays.asList(actionMaps));
    }

    public ActionGroup copy()
    {
        return new ActionGroup(this);
    }

    public List<ActionMap> select()
    {
        switch (selectionType)
        {
            case RANDOM: return selectRandom();
            case ASCENDING_SIZE: return selectAscendingSize();
            case DESCENDING_SIZE: return selectDescendingSize();
            default: return selectSequential();
        }
    }

    public List<ActionMap> selectSequential()
    {
        return getActionGroup();
    }

    public List<ActionMap> selectRandom()
    {
        ArrayList<ActionMap> random = new ArrayList<>();

        if (getActionGroup().size() != 0)
        {
            int randomInt = Utils.randomInt(0, getActionGroup().size() - 1);
            random.add(getActionGroup().get(randomInt));
        }

        return random;
    }

    public List<ActionMap> selectAscendingSize()
    {
        return copy().sortAscending();
    }

    public List<ActionMap> selectDescendingSize()
    {
        return copy().sortDescending();
    }

    public List<ActionMap> sortDescending()
    {
        return getActionGroup()
                .stream()
                .sorted(Comparator.comparing(ActionMap::size).reversed())
                .collect(Collectors.toList());
    }

    public List<ActionMap> sortAscending()
    {
        return getActionGroup()
                .stream()
                .sorted(Comparator.comparing(ActionMap::size))
                .collect(Collectors.toList());
    }

    public boolean isSequential()
    {
        return getSelectionType() == SelectionType.SEQUENTIAL;
    }

    public boolean isRandom()
    {
        return getSelectionType() == SelectionType.RANDOM;
    }

    public boolean isAscendingSize()
    {
        return getSelectionType() == SelectionType.ASCENDING_SIZE;
    }

    public boolean isDescendingSize()
    {
        return getSelectionType() == SelectionType.DESCENDING_SIZE;
    }

    public List<ActionMap> getActionGroup()
    {
        return actionGroup;
    }

    public SelectionType getSelectionType()
    {
        return selectionType;
    }

    public enum SelectionType
    {
        ASCENDING_SIZE("Descending Size"),
        DESCENDING_SIZE("Descending Size"),
        RANDOM("Random"),
        SEQUENTIAL("Sequential");

        private final String selection;

        SelectionType(final String selection)
        {
            this.selection = selection;
        }

        public String toString()
        {
            return selection;
        }
    }
}

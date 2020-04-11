package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static edu.cwru.sepia.action.ActionType.*;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may
 * add your own methods and members.
 */
public class PEAgent extends Agent
{
    // The plan being executed
    private Stack<StripsAction> plan;
    // maps the real unit Ids to the plan's unit ids
    // when you're planning you won't know the true unit IDs that sepia
    // assigns. So you'll use placeholders (1, 2, 3).
    // this maps those placeholders to the actual unit IDs.
    private Map<Integer, Integer> peasantIdMap;
    private int townhallId;
    private int peasantTemplateId;

    public PEAgent(int playernum, Stack<StripsAction> plan)
    {
        super(playernum);
        this.peasantIdMap = new HashMap<>();
        this.plan = plan;
    }

    @Override
    public Map<Integer, Action> initialStep(StateView stateView,
                                            HistoryView historyView)
    {
        // gets the townhall ID and the peasant ID
        for (int unitId : stateView.getUnitIds(playernum))
        {
            UnitView unit = stateView.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            if (unitType.equals("townhall"))
                townhallId = unitId;
            else if (unitType.equals("peasant"))
                peasantIdMap.put(unitId, unitId);
        }
        // Gets the peasant template ID. This is used when building a new
        // peasant with the townhall
        for (TemplateView templateView : stateView.getTemplates(playernum))
            if (templateView.getName().toLowerCase().equals("peasant"))
            {
                peasantTemplateId = templateView.getID();
                break;
            }
        return middleStep(stateView, historyView);
    }

    /**
     * This is where you will read the provided plan and execute it. If your
     * plan is correct then when the plan is empty the scenario should end
     * with a victory. If the scenario keeps running after you run out of
     * actions to execute then either your plan is incorrect or your
     * execution of the plan has a bug.
     * <p>
     * For the compound actions you will need to check their progress and
     * wait until they are complete before issuing another action for that
     * unit. If you issue an action before the compound action is complete
     * then the peasant will stop what it was doing and begin executing the
     * new action.
     * <p>
     * To check a unit's progress on the action they were executing last
     * turn, you can use the following: historyView.getCommandFeedback
     * (playernum, stateView.getTurnNumber() - 1).get(unitID).getFeedback()
     * This returns an enum ActionFeedback. When the action is done, it will
     * return ActionFeedback.COMPLETED
     * <p>
     * Alternatively, you can see the feedback for each action being executed
     * during this turn. Here is a short example.
     * if (stateView.getTurnNumber() != 0) {
     * Map<Integer, ActionResult> actionResults = historyView
     * .getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
     * for (ActionResult result : actionResults.values()) {
     * <stuff>
     * }
     * }
     * Also remember to check your plan's preconditions before executing!
     */
    @Override
    public Map<Integer, Action> middleStep(StateView stateView,
                                           HistoryView historyView)
    {
        Map<Integer, Action> actionMap = new HashMap<>();
        int lastTurnNum = stateView.getTurnNumber() - 1;
        Map<Integer, ActionResult> lastTurn =
                historyView.getCommandFeedback(playernum, lastTurnNum);
        if (!readyForNextStep(lastTurn))
            return actionMap;
        StripsActionSet actionSet = (StripsActionSet) plan.pop();
        for (StripsAction action : actionSet.getActions())
            actionMap.putAll(createSepiaAction(action));
        return actionMap;
    }

    private int getSepiaUnitId(int gameStateUnitId)
    {
        return peasantIdMap.get(gameStateUnitId);
    }

    private boolean readyForNextStep(Map<Integer, ActionResult> lastTurn)
    {
        for (int peasant : peasantIdMap.keySet())
            if (lastTurn.get(peasantIdMap.get(peasant))
                        .getFeedback() != ActionFeedback.COMPLETED)
                return false;
        return true;
    }

    /**
     * Returns a SEPIA version of the specified Strips Action.
     * <p>
     * You can create a SEPIA deposit action with the following method
     * Action.createPrimitiveDeposit(int peasantId, Direction
     * townhallDirection)
     * <p>
     * You can create a SEPIA harvest action with the following method
     * Action.createPrimitiveGather(int peasantId, Direction
     * resourceDirection)
     * <p>
     * You can create a SEPIA build action with the following method
     * Action.createPrimitiveProduction(int townhallId, int
     * peasantTemplateId)
     * <p>
     * You can create a SEPIA move action with the following method
     * Action.createCompoundMove(int peasantId, int x, int y)
     * <p>
     * Hint:
     * peasantId could be found in peasantIdMap
     * <p>
     * these actions are stored in a mapping between the peasant unit ID
     * executing the action and the action you created.
     * <p>
     * Returns a SEPIA version of the specified Strips Action.
     *
     * @param action StripsAction
     * @return SEPIA representation of same action
     */
    private Map<Integer, Action> createSepiaAction(StripsAction action)
    {
        Action sepiaAction = null;
        Integer sepiaUnit = null;
        Map<Integer, Action> actionMap = new HashMap<>();
        if (action.getSepiaActionType().equals(COMPOUNDDEPOSIT))
        {
            Deposit deposit = (Deposit) action;
            sepiaUnit = getSepiaUnitId(deposit.getDepositor().getId());
            sepiaAction = action.getSepiaAction(sepiaUnit, townhallId);
        }
        if (action.getSepiaActionType().equals(COMPOUNDGATHER))
        {
            Gather gather = (Gather) action;
            int resource = gather.getGatherFrom().getId();
            sepiaUnit = getSepiaUnitId(gather.getGatherer().getId());
            sepiaAction = action.getSepiaAction(sepiaUnit, resource);
        }
        if (action.getSepiaActionType().equals(COMPOUNDPRODUCE))
        {
            Produce produce = (Produce) action;
            sepiaUnit = getSepiaUnitId(produce.getProducer().getId());
            sepiaAction = action.getSepiaAction(sepiaUnit, peasantTemplateId);
        }
        if (sepiaAction != null)
            actionMap.put(sepiaUnit, sepiaAction);
        return actionMap;
    }

    @Override
    public void terminalStep(StateView stateView, HistoryView historyView)
    {
    }

    @Override
    public void savePlayerData(OutputStream outputStream)
    {
    }

    @Override
    public void loadPlayerData(InputStream inputStream)
    {
    }
}

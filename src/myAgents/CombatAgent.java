package myAgents;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import utils.ActionsMap;
import utils.PlayerState;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class CombatAgent extends Agent
{
    private int enemyPlayerNum = 1;

    public CombatAgent(int playerNum, String[] args)
    {
        super(playerNum);


        if (args.length > 0)
            this.enemyPlayerNum = new Integer(args[0]);

        System.out.println("For Mother Russia!");
    }

    @Override
    public Map<Integer, Action> initialStep(StateView state, HistoryView history)
    {
        PlayerState player = new PlayerState(playernum, state, history);
        PlayerState enemy = new PlayerState(enemyPlayerNum, state, history);
        ActionsMap actions = new ActionsMap(player.getPlayerNum());

        List<Integer> myUnitIds = state.getUnitIds(playernum);
        List<Integer> enemyUnitIds = state.getUnitIds(enemyPlayerNum);

        if (PlayerState.noEnemiesExist(enemy))
            return actions.getMap();

        for (Integer myUnitId : myUnitIds)
            actions.assign(Action.createCompoundAttack(myUnitId, enemyUnitIds.get(0)));

        return actions.getMap();
    }

    @Override
    public Map<Integer, Action> middleStep(StateView state, HistoryView history)
    {
        PlayerState player = new PlayerState(playernum, state, history);
        PlayerState enemy = new PlayerState(enemyPlayerNum, state, history);
        ActionsMap actions = new ActionsMap(player.getPlayerNum());

        List<Integer> enemyUnitIds = state.getUnitIds(enemyPlayerNum);

        if (PlayerState.noEnemiesExist(enemy))
            return actions.getMap();

        int currentStep = state.getTurnNumber();

        for (ActionResult feedback : player.getCommandFeedback(currentStep - 1).values())
            if (feedback.getFeedback() != ActionFeedback.INCOMPLETE)
            {
                int unitId = feedback.getAction().getUnitId();
                actions.assign(Action.createCompoundAttack(unitId, enemyUnitIds.get(0)));
            }

        return actions.getMap();
    }

    @Override
    public void terminalStep(StateView stateView, HistoryView historyView)
    {
        System.out.println("Finished episode!");
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

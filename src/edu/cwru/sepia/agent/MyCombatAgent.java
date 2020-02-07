package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import ActionMap;
import playerView.PlayerView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class MyCombatAgent extends Agent
{
    private int enemyPlayerNum = 1;

    public MyCombatAgent(int playerNum, String[] args)
    {
        super(playerNum);

        if (args.length > 0)
            this.enemyPlayerNum = new Integer(args[0]);

        System.out.println("For Mother Russia!");
    }

    @Override
    public Map<Integer, Action> initialStep(StateView state, HistoryView history)
    {
        PlayerView player = new PlayerView(getPlayerNumber(), state, history);
        PlayerView enemy = new PlayerView(enemyPlayerNum, state, history);
        ActionMap actions = new ActionMap();

        List<Integer> myUnitIds = player.getUnitIds();
        List<Integer> enemyUnitIds = enemy.getUnitIds();

        if (PlayerView.noEnemiesExist(enemy))
            return actions.getMap();

        for (Integer myUnitId : myUnitIds)
            actions.assign(Action.createCompoundAttack(myUnitId, enemyUnitIds.get(0)));

        return actions.getMap();
    }

    @Override
    public Map<Integer, Action> middleStep(StateView state, HistoryView history)
    {
        PlayerView player = new PlayerView(playernum, state, history);
        PlayerView enemy = new PlayerView(enemyPlayerNum, state, history);
        ActionMap actions = new ActionMap();

        List<Integer> enemyUnitIds = state.getUnitIds(enemyPlayerNum);

        if (PlayerView.noEnemiesExist(enemy))
            return actions.getMap();

        int currentStep = state.getTurnNumber();

        player.getUnoccupiedUnits()
                .stream()
                .map(UnitView::getID)
                .forEach(unit -> actions.assign(Action.createCompoundAttack(unit, enemyUnitIds.get(0))));

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

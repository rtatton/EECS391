package action;

public class ActionItem
{
    private int maxNumExecutions;
    private double cost;
    private Priority priority;

    public ActionItem(int maxNumExecutions, double cost, Priority priority)
    {
        this.maxNumExecutions = maxNumExecutions;
        this.cost = cost;
        this.priority = priority;
    }

    public ActionItem(int maxNumExecutions)
    {
        this(maxNumExecutions, 0, Priority.LOW);
    }

    public ActionItem(int cost, Priority priority)
    {
        this(0, cost, priority);
    }

    public enum Priority
    {
        HIGH(3),
        MEDIUM(2),
        LOW(1);

        private final int priority;

        Priority(final int priority)
        {
            this.priority = priority;
        }

        public int getPriority()
        {
            return priority;
        }
    }
}

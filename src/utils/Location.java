package utils;

import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

/**
 * Utility class that provides a simpler interface to access the location of a {@link UnitView} or {@link ResourceView}.
 * @author Ryan Tatton
 */
public class Location
{
    private final int X;
    private final int Y;

    public Location(ResourceView resource)
    {
        this.X = resource.getXPosition();
        this.Y = resource.getYPosition();
    }

    public Location(UnitView unit)
    {
        this.X = unit.getXPosition();
        this.Y = unit.getYPosition();
    }

    @Override
    public String toString()
    {
        return "Location{" + "X=" + X + ", Y=" + Y + '}';
    }

    public int X()
    {
        return X;
    }

    public int Y()
    {
        return Y;
    }
}
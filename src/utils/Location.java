package utils;

import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import static edu.cwru.sepia.util.DistanceMetrics.euclideanDistance;

/**
 * Utility class that provides a simpler interface to access the location of a {@link UnitView} or {@link ResourceView}.
 *
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

    public Location(int X, int Y)
    {
        this.X = X;
        this.Y = Y;
    }

    /**
     * @param unit {@link UnitView} to locate.
     * @return A {@link Location} instance that contains the X and Y positions of the {@link UnitView}.
     */
    public static Location locate(UnitView unit)
    {
        return new Location(unit);
    }

    /**
     * @param resource {@link ResourceView} to locate.
     * @return A {@link Location} instance that contains the X and Y positions of the {@link ResourceView}.
     */
    public static Location locate(ResourceView resource)
    {
        return new Location(resource);
    }

    /**
     * @param unit1 UnitView to find.
     * @param unit2 UnitView to find.
     * @return Euclidean distance between two {@link UnitView}s.
     */
    public static double distBetween(UnitView unit1, UnitView unit2)
    {
        Location loc1 = locate(unit1);
        Location loc2 = locate(unit2);

        return euclideanDistance(loc2.getX(), loc2.getY(), loc1.getX(), loc1.getY());
    }

    /**
     * @param resource1 ResoureView to find.
     * @param resource2 ResourceView to find.
     * @return Euclidean distance between two {@link ResourceView}s.
     */
    public static double distBetween(ResourceView resource1, ResourceView resource2)
    {
        Location loc1 = locate(resource1);
        Location loc2 = locate(resource2);

        return euclideanDistance(loc2.getX(), loc2.getY(), loc1.getX(), loc1.getY());
    }

    /**
     * @param unit UnitView to find.
     * @param resource ResourceView to find.
     * @return Euclidean distance between a {@link UnitView} and {@link ResourceView}.
     */
    public static double distBetween(UnitView unit, ResourceView resource)
    {
        Location uLoc = locate(unit);
        Location rLoc = locate(resource);

        return euclideanDistance(rLoc.getX(), rLoc.getY(), uLoc.getX(), uLoc.getY());
    }

    public static Location randomLocation(StateView state)
    {
        int x = Utils.randomInt(0, state.getXExtent());
        int y = Utils.randomInt(0, state.getYExtent());
        Location location = new Location(x, y);

        return isOpen(state, location) ? location : randomLocation(state);
    }

    public static boolean isOpen(StateView state, Location center, int radius, boolean checkCenter) throws IllegalArgumentException
    {
        if (radius < 1)
            throw new IllegalArgumentException("Radius must be greater than 0.");
        if (!isOnBoard(state, center))
            throw new IllegalArgumentException("Location specified is not on the board.");

        for (int x = center.getX() - radius; x < center.getX() + radius; x++)
        {
            for (int y = center.getY() - radius; y < center.getY(); y++)
            {
                if (!isOpen(state, new Location(x, y)))
                    if (checkCenter && x != center.getX() && y != center.getY())
                        return false;
            }
        }
        return true;
    }

    public static boolean isOpen(StateView state, Location location)
    {
        boolean isResourceAt = state.isResourceAt(location.getX(), location.getY());
        boolean isUnitAt = state.isUnitAt(location.getX(), location.getY());
        return !(isResourceAt || isUnitAt);
    }

    public static boolean isOnBoard(StateView state, Location location)
    {
        return location.getX() <= state.getXExtent() && location.getY() <= state.getYExtent();
    }

    @Override
    public String toString()
    {
        return "Location{" + "X=" + X + ", Y=" + Y + '}';
    }

    public int getX()
    {
        return X;
    }

    public int getY()
    {
        return Y;
    }
}
package utils;

import java.util.Random;

/**
 * Miscellaneous helper functions.
 *
 * @author Ryan Tatton
 */
public class Utils
{
    /**
     * Calculate a random integer between a given lower and upper bound. If the bounds are equal, then the lower bound
     * bound is returned.
     * @param min Lower bound of the random integer.
     * @param max Upper bound of the random integer.
     * @return Random integer.
     * @throws IllegalArgumentException Thrown when the lower bound is greater than the upper bound.
     */
    public static int randomInt(int min, int max) throws IllegalArgumentException
    {
        if (min > max)
            throw new IllegalArgumentException(min + " must be less than " + max);

        else if (min == max)
            return min;

        return new Random().ints(min, max).findFirst().getAsInt();
    }
}

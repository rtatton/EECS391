package edu.cwru.sepia.agent.planner;

import java.util.Map;

public interface Satisfiable<T>
{
    boolean isSatisfied(Map<String, T> test);
}

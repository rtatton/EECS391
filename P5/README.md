# Programming 5: STRIPS Planning with Peasant Building

## Note
While much of the raw code is here, unfortunately, I did not have enough time to actually get it work. Given this fact, let me try to outline the logic as best I can so at least the intended flow of the program is expressed here.

## GameState

GameState is comprised of several inner classes that were written to be extendable for other implementations using SEPIA. Several of these inner classes are constructed using the builder design pattern which provides both flexibility and readability, with the tradeoff of increased lines of code. Each builder class is intuitively appended with "Builder" to distinguish it from the actual class instance.

### Goal and Criterion

To accomodate the aspect of collecting a certain amount of gold and wood, I designed a generalized notion of a Goal. That is, a Goal is comprised of a set of Criterion, collectively called Criteria. Each Criterion has a label and an objective to be reached. The Satisfiable interface has a single method, isSatisfied() which is implemented by both Criterion and Goal. Essentially, a Goal is satisfied when all of its Criterion are. However, it is assumed that when given a set of test values that does not contain all of the Criterion of the Goal, the Goal is not satisfied. For the concrete implementation of this project, an Integer Goal was instantiated with two Criterion, one of gold and the other for wood.

The Goal class is used by the ResourceTracker.

### Tracker

The Tracker class, as it is currently written, provides a semantic that sits on top of the Map interface. The idea is that the keys of the Tracker are the objects to be tracked, while the values are the statuses of each tracked object.

For this project, the two trackers are UnitTracker and ResourceTracker, which help organize a lot of changes that occur within and between GameStates.

### Resource and ResourceTracker

A wrapper for a SEPIA resource was written that contains the resource id, the amount remaining, the distance from the town hall, and the resource type.

All Resources are contained in an instance of ResourceTracker, which contains information about the current amount of each resource (i.e., gold and wood in the town hall) and the Goal to be achieved.

Convienence methods were written exclusively for ResourceTracker that helped with other aspects of GameSate.

### Unit and UnitTracker

A wrapper for a SEPIA unit was written that contains the unit id, the cost, in gold and wood, to produce, the set of StripsEnum actions it can perform, and the initial StripsEnum action it is initiated to have. Automated id setting was implemented by having a static lastAssignedId field which is incremented every time the Unit constructor is called. Having the set validActions allowed to prevent produce actions from being assigned to peasants, and from gather/deposit actions from being assigned to the town hall.

### VisitTracker

Because the statuses of Units are only managed by StripsEnums, there is no way of recalling which Resource a Unit visited to collect at the time of deposit. This information is tracked using a VisitTracker. The key is the Unit and the value is the last Resource that United gathered from. VisitTracker is updated in GameState.gather() and queried in GameState.deposit().

## STRIPS Actions

Applying STRIPS actions is handled at a couple of levels.

In GameState, there is a deposit(), gather, produce(), and idle() methods that correspond to each of the StripsEnum actions. These methods update the appropriate Trackers. The "perspective" that was taken in writing GameState is that a Unit's status in UnitTracker is what the Unit is currently doing. Thus, a precondition for depositing is that a UnitTracker contains a Unit that has the status DEPOSIT.

Each GameState STRIPS action method has a corresponding to StripsAction. The effects() method produces a Set of StripsActions that can result from this action. For example, the effect of a Gather StripsAction is Deposit. Conversely, the result of a Deposit action is either Idle or Gather. To account for any possible Gather (i.e. one for each Resource), n Gather StripsActions are created, where n is the number of Resources.

The cost of a GameState is determined by the set of StripsActions that where used to create it. Each StripsAction has a multiplicative cost factor. For deposit and gather, the magnitude is based on the distance of the Resource from the town hall. For produce, the factor is a constant 0.5. This incentivizes to produce since any GameState with a Produce StripsAction will reduce the entire cost of that GameState by a factor of 2. Likewise, Idle penalizes creating that GameState by having a cost factor of 2. This framework results in a simple, albeit naive creation of GameState children.

Each StripsAction class also has the method of converting itself into a SEPIA Action, given some external pieces of information, such as town hall id, and SEPIA peasant id.

## AStar

A fairly standard implementation of AStar is provided in PlannerAgent. The frontier is a PriorityQueue so that finding the next node to expand is efficient. Additionally, the explored set is hash-based so efficient filtering of previously generated GameStates can be checked before adding to the frontier.
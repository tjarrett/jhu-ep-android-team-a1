package jarrett.tim.rg;

import android.content.Context;

/**
 * Parent class for all the rope view classes
 * @author tjarrett
 */
public abstract class RopeView extends ThingView
{
    /**
     * The various states that our cleat can be in
     */
    public enum State implements ThingState
    {
        Slack,
        BurningSlack,
        Taut,
        BurningTaut,
        Ashes;
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public RopeView(Context context)
    {
        super(context);
        
        //Set the initial state
        setInitialState(State.Slack);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.Slack, Event.Heat, State.BurningSlack);
        addTransition(State.Slack, Event.Pull, State.Taut);
        
        addTransition(State.BurningSlack, Event.Water, State.Slack);
        addTransition(State.BurningSlack, Event.Heat, State.Ashes);
        
        addTransition(State.Taut, Event.Release, State.Slack);
        addTransition(State.Taut, Event.Pull, State.Taut);
        addTransition(State.Taut, Event.Heat, State.BurningTaut);
        
        addTransition(State.BurningTaut, Event.Water, State.Taut);
        addTransition(State.BurningTaut, Event.Heat, State.Ashes);
        
    }//end RopeView constructor    

}//end RopeView

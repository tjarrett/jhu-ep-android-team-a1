package jarrett.tim.rg;

import android.content.Context;

/**
 * Base view for our wire gadgets
 * @author tjarrett
 *
 */
public abstract class WireView extends ThingView
{
    /**
     * The various states that our this gadget can be in
     */
    public enum State implements ThingState
    {
        NoCurrent,
        Wet,
        HasCurrent,
        Toasted,
        Shorted;
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public WireView(Context context)
    {
        super(context);
        
        //Set the initial state
        setInitialState(State.NoCurrent);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.NoCurrent, Event.Water, State.Wet);
        addTransition(State.NoCurrent, Event.ElectricOn, State.HasCurrent);
        addTransition(State.NoCurrent, Event.Heat, State.Toasted);
        
        addTransition(State.Wet, Event.Heat, State.NoCurrent);
        addTransition(State.Wet, Event.ElectricOn, State.Shorted);
        
        addTransition(State.HasCurrent, Event.ElectricOff, State.NoCurrent);
        addTransition(State.HasCurrent, Event.ElectricOn, State.HasCurrent);
        addTransition(State.HasCurrent, Event.Heat, State.Toasted);        
        
    }//end WireView Constructor

}//end WireView

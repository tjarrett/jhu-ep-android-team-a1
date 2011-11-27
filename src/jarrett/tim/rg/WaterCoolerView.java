package jarrett.tim.rg;

import android.content.Context;

/**
 * Display water cooler gadget
 * @author tjarrett
 *
 */
public class WaterCoolerView extends ThingView
{
    /**
     * The various states that our this gadget can be in
     */
    public enum State implements ThingState
    {
        Empty,
        Full;
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public WaterCoolerView(Context context)
    {
        super(context);

        
        //Set the initial state
        setInitialState(State.Empty);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.Empty, Event.Pull, State.Full);
        addTransition(State.Full, Event.Release, State.Empty, Emit.WATER_DOWN);

        //Build our state-to-image map
        addStateImage(State.Empty, "water_cooler_empty");
        addStateImage(State.Full, "water_cooler_full");
        
    }//end WaterCooler Constructor

}//end WaterCooler

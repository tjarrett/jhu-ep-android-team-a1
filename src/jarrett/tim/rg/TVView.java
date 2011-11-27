package jarrett.tim.rg;

import android.content.Context;

/**
 * View representing our tv gadget
 * @author tjarrett
 *
 */
public class TVView extends ThingView
{
    /**
     * The various states that our this gadget can be in
     */
    public enum State implements ThingState
    {
        NoPower,
        Standby,
        Wet,
        PowerOn,
        Fried;
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public TVView(Context context)
    {
        super(context);
        
        //Set the initial state
        setInitialState(State.NoPower);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.NoPower, Event.ElectricOn, State.Standby);
        addTransition(State.NoPower, Event.Water, State.Wet);
        
        addTransition(State.Standby, Event.ElectricOff, State.NoPower);
        addTransition(State.Standby, Event.Turn, State.PowerOn, Emit.ALEX_ALL);
        addTransition(State.Standby, Event.Water, State.Fried, Emit.HEAT_UP);
        
        addTransition(State.Wet, Event.Heat, State.NoPower);
        addTransition(State.Wet, Event.ElectricOn, State.Fried, Emit.HEAT_UP);
        
        addTransition(State.PowerOn, Event.Pulse, State.PowerOn, Emit.ALEX_ALL);
        addTransition(State.PowerOn, Event.Water, State.Fried, Emit.HEAT_UP);
        

        //Build our state-to-image map
        addStateImage(State.NoPower, "tv_no_power");
        addStateImage(State.Standby, "tv_standby");
        addStateImage(State.Wet, "tv_wet");
        addStateImage(State.PowerOn, "tv_power_on");
        addStateImage(State.Fried, "tv_fried");
        
    }//end TVView Constructor

}//end TVView

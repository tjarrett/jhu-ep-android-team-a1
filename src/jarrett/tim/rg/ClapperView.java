package jarrett.tim.rg;

import com.javadude.rube.protocol.Event;

import android.content.Context;

/**
 * View representing our clapper gadget
 * @author tjarrett
 *
 */
public class ClapperView extends ThingView
{
    /**
     * The various states that our clapper can be in
     */
    public enum State implements ThingState
    {
        PowerOff,
        SwitchOff,
        SwitchOn,
        Wet,
        Fried;
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public ClapperView(Context context)
    {
        super(context);
        
        //Set the initial state
        setInitialState(State.PowerOff);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.PowerOff, Event.ElectricOn, State.SwitchOff);
        addTransition(State.PowerOff, Event.Heat, State.Fried, Emit.HEAT_UP);
        addTransition(State.PowerOff, Event.Water, State.Wet);
        
        addTransition(State.SwitchOff, Event.ElectricOff, State.PowerOff, Emit.ELECTRIC_OFF_RIGHT);
        addTransition(State.SwitchOff, Event.Water, State.Fried, Emit.HEAT_UP);
        addTransition(State.SwitchOff, Event.Heat, State.Fried, Emit.HEAT_UP);
        addTransition(State.SwitchOff, Event.Clap, State.SwitchOn, Emit.ELECTRIC_ON_RIGHT);
        
        addTransition(State.SwitchOn, Event.Clap, State.SwitchOff, Emit.ELECTRIC_OFF_RIGHT);
        addTransition(State.SwitchOn, Event.Water, State.Fried, Emit.HEAT_UP);
        addTransition(State.SwitchOn, Event.Heat, State.Fried, Emit.HEAT_UP);
        addTransition(State.SwitchOn, Event.ElectricOn, State.SwitchOn, Emit.ELECTRIC_ON_RIGHT);
        addTransition(State.SwitchOn, Event.ElectricOff, State.PowerOff, Emit.ELECTRIC_OFF_RIGHT);
        
        addTransition(State.Wet, Event.Heat, State.PowerOff, Emit.HEAT_UP);
        addTransition(State.Wet, Event.ElectricOn, State.Fried, Emit.HEAT_UP);
        
        
        //Build our state-to-image map
        addStateImage(State.PowerOff, "clapper_power_off");
        addStateImage(State.SwitchOff, "clapper_switch_off");
        addStateImage(State.SwitchOn, "clapper_switch_on");
        addStateImage(State.Wet, "clapper_wet");
        addStateImage(State.Fried, "clapper_fried");
        
    }//end ClapperView constructor

}//end ClapperView

package jarrett.tim.rg;

import com.javadude.rube.protocol.Event;

import android.content.Context;

/**
 * Used for displaying a Christmas Tree Thing
 * @author tjarrett
 *
 */
public class ChristmasTreeView extends ThingView
{
    /**
     * The various states that our tree can be in
     */
    public enum State implements ThingState
    {
        Unlit,
        Lit,
        Wet,
        Burning,
        Fried
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public ChristmasTreeView(Context context)
    {
        super(context);
        
        //Set the initial state
        setInitialState(State.Unlit);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.Unlit, Event.Water, State.Wet);
        addTransition(State.Unlit, Event.ElectricOn, State.Lit);
        addTransition(State.Unlit, Event.Heat, State.Burning, Emit.HEAT_UP);
        
        addTransition(State.Wet, Event.Heat, State.Unlit, Emit.HEAT_UP);
        addTransition(State.Wet, Event.ElectricOn, State.Fried, Emit.HEAT_UP);
        
        addTransition(State.Lit, Event.Water, State.Fried, Emit.HEAT_UP);
        addTransition(State.Lit, Event.ElectricOff, State.Unlit);
        addTransition(State.Lit, Event.Heat, State.Burning, Emit.HEAT_UP);

        addTransition(State.Burning, Event.Water, State.Unlit);
        addTransition(State.Burning, Event.Heat, State.Fried, Emit.HEAT_UP);
        addTransition(State.Burning, Event.Pulse, State.Fried, Emit.HEAT_UP);
        
        //Build our state-to-image map
        addStateImage(State.Unlit, "tree_unlit");
        addStateImage(State.Wet, "tree_wet");
        addStateImage(State.Lit, "tree_lit");
        addStateImage(State.Burning, "tree_burning");
        addStateImage(State.Fried, "tree_burnt");
        
    }//end constructor

}//end TreeView

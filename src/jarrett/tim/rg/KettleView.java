package jarrett.tim.rg;

import com.javadude.rube.protocol.Event;

import android.content.Context;

/**
 * View representing our kettle gadget
 * @author tjarrett
 *
 */
public class KettleView extends ThingView
{
    /**
     * The various states that our cleat can be in
     */
    public enum State implements ThingState
    {
        Empty,
        Hot,
        Full;
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public KettleView(Context context)
    {
        super(context);
        
        //Set the initial state
        setInitialState(State.Empty);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.Empty, Event.Heat, State.Hot);
        addTransition(State.Empty, Event.Water, State.Full);
        
        addTransition(State.Hot, Event.Water, State.Full, Emit.STEAM_RIGHT);
        
        addTransition(State.Full, Event.Steam, State.Full, Emit.STEAM_RIGHT);
        addTransition(State.Full, Event.Heat, State.Full, Emit.STEAM_RIGHT);

        //Build our state-to-image map
        addStateImage(State.Empty, "kettle_empty");
        addStateImage(State.Full, "kettle_full");
        addStateImage(State.Hot, "kettle_hot");
        
    }//end KettleView constructor

}//end KettleView

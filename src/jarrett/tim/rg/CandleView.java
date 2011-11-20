package jarrett.tim.rg;

import android.content.Context;

/**
 * View representing our candle gadget
 * @author tjarrett
 *
 */
public class CandleView extends ThingView
{    
    /**
     * The various states that our monkey can be in
     */
    public enum State implements ThingState
    {
        Unlit,
        Burning;
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public CandleView(Context context)
    {
        super(context);
        
        //Set the initial state
        setInitialState(State.Unlit);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.Unlit, Event.Start, State.Burning, Emit.HEAT_UP);
        addTransition(State.Unlit, Event.Heat, State.Burning, Emit.HEAT_UP);
        addTransition(State.Burning, Event.Pulse, State.Burning, Emit.HEAT_UP);
        addTransition(State.Burning, Event.Water, State.Unlit);
        
        //Build our state-to-image map
        addStateImage(State.Unlit, "candle_unlit");        
        addStateImage(State.Burning, "candle_burning");

    }//end CandleView

}//end CandleView

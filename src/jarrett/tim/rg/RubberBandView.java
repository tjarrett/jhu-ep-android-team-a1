package jarrett.tim.rg;

import android.content.Context;

/**
 * View for our rubber band gadget 
 * @author tjarrett
 *
 */
public class RubberBandView extends ThingView
{
    /**
     * The various states this gadget can be in
     */
    public enum State implements ThingState
    {
        Intact,
        Melted;
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public RubberBandView(Context context)
    {
        super(context);

        //Set the initial state
        setInitialState(State.Intact);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.Intact, Event.Turn, State.Intact);
        addTransition(State.Intact, Event.Heat, State.Melted);

        //Build our state-to-image map
        addStateImage(State.Intact, "rubberband_intact");
        addStateImage(State.Melted, "rubberband_melted");
        
    }//end RubberBandView constructor

}//end RubberBandView 

package jarrett.tim.rg;

import com.javadude.rube.protocol.Event;

import android.content.Context;

/**
 * Display cleat gadget
 * @author tjarrett
 *
 */
public class CleatView extends ThingView
{
    /**
     * The various states that our this gadget can be in
     */
    public enum State implements ThingState
    {
        Exists;
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public CleatView(Context context)
    {
        super(context);
        
        //Set the initial state
        setInitialState(State.Exists);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.Exists, Event.Pull, State.Exists, Emit.PULL_LEFT);

        //Build our state-to-image map
        addStateImage(State.Exists, "cleat_exists");
        
    }//end CleatView constructor

}//end CleatView

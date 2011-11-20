package jarrett.tim.rg;

import jarrett.tim.rg.CleatView.State;
import android.content.Context;

/**
 * View representing our electric outlet gadget
 * @author tjarrett
 *
 */
public class ElectricOutletView extends ThingView
{
    /**
     * The various states that our cleat can be in
     */
    public enum State implements ThingState
    {
        On,
        Shocked,
        Burnt;
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public ElectricOutletView(Context context)
    {
        super(context);
        
        //Set the initial state
        setInitialState(State.On);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.On, Event.Water, State.Shocked);
        addTransition(State.On, Event.Heat, State.Burnt);
        addTransition(State.On, Event.Pulse, State.On);

        //Build our state-to-image map
        addStateImage(State.On, "electric_outlet_on");
        addStateImage(State.Shocked, "electric_outlet_shocked");
        addStateImage(State.Burnt, "electric_outlet_burnt");
        
    }//end ElectricOutletView constructor

}//end ElectricOUtletView


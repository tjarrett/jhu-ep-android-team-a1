package jarrett.tim.rg;

import android.content.Context;

/**
 * Class representing the state of the monkey that we hope to draw on the screen
 * @author tjarrett
 */
public class MonkeyView extends ThingView
{
    
    /**
     * The various states that our monkey can be in
     */
    public enum State implements ThingState
    {
        Bored,
        Wet,
        Burning,
        Ashes,
        ClappingWide,
        ClappingClosed,
        WetClappingWide,
        WetClappingClosed;
        
    }//end State enum
    
    /**
     * Constructor
     */
    public MonkeyView(Context context)
    {
        super(context);
        
        //Set the initial state
        setInitialState(State.Bored);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.Bored, Event.Water, State.Wet);
        addTransition(State.Bored, Event.Heat, State.Burning);
        addTransition(State.Bored, Event.Alex, State.ClappingWide);
        
        addTransition(State.Wet, Event.Alex, State.WetClappingWide);
        addTransition(State.Wet, Event.Heat, State.Bored);
        
        addTransition(State.WetClappingWide, Event.Heat, State.ClappingClosed);
        addTransition(State.WetClappingWide, Event.Alex, State.WetClappingClosed); //as I understood the first assignment
        addTransition(State.WetClappingWide, Event.Pulse, State.WetClappingClosed);//what the given state diagram indicates

        addTransition(State.WetClappingClosed, Event.Heat, State.ClappingWide);
        addTransition(State.WetClappingClosed, Event.Alex, State.WetClappingWide); //as I understood the first assignment
        addTransition(State.WetClappingClosed, Event.Pulse, State.WetClappingWide);//what the given state diagram indicates

        addTransition(State.ClappingWide, Event.Heat, State.Burning);
        addTransition(State.ClappingWide, Event.Water, State.WetClappingClosed);
        addTransition(State.ClappingWide, Event.Alex, State.ClappingClosed); //as I understood the first assignment
        addTransition(State.ClappingWide, Event.Pulse, State.ClappingClosed); //what the given state diagram indicates

        addTransition(State.ClappingClosed, Event.Heat, State.Burning);
        addTransition(State.ClappingClosed, Event.Water, State.WetClappingWide);
        addTransition(State.ClappingClosed, Event.Alex, State.ClappingWide); //as I understood the first assignment
        addTransition(State.ClappingClosed, Event.Pulse, State.ClappingWide); //what the given state diagram indicates

        addTransition(State.Burning, Event.Heat, State.Ashes);
        addTransition(State.Burning, Event.Pulse, State.Ashes);
        addTransition(State.Burning, Event.Water, State.Bored); //from first assignment -- not in the state diagram given for the second assignment
        
        //Build our state-to-image map
        addStateImage(State.Bored, "monkey_bored");
        addStateImage(State.Wet, "monkey_wet");
        addStateImage(State.Burning, "monkey_burning");
        addStateImage(State.Ashes, "monkey_ashes");
        addStateImage(State.ClappingWide, "monkey_clapping_wide");
        addStateImage(State.ClappingClosed, "monkey_clapping_closed");
        addStateImage(State.WetClappingWide, "monkey_wet_clapping_wide");
        addStateImage(State.WetClappingClosed, "monkey_wet_clapping_closed");
        
    }//end Monkey

}//end Monkey

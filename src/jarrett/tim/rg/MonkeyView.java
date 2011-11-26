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
        addTransition(State.Bored, Event.Heat, State.Burning, Emit.HEAT_UP);
        addTransition(State.Bored, Event.Alex, State.ClappingWide, Emit.CLAP_ALL);
        
        addTransition(State.Wet, Event.Alex, State.WetClappingWide, Emit.CLAP_ALL);
        addTransition(State.Wet, Event.Heat, State.Bored);
        
        addTransition(State.WetClappingWide, Event.Heat, State.ClappingClosed, Emit.CLAP_ALL);
        addTransition(State.WetClappingWide, Event.Alex, State.WetClappingClosed, Emit.CLAP_ALL); //as I understood the first assignment
        addTransition(State.WetClappingWide, Event.Pulse, State.WetClappingClosed, Emit.CLAP_ALL);//what the given state diagram indicates

        addTransition(State.WetClappingClosed, Event.Heat, State.ClappingWide, Emit.CLAP_ALL);
        addTransition(State.WetClappingClosed, Event.Alex, State.WetClappingWide, Emit.CLAP_ALL); //as I understood the first assignment
        addTransition(State.WetClappingClosed, Event.Pulse, State.WetClappingWide, Emit.CLAP_ALL);//what the given state diagram indicates

        addTransition(State.ClappingWide, Event.Heat, State.Burning, Emit.HEAT_UP);
        addTransition(State.ClappingWide, Event.Water, State.WetClappingClosed, Emit.CLAP_ALL);
        addTransition(State.ClappingWide, Event.Alex, State.ClappingClosed, Emit.CLAP_ALL); //as I understood the first assignment
        addTransition(State.ClappingWide, Event.Pulse, State.ClappingClosed, Emit.CLAP_ALL); //what the given state diagram indicates

        addTransition(State.ClappingClosed, Event.Heat, State.Burning, Emit.HEAT_UP);
        addTransition(State.ClappingClosed, Event.Water, State.WetClappingWide, Emit.CLAP_ALL);
        addTransition(State.ClappingClosed, Event.Alex, State.ClappingWide, Emit.CLAP_ALL); //as I understood the first assignment
        addTransition(State.ClappingClosed, Event.Pulse, State.ClappingWide, Emit.CLAP_ALL); //what the given state diagram indicates

        addTransition(State.Burning, Event.Heat, State.Ashes, Emit.HEAT_UP);
        addTransition(State.Burning, Event.Pulse, State.Ashes, Emit.HEAT_UP);
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

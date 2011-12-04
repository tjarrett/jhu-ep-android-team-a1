package jarrett.tim.rg;

import com.javadude.rube.protocol.Event;

import android.content.Context;

/**
 * Base view for our wire gadgets
 * @author tjarrett
 *
 */
public abstract class WireView extends ThingView
{
    /**
     * The various states that our this gadget can be in
     */
    public enum State implements ThingState
    {
        NoCurrent,
        Wet,
        HasCurrent,
        Toasted,
        Shorted;
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public WireView(Context context)
    {
        super(context);
        
        //Set the initial state
        setInitialState(State.NoCurrent);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.NoCurrent, Event.Water, State.Wet);
        addTransition(State.NoCurrent, Event.ElectricOn, State.HasCurrent, Emit.ELECTRIC_ON_OPPOSITE);
        addTransition(State.NoCurrent, Event.Heat, State.Toasted, Emit.HEAT_UP);
        
        addTransition(State.Wet, Event.Heat, State.NoCurrent);
        addTransition(State.Wet, Event.ElectricOn, State.Shorted, Emit.HEAT_UP);
        
        addTransition(State.HasCurrent, Event.ElectricOff, State.NoCurrent, Emit.ELECTRIC_OFF_OPPOSITE);
        addTransition(State.HasCurrent, Event.ElectricOn, State.HasCurrent, Emit.ELECTRIC_ON_OPPOSITE);
        addTransition(State.HasCurrent, Event.Heat, State.Toasted, Emit.HEAT_UP);        
        
    }//end WireView Constructor
    
    /**
     * 
     */
	@Override
	public void receiveEvent(String event) 
	{
        //Translate the event
        EventMessage em;
        
        try {
            em = EventMessage.parse(event);
            
        } catch ( Exception e ) {
            //Couldn't parse the event... let our parent worry about it...
            super.receiveEvent(event);            
            return;
            
        }
        
        //Still here? Then we successfully parsed the event -- if it's not electric on or electric off, let the parent deal with it        
        if ( !em.getEvent().equals(Event.ElectricOn) && !em.getEvent().equals(Event.ElectricOff) ) {
        	
        	super.receiveEvent(event);
        	return;
        	
        }
		
        //Still here? Then the event is either electric on or electric off. It has to come in one end and go out the other end...
        if ( getOppositeEnd(em.getDirection()) == null ) {
        	//If this returns null, then the direction passed in doesn't match one of the two ends... so the message shouldn't effect us because 
        	//it came from a direction that we are not listening for...
        	return;
        	
        }
        
        //Still here? Then either ElectricOn or ElectricOff came in an appropriate end... Pass it up the chain...
        super.receiveEvent(event);        
        
	}//end receiveEvent

}//end WireView

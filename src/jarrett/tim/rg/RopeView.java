package jarrett.tim.rg;

import com.javadude.rube.protocol.Event;

import android.content.Context;

/**
 * Parent class for all the rope view classes
 * @author tjarrett
 */
public abstract class RopeView extends ThingView
{
    /**
     * The various states that our cleat can be in
     */
    public enum State implements ThingState
    {
        Slack,
        BurningSlack,
        Taut,
        BurningTaut,
        Ashes;
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public RopeView(Context context)
    {
        super(context);
        
        //Set the initial state
        setInitialState(State.Slack);
        reset();
        
        //Add in all the appropriate transitions...
        addTransition(State.Slack, Event.Heat, State.BurningSlack, Emit.HEAT_UP);
        addTransition(State.Slack, Event.Pull, State.Taut, Emit.PULL_OPPOSITE);
        
        addTransition(State.BurningSlack, Event.Water, State.Slack);
        addTransition(State.BurningSlack, Event.Heat, State.Ashes, Emit.HEAT_UP);
        
        addTransition(State.Taut, Event.Release, State.Slack, Emit.RELEASE_OPPOSITE);
        addTransition(State.Taut, Event.Pull, State.Taut, Emit.PULL_OPPOSITE);
        addTransition(State.Taut, Event.Heat, State.BurningTaut, Emit.HEAT_UP);
        
        addTransition(State.BurningTaut, Event.Water, State.Taut);
        addTransition(State.BurningTaut, Event.Heat, State.Ashes, Emit.RELEASE_BOTH);
        
    }//end RopeView constructor   
    
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
        	e.printStackTrace();
            super.receiveEvent(event);            
            return;
            
        }
        
        //Still here? Then we successfully parsed the event -- if it's not Pull or Release, let the parent deal with it        
        if ( !em.getEvent().equals(Event.Pull) && !em.getEvent().equals(Event.Release) ) {
        	
        	super.receiveEvent(event);
        	return;
        	
        }
        
        //Flip the direction on the incoming event to see if we have a match...

        
        
        //Still here? Then the event is either Pull or Release. It has to come in one end and go out the other end...
        if ( getOppositeEnd(getOppositeDirection(em.getDirection())) == null ) {
        	//If this returns null, then the direction passed in doesn't match one of the two ends... so the message shouldn't effect us because 
        	//it came from a direction that we are not listening for...
        	return;
        	
        }
        
        //Still here? Then either Pull or Release came in an appropriate end... Pass it up the chain...
        super.receiveEvent(event);        
        
	}//end receiveEvent

}//end RopeView

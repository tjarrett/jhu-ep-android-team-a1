package jarrett.tim.rg;

import com.javadude.rube.protocol.Direction;
import com.javadude.rube.protocol.Event;

import android.util.Log;

/**
 * Class for parsing evts sent around via bluetooth and breaking it into it's component parts
 * @author tjarrett
 *
 */
public class EventMessage
{
    /**
     * The event
     */
    private Event event = null;
    
    /**
     * The direction
     */
    private Direction direction = null;
    
    /**
     * Constructor
     * @param evt   The event being sent in -- looks something like: 0,1|Heat|UP
     */
    private EventMessage()
    {        

        
    }//end Constructor

    /**
     * Get the event
     * @return the event
     */
    public Event getEvent()
    {
        return event;
        
    }//end getEvent

    /**
     * Returns the direction as a string
     * @return the direction
     */
    public Direction getDirection()
    {
        return direction;
        
    }//end getDirection
    
    /**
     * Parse the given string (which should be formatted like: 0,1|Heat|UP) and return an EventMessage. 
     * @param evt
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    static public EventMessage parse(String evt) throws NullPointerException, IllegalArgumentException
    {
        //Was getting nulls passed in for some reason... handle that
        if ( evt == null ) {
            throw new NullPointerException();
            
        }
        
        //Split on |
        String[] bits = evt.split("\\|");
        
        EventMessage evm = new EventMessage();
        
        //Sometimes there won't be a direction... in case there is, capture it
        if ( bits.length == 3 ) {
            evm.event = Event.valueOf(bits[1]);
            evm.direction = Direction.valueOf(bits[2]);
            return evm;
        }
        
        //If it's reset... 
        if ( evt.toLowerCase().indexOf("reset") != -1 ) {
            evm.event = Event.Reset;
            return evm;
            
        }
        
        //If it's register
        if ( evt.toLowerCase().indexOf("register") != -1 ) {
            evm.event = Event.Register;
            return evm;
        }
        
        //Otherwise error
        Log.d(ActivityMain.DEBUG, "Malformed input received: " + evt);
        throw new IllegalArgumentException("The event " + evt + " is not recognized as valid");
        
    }//end parse

}//end EventMessage


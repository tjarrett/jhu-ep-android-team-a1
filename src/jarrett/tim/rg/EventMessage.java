package jarrett.tim.rg;

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
    private Event event;
    
    /**
     * The direction
     */
    private String direction;
    
    /**
     * Constructor
     * @param evt   The event being sent in -- looks something like: 0,1|Heat|UP
     */
    public EventMessage(String evt)
    {
        //Split on |
        String[] bits = evt.split("\\|");
        
        //Sometimes there won't be a direction... 
        if ( bits.length == 3 ) {
            event = Event.valueOf(bits[1]);
            direction = bits[2];
            return;
        }
        
        //If it's reset... 
        if ( evt.toLowerCase().indexOf("reset") != -1 ) {
            event = Event.Reset;
            return;
            
        }
        
        //If it's register
        if ( evt.toLowerCase().indexOf("register") != -1 ) {
            event = Event.Register;
            return;
        }
        
        //Otherwise error
        throw new RuntimeException("Kaboom!");
        
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
     * @return the direction
     */
    public String getDirection()
    {
        return direction;
    }

}

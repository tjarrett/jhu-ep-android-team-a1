package jarrett.tim.rg;

public class EventMessage
{
    private Event event;
    
    private String direction;
    
    public EventMessage(String evt)
    {
        String[] bits = evt.split("\\|");
        
        if ( bits.length == 3 ) {
            event = Event.valueOf(bits[1]);
            direction = bits[2];
            return;
        }
        
        if ( evt.toLowerCase().indexOf("reset") != -1 ) {
            event = Event.Reset;
            return;
            
        }
        
        throw new RuntimeException("Kaboom!");
        
    }

    /**
     * @return the event
     */
    public Event getEvent()
    {
        return event;
    }

    /**
     * @return the direction
     */
    public String getDirection()
    {
        return direction;
    }

}

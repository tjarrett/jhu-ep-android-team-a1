package jarrett.tim.rg;

import com.javadude.rube.protocol.Direction;

import android.content.Context;

/**
 * Handle the wire left-right gadget
 * @author tjarrett
 *
 */
public class WireLeftRightView extends WireView
{
	private ThingEnds ends;
	
    /**
     * Constructor
     * @param context
     */
    public WireLeftRightView(Context context)
    {    	
        super(context);

        //Build our state-to-image map
        addStateImage(State.NoCurrent, "wire_no_current_left_right");
        addStateImage(State.Wet, "wire_wet_left_right");
        addStateImage(State.Shorted, "wire_shorted_left_right");
        addStateImage(State.HasCurrent, "wire_has_current_left_right");
        addStateImage(State.Toasted, "wire_toasted_left_right");
        
    }//end WireLeftRightView constructor
    
    /**
     * Return the ends that we are attached to...
     */
	@Override public ThingEnds getEnds() 
	{
		if ( ends == null ) {
	    	//Set up our ends...
	    	ends = new ThingEnds(Direction.LEFT, Direction.RIGHT);
	    	
		}
		
		return ends;
		
	}//end getEnds

}//end WireLeftRightView

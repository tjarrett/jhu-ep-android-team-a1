package jarrett.tim.rg;

import com.javadude.rube.protocol.Direction;

import android.content.Context;

/**
 * Display the rope gadget going from bottom of screen to right screen
 * @author tjarrett
 */
public class RopeUpRightView extends RopeView
{
	private ThingEnds ends;
	
    /**
     * Constructor
     * @param context
     */
    public RopeUpRightView(Context context)
    {
        super(context);
        
        //Build our state-to-image map
        addStateImage(State.Slack, "rope_slack_up_right");
        addStateImage(State.BurningSlack, "rope_burning_slack_up_right");
        addStateImage(State.Taut, "rope_taut_up_right");
        addStateImage(State.BurningTaut, "rope_burning_taut_up_right");
        addStateImage(State.Ashes, "rope_ashes_up_right");
        
    }//end RopeLeftRightView constructor
    
    /**
     * Return the ends that we are attached to...
     */
	@Override public ThingEnds getEnds() 
	{
		if ( ends == null ) {
	    	//Set up our ends...
	    	ends = new ThingEnds(Direction.UP, Direction.RIGHT);
	    	
		}
		
		return ends;
		
	}//end getEnds

}//end RopeLeftRightView

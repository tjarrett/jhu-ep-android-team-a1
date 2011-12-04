package jarrett.tim.rg;

import com.javadude.rube.protocol.Direction;

import android.content.Context;

/**
 * View for the Rope left-right gadget
 * @author tjarrett
 *
 */
public class RopeLeftRightView extends RopeView
{
	private ThingEnds ends;
	
    /**
     * Constructor
     * @param context
     */
    public RopeLeftRightView(Context context)
    {
        super(context);
        
        //Build our state-to-image map
        addStateImage(State.Slack, "rope_slack_left_right");
        addStateImage(State.BurningSlack, "rope_burning_slack_left_right");
        addStateImage(State.Taut, "rope_taut_left_right");
        addStateImage(State.BurningTaut, "rope_burning_taut_left_right");
        addStateImage(State.Ashes, "rope_ashes_left_right");
        
    }//end RopeLeftRightView constructor
    
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

}//end RopeLeftRightView

package jarrett.tim.rg;

import jarrett.tim.rg.CleatView.State;
import android.content.Context;

/**
 * Display the rope gadget going from bottom of screen to right screen
 * @author tjarrett
 */
public class RopeUpDownView extends RopeView
{
    /**
     * Constructor
     * @param context
     */
    public RopeUpDownView(Context context)
    {
        super(context);
        
        //Build our state-to-image map
        addStateImage(State.Slack, "rope_slack_up_down");
        addStateImage(State.BurningSlack, "rope_burning_slack_up_down");
        addStateImage(State.Taut, "rope_taut_up_down");
        addStateImage(State.BurningTaut, "rope_burning_taut_up_down");
        addStateImage(State.Ashes, "rope_ashes_up_down");
        
    }//end RopeLeftRightView constructor

}//end RopeLeftRightView

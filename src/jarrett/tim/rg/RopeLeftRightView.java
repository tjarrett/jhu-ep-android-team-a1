package jarrett.tim.rg;

import jarrett.tim.rg.RopeView.State;
import android.content.Context;

/**
 * View for the Rope left-right gadget
 * @author tjarrett
 *
 */
public class RopeLeftRightView extends RopeView
{
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

}//end RopeLeftRightView

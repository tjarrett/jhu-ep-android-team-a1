package jarrett.tim.rg;

import android.content.Context;

/**
 * Handle the wire left-right gadget
 * @author tjarrett
 *
 */
public class WireLeftRightView extends WireView
{
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

}//end WireLeftRightView

package jarrett.tim.rg;

import android.content.Context;

/**
 * Handle the wire up-down gadget 
 * @author tjarrett
 *
 */
public class WireUpDownView extends WireView
{
    /**
     * Constructor
     * @param context
     */
    public WireUpDownView(Context context)
    {
        super(context);

        //Build our state-to-image map
        addStateImage(State.NoCurrent, "wire_no_current_up_down");
        addStateImage(State.Wet, "wire_wet_up_down");
        addStateImage(State.Shorted, "wire_shorted_up_down");
        addStateImage(State.HasCurrent, "wire_has_current_up_down");
        addStateImage(State.Toasted, "wire_toasted_up_down");
        
    }//end WireUpDownView constructor

}//end WireUpDownView

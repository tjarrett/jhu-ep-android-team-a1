package jarrett.tim.rg;

/**
 * Dictate which methods a StateChangeListener must implement
 * @author tjarrett
 *
 */
public interface StateChangeListener
{
    /**
     * Fired when the state is changed and sends the view on which the state was changed
     * @param view
     */
    public void onStateChanged(ThingView view);

}//end StateChangeListener

package jarrett.tim.rg;

import com.javadude.rube.protocol.Event;

import android.content.Context;
import android.graphics.Canvas;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.RotateAnimation;

/**
 * Handles displaying the PinWheel gadget
 * @author tjarrett
 *
 * @todo In future or if I have time before 11/7/2011 -- figure out how to stop and start the pinwheel in place... it would be nice 
 *       if they made it easier to get the current rotation amount without doing the math using the matrix...
 */
public class PinwheelView extends ThingView
{
    /**
     * Hold on to the animation so that we aren't setting it up each time
     */
    private Animation animation;
    
    /**
     * Hold on to the context... even though my parent has it he isn't sharing...
     */
    private Context context;
    
    /**
     * The various states that our tree can be in
     */
    public enum State implements ThingState
    {
        Stopped,
        Turning
        
    }//end State enum

    /**
     * Constructor
     * @param context
     */
    public PinwheelView(Context context)
    {
        super(context);
        
        this.context = context;
        
        //Set the initial state
        setInitialState(State.Stopped);
        reset();
        
        //Build our transition tables for our state machine
        addTransition(State.Stopped, Event.Steam, State.Turning, Emit.TURN_ALL);
        addTransition(State.Turning, Event.Pulse, State.Stopped);
        
        //Add our images for each state
        addStateImage(State.Stopped, "pinwheel");
        addStateImage(State.Turning, "pinwheel");
        
        //Don't auto-invalidate when an event is received... that messes with our animation
        setAutoInvalidateOnReceiveEvent(false);
        
    }//end PinwheelView
    
    /**
     * Transitions to the next state based on the event given. 
     * @param event
     */
    public void receiveEvent(String event)
    {    
        //Translate the event
        EventMessage em;
        
        try {
            em = EventMessage.parse(event);
            
        } catch ( Exception e ) {
            //Something went wrong, complain about it but swallow the error and continue on
            RgTools.createNotification(this.context, "Invalid event", "Invalid event " + event + " received", android.R.drawable.ic_menu_share);            
            return;
        }        
        
        //Animation makes this less straightforward than the others because we can 
        //interrupt a state ...
        if ( em.getEvent() == Event.Pulse && getState() == State.Turning ) {
            clearAnimation();
            animation.cancel();
            
        } else if ( getState() == State.Stopped && em.getEvent() == Event.Steam ) {
            startAnimation(animation);
            
        }
        
        //Still pass it up the chain
        super.receiveEvent(event); 
        
    }//end receiveEvent

    /**
     * Handle the drawing
     */
    @Override protected void onDraw(Canvas canvas)
    {               
        //Build our animation if we don't have it already
        if ( animation == null ) {
            animation = new RotateAnimation(360, 0, (float)this.getWidth()/2, (float)this.getHeight()/2);      
            animation.setFillAfter(true);            
            animation.setRepeatCount(0);
            animation.setDuration(3000);
            animation.setAnimationListener(new AnimationListener() {
                /**
                 * Update the state
                 */
                @Override public void onAnimationEnd(Animation animation)
                {
                    setState(State.Stopped);
                    notifyStateChangeListeners();
                    
                }

                @Override
                public void onAnimationRepeat(Animation animation)
                {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public void onAnimationStart(Animation animation)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            
        }

        //Go do the drawing stuff
        super.onDraw(canvas);

    }//end onDraw

}//end PinwheelView


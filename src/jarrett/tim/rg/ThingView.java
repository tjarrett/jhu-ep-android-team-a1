package jarrett.tim.rg;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.Log;
import android.widget.ImageView;

/**
 * An abstract base class used by the Monkey, ChristmasTree, and Pinwheel classes that extends 
 * ImageView. It provides some common functionality and to help this homework adhere to the DRY 
 * principle.
 * @author tjarrett
 *
 */
abstract public class ThingView extends ImageView
{    
    /**
     * The initial state
     */
    private ThingState state;
    
    /**
     * The initial state 
     */
    private ThingState initialState;
    
    /**
     * Mapping of state to a string representing the image to display for that state
     */
    private Map<ThingState, String> stateImageMap = new HashMap<ThingState, String>();
    
    /**
     * Keep track of our transitions between states
     */
    private Map<ThingState, Map<Event, StateTransitionPackage>> transitions = new HashMap<ThingState, Map<Event, StateTransitionPackage>>();
    
    /**
     * Whether or not to invalidate when we receive an event
     */
    private boolean invalidateOnReceiveEvent = true;
    
    /**
     * Is it legal to hold on to the context... I hope so...
     */
    private Context context;
    
    /**
     * List of state change listeners that are awaiting state changes
     */
    private List<StateChangeListener> stateChangeListeners = new LinkedList<StateChangeListener>();
    
    /**
     * The event that is emitted by the last state change. Can be null.
     */
    private List<String> emits;

    /**
     * Constructor
     */
    public ThingView(Context context)
    {
        super(context);
        this.context = context;
        
    }//end Thing    
    
    /**
     * Set the state to a particular state
     * @param state
     */
    protected void setState(ThingState state)
    {        
        this.state = state;        
        
    }//end setState
    
    /**
     * Get the initial state 
     * @return
     */
    public ThingState getInitialState()
    {
        return initialState;
        
    }//end getInitialState 

    /**
     * Set the initial state
     * @param initialState
     */
    public void setInitialState(ThingState initialState)
    {
        this.initialState = initialState;
        
    }//end setInitialState
    
    /**
     * Returns the current state
     * @return
     */
    public ThingState getState()
    {
        return state;
        
    }//end getState
    
    /**
     * @return the emits
     */
    public List<String> getEmits()
    {
        return emits;
    }

    /**
     * @param emits the emits to set
     */
    protected void setEmits(List<String> emits)
    {
        this.emits = emits;
    }

    /**
     * Whether we want to auto invalidate this view when we receive a new event
     * @param setting
     */
    protected void setAutoInvalidateOnReceiveEvent(boolean setting)
    {
        this.invalidateOnReceiveEvent = setting;
        
    }//end setAutoInvalidateOnReceiveEvent

    /**
     * Transitions to the next state based on the event given. 
     * @param event
     */
    public void receiveEvent(String event)
    {   
    	Log.d("Tim", "Received message: " + event);
    	
        //Translate the event
        EventMessage em;
        
        try {
            em = EventMessage.parse(event);
            
        } catch ( Exception e ) {
            //Something went wrong, complain about it but otherwise just swallow the error...
            RgTools.createNotification(this.context, "Invalid event", "Invalid event " + event + " received", android.R.drawable.ic_menu_share);            
            return;
        }
        
        //Regardless of the Thing, if the event is reset, just reset it...
        if ( em.getEvent() == Event.Reset ) {
            reset();
            
        } else {
            //Try to get the next state map
            Map<Event, StateTransitionPackage> nextStateMap = transitions.get(state);
            
            //If it's null, then something is pretty screwed up because we don't have 
            //an entry for the current state that we are in... 
            if ( nextStateMap == null ) {
                //No state transition for that event, also clear out emits and direction
                setEmits(null);
                return;
                
            }
            
            //Still here? Then we have a non-null map. Now try to see if we have 
            //a state to transition to...
            StateTransitionPackage pkg = nextStateMap.get(em.getEvent());
            
            if ( state.toString().equals("Taut") && em.getEvent().toString().equals("Release") ) {
            	if ( pkg == null ) {
            		Log.d("Tim", "No match...");
            	}
            }
            
            //Do the check again. This time, we expect null to happen from time-to-time, just ignore it
            if ( pkg == null ) {
            	Log.d("Tim", "Current State: " + this.state.toString() + "; Current Event: " + em.getEvent().toString()); 
                setEmits(null);
                return;
                
            }
            
            //All pkgs have a nextState
            ThingState nextState = pkg.getNextState();
            
            //Otherwise, transition the state
            setState(nextState);
            
            //Make a copy of the things that should be emitted in this state change
            List<String> emits = pkg.getEmits();
            List<String> preEmit = new LinkedList<String>();
            
            //Loop through everything that is emits
            for ( int i=0; i<emits.size(); i++ ) {
            	//Get the test
            	String response = emits.get(i);
            	
            	//Split it out
            	String[] bits = response.split("\\|");
            	
            	//There should always be two items... if not just skip forward
            	if ( bits.length != 2 ) {
            		preEmit.add(response);
            		continue;
            		
            	}
            	
            	//Figure out the direction that the response is going to be emitted
            	Direction direction = Direction.valueOf(bits[1]);
            	
            	//If the direction is opposite, then we are probably dealing with a Wire or a Rope (otherwise something is really wrong)
            	if ( direction == Direction.OPPOSITE ) {
            		Log.d("Tim", "Came from direction: " + getOppositeDirection(em.getDirection()).toString());
            		
            		//Figure out the OPPOSITE of the incoming event
            		Direction opposite = getOppositeEnd(getOppositeDirection(em.getDirection()));
            		Log.d("Tim", "So opposite end would be... " + opposite);
            		
            		//If the opposite makes sense... (is not null)
            		if ( opposite != null ) {
            			//Push this message into the list of things that we are going to output
            			preEmit.add(bits[0] + "|" + opposite.toString());
            			
            		}
            		
            	} else {
            		preEmit.add(response);
            		
            	}
            	
            }//end for i
            
            //Send the updated list out as a response
            setEmits(preEmit);
            
        }
        
        //Notify any state change listeners...
        notifyStateChangeListeners();
        
        //Invalidate (or not)
        if ( invalidateOnReceiveEvent ) {
            invalidate();
            
        }
        
    }//end receiveEvent

    /**
     * Resets this gadget to it's initial state
     */
    public void reset()
    {
        //Set the state
        setState(getInitialState());
        
        //Clear out emits and direction as well
        setEmits(null);
        
        //Notify any listeners
        notifyStateChangeListeners();
        
    }//end reset
    
    /**
     * Given a string emits, tries to match it up to an Emit constant and returns a list 
     * of responses. Override this method for ropes and wires to deal with BOTH and OPPOSITE
     * @param emits
     * @return
     */
    protected List<String> generateEmitsList(String emits)
    {	
    	//We are always going to return a list...
        List<String> emitList = new LinkedList<String>();
        
        //May be that a state change doesn't emit anything...
    	if ( emits == null ) {
    		return emitList;
    		
    	}
        
    	//Still here? Then emits wasn't null... try splitting it apart
        String[] bits = emits.split("\\|");
        
        //Has to be exactly two 
        if ( bits.length != 2 ) {
        	
        	return emitList;
        	
        }
        
        //Get the event and direction
        Event event = Event.valueOf(bits[0]);
        Direction direction = Direction.valueOf(bits[1]);
        
        //If direction is ALL then we need to generate 4 messages -- one in each direction
        switch ( direction ) {
        	case ALL:
                emitList.add(event + "|" + Direction.UP);
                emitList.add(event + "|" + Direction.LEFT);
                emitList.add(event + "|" + Direction.RIGHT);
                emitList.add(event + "|" + Direction.DOWN);
        		break;
        		
        	case BOTH:
        		ThingEnds ends = getEnds();
        		if ( ends == null ) {
        			emitList.add(emits);
        			
        		} else {
        			emitList.add(event + "|" + ends.getEnd1().toString());
        			emitList.add(event + "|" + ends.getEnd2().toString());
        			
        		}
        		break;
        
        	default:
        		emitList.add(emits);
        		break;
        		
        }//end switch
        
        return emitList;
        
    }//end generateEmitsList

    /**
     * Add a transition rule to the internal state table
     * 
     * @param currentState
     * @param event
     * @param nextState
     */
    protected void addTransition(ThingState currentState, Event event, ThingState nextState)
    {
        String emits = null;
        addTransition(currentState, event, nextState, emits);
        
    }//end addTransition
    
    /**
     * Add a transition rule to the internal state table
     * @param currentState
     * @param event
     * @param nextState
     * @param emits
     * @param direction
     */
    protected void addTransition(ThingState currentState, Event event, ThingState nextState, String emits)
    {
        addTransition(currentState, event, nextState, generateEmitsList(emits));
        
    }//end addTransition

    /**
     * Add a transition rule to the internal state table
     * @param currentState
     * @param event
     * @param nextState
     * @param emits
     * @param direction
     */
    protected void addTransition(ThingState currentState, Event event, ThingState nextState, List<String> emits)
    {
        //Figure out if we have an entry for this currentState yet, if not add it...
        if ( transitions.get(currentState) == null ) {
            transitions.put(currentState, new HashMap<Event, StateTransitionPackage>());
            
        }
        
        //Now we KNOW we have an entry for currentState, so get it
        Map<Event, StateTransitionPackage> eventToNextState = transitions.get(currentState);
        
        //Build out package
        StateTransitionPackage pkg = new StateTransitionPackage(nextState, emits);
        
        //Save this package to be returned when the appropriate event occurs
        eventToNextState.put(event, pkg);
        
    }//end addTransition
    
    /**
     * Utility method for building the state-to-image map
     * @param state
     * @param image
     */
    protected void addStateImage(ThingState state, String image)
    {
        stateImageMap.put(state, image);
        
    }//end addStateImage
    
    /**
     * Get the image for a given state based on the state-to-image map built up using 
     * addStateImage
     * @param state
     * @return
     */
    protected String getImageForState(ThingState state)
    {
        return stateImageMap.get(state);
        
    }//end getImageForState

    /**
     * Default onDraw behavior
     */
    @Override protected void onDraw(Canvas canvas)
    {
        //Get the name of the image
        String imageName = getImageForState(getState());     
        
        //http://www.anddev.org/how_to_get_image_in_res-drawable_by_name_not_by_id-t9833.html
        //Got get the id of the image (this is why we held on to the context)
        int imageResource = context.getResources().getIdentifier(imageName, "drawable", getClass().getPackage().getName());
        
        //Get our bitmap
        Bitmap image = BitmapFactory.decodeResource(getResources(), imageResource);  
        
        //Center it
        float left = ((float)this.getWidth()/2)-((float)image.getWidth()/2);
        float top = ((float)this.getHeight()/2)-((float)image.getHeight()/2);
        
        //Draw it
        canvas.drawBitmap(image, left, top, null);
        
        //Let me parent do it's work
        super.onDraw(canvas);
        
    }//end onDraw
    
    /**
     * Add a state change listener
     * @param scl
     */
    public void addStateChangeListener(StateChangeListener scl)
    {
        stateChangeListeners.add(scl);
        
    }//end addStateChangeListener
    
    /**
     * Clear all state change listeners
     */
    public void clearStateChangeListeners()
    {
        stateChangeListeners.clear();
        
    }//end clearStateChangeListeners
    
    /**
     * Notify any state change listeners that are listening
     */
    protected void notifyStateChangeListeners()
    {
        for ( StateChangeListener scl : stateChangeListeners ) {
            scl.onStateChanged(this);
            
        }//end for
        
    }//end notifyStateChangeListeners
    
    /**
     * Return the opposite direction for this type of wire or rope. Returns null if 
     * direction is not a direction that this wire or rope is connected to or if 
     * this is not a wire or rope...
     * 
     * @param direction
     * @return
     */
    public Direction getOppositeEnd(Direction direction)
    {
    	ThingEnds ends = getEnds();
    	
    	if ( ends == null ) {
    		return null;
    		
    	}
    	
    	if ( ends.getEnd1() == direction ) {
    		return ends.getEnd2();
    		
    	} else if ( ends.getEnd2() == direction ) {
    		return ends.getEnd1();
    		
    	}
    	
    	return null;
    	
    }//end getOppositeEnd
    
    /**
     * Get the opposite of the given direction. Returns null if originalDirection is not valid
     * @param originalDirection
     * @return
     */
    static public Direction getOppositeDirection(Direction originalDirection)
    {
        Direction flipped;
        Direction comingFromDirection = originalDirection;
        
        if ( comingFromDirection == Direction.UP ) {
        	flipped = Direction.DOWN;
        	
        } else if ( comingFromDirection == Direction.DOWN ) {
        	flipped = Direction.UP;
        	
        } else if ( comingFromDirection == Direction.LEFT ) {
        	flipped = Direction.RIGHT;
        	
        } else if ( comingFromDirection == Direction.RIGHT ) {
        	flipped = Direction.LEFT;
        	
        } else {
        	return null;
        	
        }
        
        return flipped;
    	
    }//end 
    
    /**
     * Returns the valid ends associated with this Thing. For most things, this will be null because 
     * they have no ends... If you are implementing a Wire or a Rope you should override...
     * @return
     */
    public ThingEnds getEnds()
    {
    	return null;
    	
    }//end getEnds
    
    /**
     * Simple class for tracking the two ends of the Wire and Rope classes
     * @author tjarrett
     *
     */
    protected class ThingEnds
    {
    	private Direction end1;
    	
    	private Direction end2;
    	
    	/**
    	 * Constructor
    	 * @param end1
    	 * @param end2
    	 */
    	public ThingEnds(Direction end1, Direction end2)
    	{
    		this.end1 = end1;
    		this.end2 = end2;
    		
    	}//end ThingEnds Constructor
    	
    	/**
    	 * Returns the first end
    	 * @return
    	 */
    	public Direction getEnd1()
    	{
    		return end1;
    		
    	}//end getEnd1
    	
    	/**
    	 * Returns the 2nd end
    	 * @return
    	 */
    	public Direction getEnd2()
    	{
    		return end2;
    		
    	}//end getEnds2
    	
    }//end ThingEnds
    
    /**
     * Class for keeping track of event transitions
     * @author tjarrett
     *
     */
    private class StateTransitionPackage
    {
        /**
         * The next state that this should transition to. This should not be null
         */
        private ThingState nextState;
        
        /**
         * An event that this state transition emits
         */
        private List<String> emits;
        
        /**
         * We are guranteeing that we at least have the nextState
         * @param nextState
         */
        @SuppressWarnings("unused")
        public StateTransitionPackage(ThingState nextState)
        {
            this.nextState = nextState;
            
        }//end Constructor
        
        /**
         * Constructor with emits and direction (which can be null)
         * @param nextState
         * @param emits
         * @param direction
         */
        public StateTransitionPackage(ThingState nextState, List<String> emits)
        {
            this.nextState = nextState;
            this.emits = emits;
            
        }//end StateTransitionPackage

        /**
         * @return the nextState
         */
        public ThingState getNextState()
        {
            return nextState;
        }

        /**
         * @param nextState the nextState to set
         */
        @SuppressWarnings("unused")
        public void setNextState(ThingState nextState)
        {
            this.nextState = nextState;
        }

        /**
         * @return the emits
         */
        public List<String> getEmits()
        {
            return emits;
        }

        /**
         * @param emits the emits to set
         */
        @SuppressWarnings("unused")
        public void setEmits(List<String> emits)
        {
            this.emits = emits;
        }     
        
    }//end StateTransitionPackage
    
}//end ThingView


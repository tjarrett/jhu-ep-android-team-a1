package jarrett.tim.rg;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
    private void setEmits(List<String> emits)
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
            
            //Do the check again. This time, we expect null to happen from time-to-time, just ignore it
            if ( pkg == null ) {
                setEmits(null);
                return;
                
            }
            
            //All pkgs have a nextState
            ThingState nextState = pkg.getNextState();
            
            //Otherwise, transition the state
            setState(nextState);
            
            //And record the latest emits and direction...
            setEmits(pkg.getEmits());
            
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
        List<String> emitList = new LinkedList<String>();
        
        if ( Emit.HEAT_UP.equals(emits) ) {
            emitList.add(Event.Heat.toString() + "|" + Direction.UP);
             
        } else if ( Emit.CLAP_ALL.equals(emits) ) {
            String evt = Event.Clap.toString();
            emitList.add(evt + "|" + Direction.UP);
            emitList.add(evt + "|" + Direction.LEFT);
            emitList.add(evt + "|" + Direction.RIGHT);
            emitList.add(evt + "|" + Direction.DOWN);
            
        } else if ( Emit.STEAM_RIGHT.equals(emits) ) {
            emitList.add(Event.Steam.toString() + "|" + Direction.RIGHT);
            
        } else if ( Emit.ALEX_ALL.equals(emits) ) {
            String evt = Event.Alex.toString();
            emitList.add(evt + "|" + Direction.UP);
            emitList.add(evt + "|" + Direction.LEFT);
            emitList.add(evt + "|" + Direction.RIGHT);
            emitList.add(evt + "|" + Direction.DOWN);
            
        } else if ( Emit.WATER_DOWN.equals(emits) ) {
            emitList.add(Event.Water.toString() + "|" + Direction.DOWN);
            
        }
        
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


package jarrett.tim.rg;

import java.net.Socket;
import com.javadude.rube.protocol.Direction;
import com.javadude.rube.protocol.Event;
import com.javadude.rube.protocol.EventCarrier;
import com.javadude.rube.protocol.SocketHandler;

/**
 * A concrete implementation of a socket handler for a Rube app on a device
 */
public class GadgetSocketHandler extends SocketHandler {
	private final ActivityMain rubeActivity;
	/** constructor */
	public GadgetSocketHandler(ActivityMain rubeActivity, Socket socket) {
		super(rubeActivity, rubeActivity, socket);
		this.rubeActivity = rubeActivity;
	}
	/** process an event on the dummy client - we just report it */
	@Override protected void processReceivedEvent(int x, int y, Event event, Direction direction) {
		getReporter().report("Processing " + event + "[" + direction + "] at " + x + ", " + y);
		rubeActivity.sendToHandler(new EventCarrier(event, x, y, 0, direction));
	}
}

package jarrett.tim.rg;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Utility class for dealing with switching modes, comments etc
 */
public class RgTools {
	/**
	 * Debug constant for android debugging
	 */
	public static final String DEBUG = "Group-A1-DEBUG";
	
	/**
	 * Server constant for android debugging
	 */
    public static final String SERVER = "Group-A1-Server";

	/**
	 * Client constant for android debugging
	 */
	public static final String CLIENT = "Group-A1-Client";

	/**
	 * Bluetooth constant for android debugging
	 */
	public static final String BLUETOOTH_SERVER = "Group-A1-Bluetooth-Server";
	
	/**
	 * QR code constant for android debugging
	 */
	public static final String QR_SCANNER = "Group-A1-QR-Scanner";

	
	/**
	 * If this is set to true, will try to send button press events via wifi otherwise 
	 * keeps the events local -- mostly for testing purposes in an emulator only. Leave this 
	 * as true as it will get flipped to false if no wifi is detected (and a message will 
	 * be displayed)
	 */
	public static final boolean wifiMode = true;
	
	/**
	 * Create a notification and show it
	 * @param context
	 * @param ntf_title
	 * @param ntf_text
	 * @param icon
	 */
	public static void createNotification(Context context, String ntf_title, String ntf_text, int icon) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
		
		Notification notification = new Notification(icon, ntf_text, System.currentTimeMillis());

		Intent notificationIntent = new Intent(context, context.getClass());
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, ntf_title, ntf_text, contentIntent);
		mNotificationManager.notify(1, notification);
	}
}

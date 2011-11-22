package jarrett.tim.rg;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class RgTools {
    public static final String SERVER = "Group-A1-Server";

	public static final String CLIENT = "Group-A1-Client";

	public static final String BLUETOOTH_SERVER = "Group-A1-Bluetooth-Server";
	
	public static final String QR_SCANNER = "Group-A1-QR-Scanner";
	
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

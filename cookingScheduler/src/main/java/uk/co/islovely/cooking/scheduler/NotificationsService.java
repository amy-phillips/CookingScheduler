package uk.co.islovely.cooking.scheduler;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;


public class NotificationsService extends IntentService {
	private static final String TAG = "NotificationsService";
	public static final String BROADCAST_ACTION = "uk.co.islovely.cooking.scheduler.notification";
	
	public class RecipeStepDataComparator implements Comparator<RecipeStepData> {
	    public int compare(RecipeStepData o1, RecipeStepData o2) {
	        return o1.ReminderTime.compareTo(o2.ReminderTime);
	    }
	}
	
	private ArrayList<AmyMenuItem> selectedItems = new ArrayList<AmyMenuItem>();
    private Calendar FinishTime = new GregorianCalendar();
    private ArrayList<RecipeStepData> sortedSteps = new ArrayList<RecipeStepData>();
    
    // current step needs to be saved so that when user selects a notification and 
    // this service is restarted they don't get that same notification again!
    private static int currentStepIndex = 0;
    
    private Intent updateGUIIntent;
	private boolean abort = false;
	
	/** 
	 * A constructor is required, and must call the super IntentService(String)
	 * constructor with a name for the worker thread.
	 */
	public NotificationsService() {
		super("CookingSchedulerNotificationsService");
	}
	
	@Override
    public void onCreate() {
        super.onCreate();
        abort = false;
        updateGUIIntent = new Intent(BROADCAST_ACTION);	
    }
	
	@Override
	public void onDestroy() {
		abort = true;
		super.onDestroy();
	}
	
	// tell the summary activity to update ui
	private void UpdateSummaryActivityUI()
	{
		Log.d(TAG, "UpdateSummaryActivityUI");
		
		if(currentStepIndex < sortedSteps.size()) {
			RecipeStepData current_step = GetCurrentStepData();
			updateGUIIntent.putExtra("next_step", current_step);
		}
		else {
			updateGUIIntent.removeExtra("next_step");
		}
		
    	sendBroadcast(updateGUIIntent);
	}

	private void WaitSeconds(int seconds) throws InterruptedException {
		long endTime = System.currentTimeMillis() + seconds*1000;
		while (System.currentTimeMillis() < endTime) {
			synchronized (this) {
				wait(endTime - System.currentTimeMillis());
			}
		} 
	}
	
	public void FillInSortedSteps() {
		// fill in reminder times
		for (AmyMenuItem item : selectedItems) {
			int item_minutes = item.GetTotalTime();
			Calendar start_this_step_time = (Calendar) FinishTime.clone();			
			start_this_step_time.add(Calendar.MINUTE, -item_minutes);
			
			for (RecipeStep step : item.Steps) {
				sortedSteps.add(new RecipeStepData(step, item.Name, (Calendar) start_this_step_time.clone()));
				start_this_step_time.add(Calendar.MINUTE, step.TimeTaken);
			}
		}
		
		// sort the steps
		Collections.sort(sortedSteps, new RecipeStepDataComparator());
	}
	
	public RecipeStepData GetCurrentStepData() {
		return sortedSteps.get(currentStepIndex);
	}

	
	/**
	 * The IntentService calls this method from the default worker thread with
	 * the intent that started the service. When this method returns, IntentService
	 * stops the service, as appropriate.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
	
		FinishTime.setTimeInMillis(intent.getExtras().getLong("finish_time"));
		selectedItems = intent.getParcelableArrayListExtra("selected_items");
		// bit hacky but this gets round starting via start cooking button (reset state), vs start via a notification (leave state)
		boolean reset_current_step = intent.getExtras().getBoolean("reset_current_step"); 
		if(reset_current_step) {
			currentStepIndex = 0;
		}
		
		// fill in the reminder times for all recipe steps
		FillInSortedSteps();
		UpdateSummaryActivityUI();
		
		try {
			while (currentStepIndex < sortedSteps.size() && !abort) {
				Calendar now = new GregorianCalendar();
				RecipeStepData next_step = GetCurrentStepData();
				String now_time_string=DateFormat.getLongDateFormat(this).format(now.getTimeInMillis()) + " " + DateFormat.getTimeFormat(this).format(now.getTimeInMillis());
				String step_time_string=DateFormat.getLongDateFormat(this).format(next_step.ReminderTime.getTimeInMillis()) + " " + DateFormat.getTimeFormat(this).format(next_step.ReminderTime.getTimeInMillis());
				Log.d(TAG, "checking now "+now_time_string+" against "+next_step.step.Description+" "+step_time_string);
				boolean did_something = false;
				if(next_step.ReminderTime.before(now)) {
					DoNotification(next_step);
					currentStepIndex++;
					UpdateSummaryActivityUI();
					did_something = true;
				}
				
				if(!did_something) {
					WaitSeconds(15);
				}
			}
		} catch (InterruptedException e) {
		}
		
		// our work here is done
		stopSelf();
	}

	private int notificationId = 1;

	public void DoNotification(RecipeStepData next_step_data) {
		// get notification manager
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		// instantiate me a notification
		NotificationCompat.Builder notification_builder = new NotificationCompat.Builder(this);
		
		Intent notificationIntent = new Intent(this, SummaryActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification_builder.setContentIntent(contentIntent);
		
		notification_builder.setTicker(next_step_data.step.Description);
		notification_builder.setContentTitle(next_step_data.step.Description);
		CharSequence contentText = String.format (getString(R.string.reminder_text_format), next_step_data.menuItemName, next_step_data.step.Description);
		notification_builder.setContentText(contentText);
		notification_builder.setWhen(next_step_data.ReminderTime.getTimeInMillis());
		
		notification_builder.setDefaults(Notification.DEFAULT_ALL);
		notification_builder.setAutoCancel(true); //remove notification after selected
		notification_builder.setOngoing(false);
		notification_builder.setSmallIcon(R.drawable.ic_launcher);
		
		// bosh it into the notification manager - increment notification ID so we can have more than one notification
		mNotificationManager.notify(notificationId++, notification_builder.build());
	}

}




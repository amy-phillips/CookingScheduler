package uk.co.islovely.cooking.scheduler;

// test rotating with dialog
// test all areas for red log text

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

import uk.co.islovely.cooking.scheduler.R;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

public class SummaryActivity extends Activity {
	private static final String TAG = "SummaryActivity";
	
	// these are filled in when the intent is passed from the summary activity,
	// so keep them
	// static so that when we get an intent from a notification we can just use
	// the old values
	private static ArrayList<AmyMenuItem> selectedItems = new ArrayList<AmyMenuItem>();
	private static Calendar FinishTime = new GregorianCalendar();
    private static ArrayList<RecipeStepData> sortedSteps = new ArrayList<RecipeStepData>();
    
    // current step needs to be saved so that when user selects a notification and 
    // this service is restarted they don't get that same notification again!
    private static int currentStepIndex = 0;
    
    private SummaryListAdapter adapter;
    
	public static class RecipeStepDataComparator implements Comparator<RecipeStepData> {
	    public int compare(RecipeStepData o1, RecipeStepData o2) {
	        return o1.ReminderTime.compareTo(o2.ReminderTime);
	    }
	}
	
	public static ArrayList<RecipeStepData> FillInSortedSteps(Context context, ArrayList<AmyMenuItem> items, Calendar finish_time) {
		ArrayList<RecipeStepData> steps = new ArrayList<RecipeStepData>();
		
		// fill in reminder times
		for (AmyMenuItem item : items) {
			int item_minutes = item.GetTotalTime();
			Calendar start_this_step_time = (Calendar) finish_time.clone();			
			start_this_step_time.add(Calendar.MINUTE, -item_minutes);
			
			for (RecipeStep step : item.Steps) {
				steps.add(new RecipeStepData(step, item.Name, (Calendar) start_this_step_time.clone()));
				start_this_step_time.add(Calendar.MINUTE, step.TimeTaken);
			}
		}
		
		// add a final step saying finished
		Calendar start_this_step_time = (Calendar) finish_time.clone();
		RecipeStep final_step = new RecipeStep(context.getString(R.string.nom), 0);
		steps.add(new RecipeStepData(final_step, context.getString(R.string.eat), (Calendar) start_this_step_time.clone()));
		
		// sort the steps
		Collections.sort(steps, new RecipeStepDataComparator());
		
		return steps;
	}
	
	public static RecipeStepData GetCurrentStepData() {
		return sortedSteps.get(currentStepIndex);
	}
	
	public RecipeStepData GetCurrentUnfinishedStepData() {
		// skip ahead until we find a step that is not tagged as complete
		final ListView lv = (ListView) findViewById(android.R.id.list);
		SparseBooleanArray completedItems = lv.getCheckedItemPositions();
        if(completedItems == null) {
            // nothing completed
            sortedSteps.get(0);
        }
		for(int i=currentStepIndex; i<sortedSteps.size(); ++i) {
			if(completedItems.get(i) == true) 
		    {
				// ignore this item as we've already completed it
				Log.v(TAG, "Ignoring step because already completed: "+sortedSteps.get(i));
		        continue;
		    }
			
			return sortedSteps.get(i);
		}
		
		// last step will be eat, so we'll return that one anyway
		return sortedSteps.get(sortedSteps.size()-1);
	}
	
	public static RecipeStepData GetPreviousStepData() {
		if(currentStepIndex == 0)
			return null;
		return sortedSteps.get(currentStepIndex-1);
	}

	private void InitNextStepUI() {
		Log.v(TAG, "InitNextStepUI");
		
		adapter = new SummaryListAdapter(this, android.R.layout.simple_list_item_multiple_choice, sortedSteps);
		final ListView lv = (ListView) findViewById(android.R.id.list);
		lv.setAdapter(adapter);
		lv.setItemsCanFocus(false);
		lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		lv.setOnItemClickListener(
		        new OnItemClickListener()
		        {
		            @Override
		            public void onItemClick(AdapterView<?> arg0, View view,
		                    int position, long id) {

		            	CheckedTextView  textView = (CheckedTextView)view.findViewById(android.R.id.text1);
		            	adapter.SetTextViewStyle(textView, lv, position);
		            	
		            	if(CheckForFinished()) {
		        			FinishedCooking();
		        		}
	                 }
	            }
		     );
	}

	public void UpdateNextStepUI() {
		Log.v(TAG, "UpdateNextStepUI");
		
		// any views that we already have we make sure are up to date with color and font...
		final ListView lv = (ListView) findViewById(android.R.id.list);
		for(int i=0; i<lv.getChildCount(); ++i) {
			View item_view = lv.getChildAt(i);
			int position = lv.getPositionForView(item_view);
			
			CheckedTextView  textView = (CheckedTextView)item_view.findViewById(android.R.id.text1);
			adapter.SetTextViewStyle(textView, lv, position);
		}
		
		adapter.notifyDataSetChanged();
	}
	
	public void SetNextAlarm() {
		Log.v(TAG, "SetNextAlarm");
		
		RecipeStepData next_step = GetCurrentStepData();
		if(next_step == null)
			return;
		
		Bundle bundle = new Bundle();
		bundle.putParcelable("uk.co.islovely.alarm.step", next_step);

		// Alarm manager - magic thing that does what we need
		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // Intent for our  BroadcastReceiver 
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("uk.co.islovely.alarm", bundle);
        
        // PendingIntent for AlarmManager 
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT );
        // Just in case we have already set up AlarmManager,
        // we do cancel.
        am.cancel(pendingIntent);

        Date stamp =  next_step.ReminderTime.getTime();
        
        // In case it's too late notify user today
        //if(stamp.getTime() < System.currentTimeMillis())
        //    stamp.setTime(stamp.getTime() + AlarmManager.INTERVAL_DAY);
                
        // Set one-time alarm
        am.set(AlarmManager.RTC_WAKEUP, stamp.getTime(), pendingIntent);
	}
	
	public void CancelAllAlarms() {
		Log.v(TAG, "cancelling alarm");
		
		Intent intent = new Intent(this, AlarmReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		alarmManager.cancel(sender);
	}
	
	private void DealWithIntent(Intent intent) {
		Log.v(TAG, "DealWithIntent");
		
		if (intent.hasExtra("finish_time")) // extras will not be set if we get
			// here from a notification (but
			// data will already be stored in
			// member variables)
		{
			Log.v(TAG, "extracting data and filling in steps");
			
			FinishTime.setTimeInMillis(intent.getExtras()
					.getLong("finish_time"));
			selectedItems = intent
					.getParcelableArrayListExtra("selected_items");
			
			sortedSteps = FillInSortedSteps(this,selectedItems,FinishTime);
			currentStepIndex = 0;
			InitNextStepUI();
		} else if (intent.hasExtra("step")) { // the alarm has gone off for this step - set up the next alarm
			Log.v(TAG, "on to the next step");
			//InitNextStepUI(); // make sure we have the listview set up properly - it may have gone back to no choices if we got shut down
			// have we already marked this as done?  If so, no notification, we just update the UI and set the next alarm
			ListView lv = (ListView) findViewById(android.R.id.list);
			SparseBooleanArray completedItems = lv.getCheckedItemPositions();

			if (completedItems == null || (completedItems.get(currentStepIndex) == false)) {
				RecipeStepData next_step = GetCurrentStepData();
				if(next_step != null)
					DoNotification(next_step);
			}
			
			currentStepIndex++;
		}
		
		// are we finished?
		if(CheckForFinished()) {
			FinishedCooking();
		}
		else if(currentStepIndex >= sortedSteps.size()) {
			//we're supposed to be finished but we haven't ticked all the boxes
			UpdateNextStepUI();
		} else {
			UpdateNextStepUI();
			SetNextAlarm();
		}
	}
	
	private boolean CheckForFinished() {
		if(currentStepIndex < sortedSteps.size()) 
			return false;
		
		// last step has finished 
		
		// have we ticked everything?
		final ListView lv = (ListView) findViewById(android.R.id.list);
		SparseBooleanArray checked_items = lv.getCheckedItemPositions();
		// if we don't have mappings for all items (except maybe the last nom nom nom step) then they're not all checked
		if((checked_items == null) || (checked_items.size()+1 < sortedSteps.size()))
			return false;
		
		boolean all_checked = true;
		for(int i=0; i<sortedSteps.size()-1; ++i) {
			if(!checked_items.get(i)) {
				all_checked = false;
				break;
			}
		}
		
		return all_checked;
	}
	
	private int notificationId = 1;
	public void DoNotification(RecipeStepData next_step_data) {
		Log.v(TAG, "DoNotification");
		
		CharSequence contentText = String.format (getString(R.string.reminder_text_format), next_step_data.menuItemName, next_step_data.step.Description);
		
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentTitle(next_step_data.step.Description)
		        .setTicker(next_step_data.step.Description)
		        .setContentText(contentText)
		        .setWhen(next_step_data.ReminderTime.getTimeInMillis());
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, SummaryActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(SummaryActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(notificationId, mBuilder.build());
		
	
		//notification_builder.setAutoCancel(true); //remove notification after selected
		//notification_builder.setOngoing(false);


	}
	
	@Override
	public void onNewIntent(Intent intent) {
		Log.v(TAG, "onNewIntent");
		
		super.onNewIntent(intent);
		
		DealWithIntent(intent);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		Log.v(TAG, "savedInstanceState" + savedInstanceState);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.summary);

		if(savedInstanceState == null) {
			Intent sender = getIntent();		
			DealWithIntent(sender);
		}
		else {
			// rotating screen or resuming...
			ExtractState(savedInstanceState);
			InitNextStepUI();
			UpdateNextStepUI();
		}
		
		String finish_time_string = DateFormat.getTimeFormat(this).format(
				FinishTime.getTime());
		// check that we are dealing with today, if not display the date
		Calendar now = new GregorianCalendar();
		if (FinishTime.get(Calendar.DATE) != now
				.get(Calendar.DATE)) {
			finish_time_string = DateFormat.getDateFormat(this).format(
					FinishTime.getTimeInMillis())
					+ " " + finish_time_string;
		}
		TextView finish_time = (TextView) this.findViewById(R.id.finishTime);
		finish_time.setText(finish_time_string);
	}
	
	 @Override
	 protected void onSaveInstanceState(Bundle state) {
	     super.onSaveInstanceState(state);
	     Log.v(TAG, "onSaveInstanceState");
	     
	     state.putParcelableArrayList("uk.co.islovely.cooking.selectedItems", selectedItems);
	     state.putSerializable("uk.co.islovely.cooking.FinishTime", FinishTime);
	     state.putParcelableArrayList("uk.co.islovely.cooking.sortedSteps", sortedSteps);
	     state.putSerializable("uk.co.islovely.cooking.currentStepIndex", currentStepIndex);
	     
	     Log.v(TAG, "state"+state);
	     
	     //final ListView lv = (ListView) findViewById(android.R.id.list);
		 //SparseBooleanArray completedItems = lv.getCheckedItemPositions();
		 //state.putParcelable("uk.co.islovely.cooking.completedItems", completedItems);
		 
//	     Log.v(TAG, "selectedItems " + selectedItems);
//	     Log.v(TAG, "FinishTime " + FinishTime);
//	     Log.v(TAG, "sortedSteps " + sortedSteps);
//	     Log.v(TAG, "currentStepIndex " + currentStepIndex);
	     
	 }
	 
	 @Override
	 protected void onRestoreInstanceState(Bundle savedInstanceState) {
	     super.onRestoreInstanceState(savedInstanceState);
	     Log.v(TAG, "onRestoreInstanceState");
	     Log.v(TAG, "state"+savedInstanceState);
	     
	     // we extract the state in onCreate (before this), so no need to extract it here too
	     //ExtractState(savedInstanceState);
	 }
	 
	 private void ExtractState(Bundle savedInstanceState) {
		 Log.v(TAG, "ExtractState");
		 
	     selectedItems = savedInstanceState.getParcelableArrayList("uk.co.islovely.cooking.selectedItems");
	     FinishTime = (Calendar)savedInstanceState.getSerializable("uk.co.islovely.cooking.FinishTime");
	     sortedSteps = savedInstanceState.getParcelableArrayList("uk.co.islovely.cooking.sortedSteps");
	     currentStepIndex = savedInstanceState.getInt("uk.co.islovely.cooking.currentStepIndex");
	   
//	     Log.v(TAG, "selectedItems " + selectedItems);
//	     Log.v(TAG, "FinishTime " + FinishTime);
//	     Log.v(TAG, "sortedSteps " + sortedSteps);
//	     Log.v(TAG, "currentStepIndex " + currentStepIndex);
	}

	public void StopCooking() {
		new AlertDialog.Builder(this)
        .setTitle(getString(R.string.back))
        .setMessage(getString(R.string.sure_stop_cooking))
        .setNegativeButton(android.R.string.no, null)
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int arg1) {
            	CancelAllAlarms();
            	
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        		notificationManager.cancelAll();

        		dialog.dismiss();
        		
        		Intent data = new Intent();
        		setResult(RESULT_CANCELED, data);
        		SummaryActivity.super.finish();
            }
        }).create().show();
		
	}
	
	public void FinishedCooking() {
		new AlertDialog.Builder(this)
        .setTitle(getString(R.string.eat))
        .setMessage(getString(R.string.nom))
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int arg1) {
            	CancelAllAlarms();
            	
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        		notificationManager.cancelAll();

        		dialog.dismiss();
        		
        		Intent data = new Intent();
	    		setResult(RESULT_OK, data);
	    		SummaryActivity.super.finish();
            }
        }).create().show();
		
	}
	
	public void OnCancel(View v) {
		StopCooking();
	}
	
	@Override
	public void onBackPressed() {
		StopCooking();
	}

	@Override
	public void onResume() {
		super.onResume();
		

	}

	@Override
	public void onPause() {
		super.onPause();

	}

}

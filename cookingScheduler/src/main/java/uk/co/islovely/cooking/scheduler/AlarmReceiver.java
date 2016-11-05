package uk.co.islovely.cooking.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
	private static final String TAG = "AlarmReceiver";
	
	static String key3 = "c85UrLtQzrIiCP7fbw9BF0ewLP0QdYIEUQqWenP1smEsf6JSzpJC9LulQBfugMB0";
	
	@Override
	public void onReceive(Context context, Intent incoming_intent) {
		Bundle extras=incoming_intent.getExtras();
	    extras.setClassLoader(getClass().getClassLoader());
	    
	    Bundle oldBundle = incoming_intent.getBundleExtra("uk.co.islovely.alarm");      

		RecipeStepData next_step_data = oldBundle.getParcelable("uk.co.islovely.alarm.step");
		Log.v(TAG, "onReceive " + next_step_data.toString());

		// set us up to trigger the next notification
		//SummaryActivity.startNextStep();
		Intent outgoing_intent = new Intent(context, SummaryActivity.class);
		outgoing_intent.putExtra("step", next_step_data);
		outgoing_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(outgoing_intent);   
	}

}

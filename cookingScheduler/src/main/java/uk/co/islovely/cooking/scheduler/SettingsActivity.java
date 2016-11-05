package uk.co.islovely.cooking.scheduler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;

public class SettingsActivity extends Activity {
	//private static final String TAG = "SettingsActivity";
	
	public static final int DEFAULT_FINISH_TIME_30_MINS = 0;
	public static final int DEFAULT_FINISH_TIME_1_HOUR = 1;
	public static final int DEFAULT_FINISH_TIME_ASAP = 2;
	
	 @Override
     public void onCreate(Bundle savedInstanceState) {         

        super.onCreate(savedInstanceState);    
        setContentView(R.layout.settings);
 
        Intent sender = getIntent();
		int default_finish_time = sender.getExtras().getInt("default_finish_time");
		boolean display_items_that_take_too_long = sender.getExtras().getBoolean("display_items_that_take_too_long");
		
        // set up current settings
        RadioGroup rg = (RadioGroup) findViewById(R.id.radioFinishTime);
        switch(default_finish_time) {
        case DEFAULT_FINISH_TIME_30_MINS:
        	rg.check(R.id.radio30minutes);
        	break;
        case DEFAULT_FINISH_TIME_1_HOUR:
        	rg.check(R.id.radio1hour);
        	break;
        case DEFAULT_FINISH_TIME_ASAP:
        	rg.check(R.id.radioasap);
        	break;
        }
        
        CheckBox cb = (CheckBox)findViewById(R.id.displayItemsThatTakeTooLong);
        cb.setChecked(display_items_that_take_too_long);
    }
	 
//	 @Override
//	 protected void onSaveInstanceState(Bundle state) {
//	     super.onSaveInstanceState(state);
//	     Log.v(TAG, "onSaveInstanceState");     
//	 }
//	 
//	 @Override
//	 protected void onRestoreInstanceState(Bundle savedInstanceState) {
//	     super.onRestoreInstanceState(savedInstanceState);
//	     Log.v(TAG, "onRestoreInstanceState");
//	     
//	     // we extract the state in onCreate (before this), so no need to extract it here too
//	     //ExtractState(savedInstanceState);
//	 }
//	 
//	 private void ExtractState(Bundle savedInstanceState) {
//	 }
	 
	 public void OnSaveSettings(View v) {
		 // Prepare data intent 
		 RadioGroup rg = (RadioGroup) findViewById(R.id.radioFinishTime);
		 int checked_radio = rg.getCheckedRadioButtonId();
		 int default_finish_time = DEFAULT_FINISH_TIME_30_MINS;
		 switch(checked_radio) {
		 case R.id.radio30minutes:
			 default_finish_time = DEFAULT_FINISH_TIME_30_MINS;
			 break;
		 case R.id.radio1hour:
			 default_finish_time = DEFAULT_FINISH_TIME_1_HOUR;
			 break;
		 case R.id.radioasap:
			 default_finish_time = DEFAULT_FINISH_TIME_ASAP;
			 break;
		 }
		 Intent data = new Intent();
		 data.putExtra("default_finish_time", default_finish_time);
		 
		 CheckBox cb = (CheckBox)findViewById(R.id.displayItemsThatTakeTooLong);
		 data.putExtra("display_items_that_take_too_long", cb.isChecked());
			  
		  // Activity finished ok, return the data
		  setResult(RESULT_OK, data);
		  super.finish();
	    	
	 }
	 
	 public void CancelSettings() {
			new AlertDialog.Builder(this)
		        .setTitle(getString(R.string.back))
		        .setMessage(getString(R.string.sure_cancel_settings))
		        .setNegativeButton(R.string.oops_no, null)
		        .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {

		            public void onClick(DialogInterface arg0, int arg1) {
		        		Intent data = new Intent();
		        		setResult(RESULT_CANCELED, data);
		        		SettingsActivity.super.finish();
		            }
		        }).create().show();
				
	 }

	 @Override
	 public void onBackPressed() {
		 CancelSettings();
	 }
	 
	 public void OnCancel(View v) {
		 CancelSettings();	
	 }
}

package uk.co.islovely.cooking.scheduler;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TimePicker;

public class FinishTimeDialogFragment extends DialogFragment {
    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
	public interface FinishTimeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNeutralClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }
    
    public FinishTimeDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    
    // Use this instance of the interface to deliver action events
    FinishTimeDialogListener mListener;
    private View DialogBodyView;
    
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (FinishTimeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement FinishTimeDialogListener");
        }
    }

    Calendar GetFinishTime() {
    	Calendar finish_time = new GregorianCalendar();
    	
    	TimePicker tp = (TimePicker) DialogBodyView.findViewById(R.id.finishTime);
        DatePicker dp = (DatePicker) DialogBodyView.findViewById(R.id.finishDate);
  
        finish_time.set(Calendar.YEAR, dp.getYear());
        finish_time.set(Calendar.MONTH, dp.getMonth());
        finish_time.set(Calendar.DAY_OF_MONTH, dp.getDayOfMonth());
        
        finish_time.set(Calendar.HOUR_OF_DAY, tp.getCurrentHour());
        finish_time.set(Calendar.MINUTE, tp.getCurrentMinute());
    
        return finish_time;
    }
    
    void SetFinishTime(Calendar finish_time) {
    	 TimePicker tp = (TimePicker) DialogBodyView.findViewById(R.id.finishTime);
         DatePicker dp = (DatePicker) DialogBodyView.findViewById(R.id.finishDate);
         tp.setCurrentHour(finish_time.get(Calendar.HOUR_OF_DAY));
         tp.setCurrentMinute(finish_time.get(Calendar.MINUTE));
         dp.init(finish_time.get(Calendar.YEAR), finish_time.get(Calendar.MONTH), finish_time.get(Calendar.DAY_OF_MONTH),
        	new DatePicker.OnDateChangedListener() {
             	public void onDateChanged(DatePicker view, int year, int month, int day) {
             		RadioGroup rg = (RadioGroup) DialogBodyView.findViewById(R.id.radioGroupFinishTime);
             		rg.check(R.id.manualFinishTime);
             	} // see also setOnTimeChangedListener
         	});
    }
    
    boolean FinishTimeIsASAP() {
    	// set up current settings
        RadioGroup rg = (RadioGroup) DialogBodyView.findViewById(R.id.radioGroupFinishTime);
        return (rg.getCheckedRadioButtonId() == R.id.asSoonAsPossible);
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	boolean as_soon_as_poss = getArguments().getBoolean("as_soon_as_possible");
    	long finish_time_millis = getArguments().getLong("finish_time");
    	Calendar finish_time = new GregorianCalendar();
    	finish_time.setTimeInMillis(finish_time_millis);
    	
    	LayoutInflater inflater = getActivity().getLayoutInflater();
    	DialogBodyView = inflater.inflate(R.layout.finish_time_dialog_body, null);
        
        // set up current settings
        RadioGroup rg = (RadioGroup) DialogBodyView.findViewById(R.id.radioGroupFinishTime);
        final TimePicker tp = (TimePicker) DialogBodyView.findViewById(R.id.finishTime);
        final DatePicker dp = (DatePicker) DialogBodyView.findViewById(R.id.finishDate);
        
        // set up a listener to expand and contract the time picker based on manual radio selection
        rg.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.asSoonAsPossible) {
                	tp.setVisibility(View.GONE);
                	dp.setVisibility(View.GONE);
                } else if (checkedId == R.id.manualFinishTime) {
                	tp.setVisibility(View.VISIBLE);
                	dp.setVisibility(View.VISIBLE);
                }
            }

        });
        
        if(as_soon_as_poss) {
        	rg.check(R.id.asSoonAsPossible);
        }
        else {
        	rg.check(R.id.manualFinishTime);
        }
        
        SetFinishTime(finish_time);
        
        // set up a listener to catch changes to finish time scroller (this will then select the manual finish radio) see also dp.init()
        
//        tp.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
//            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
//            	RadioGroup rg = (RadioGroup) DialogBodyView.findViewById(R.id.radioGroupFinishTime);
//            	rg.check(R.id.manualFinishTime);
//            }
//        });
        
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(DialogBodyView)
        	   .setTitle(R.string.finish_time)
        	   .setMessage(R.string.when_to_finish)
               .setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   // Send the positive button event back to the host activity
                       mListener.onDialogPositiveClick(FinishTimeDialogFragment.this);
                       
                   }
               })
               .setNeutralButton(R.string.reset_to_default_time, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   // Send the positive button event back to the host activity
                       mListener.onDialogNeutralClick(FinishTimeDialogFragment.this);
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   // Send the negative button event back to the host activity
                       mListener.onDialogNegativeClick(FinishTimeDialogFragment.this);
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
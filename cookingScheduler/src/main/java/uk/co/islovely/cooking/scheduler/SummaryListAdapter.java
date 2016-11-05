package uk.co.islovely.cooking.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

public class SummaryListAdapter extends ArrayAdapter<RecipeStepData> {
    private Context mContext;
    private int id;
    private ArrayList<RecipeStepData> items ;
    private float NormalTextViewSize = 0;
    
    public SummaryListAdapter(Context context, int textViewResourceId , ArrayList<RecipeStepData> list ) 
    {
        super(context, textViewResourceId, list);           
        mContext = context;
        id = textViewResourceId;
        items = list ;
    }

    private float GetNormalTextSize() {
    	if(NormalTextViewSize == 0) {
    		CheckedTextView dummy = new CheckedTextView(mContext);
    		NormalTextViewSize = dummy.getTextSize();
    	}
    	return NormalTextViewSize;
    }
    
    public void SetTextViewStyle(CheckedTextView textView, ListView lv, int position) {
		SparseBooleanArray checked_items = lv.getCheckedItemPositions();
		RecipeStepData current_step = SummaryActivity.GetPreviousStepData();
		RecipeStepData step_data = items.get(position);
    	
    	boolean currentStep = current_step == step_data;
    	boolean overdue = step_data.ReminderTime.getTimeInMillis() < System.currentTimeMillis();
    	boolean checked = checked_items.get(position);
    	
    	float default_textsize = GetNormalTextSize();
		float large_textsize = default_textsize * 1.5f;

		textView.setTypeface(null, Typeface.NORMAL);
		textView.setTextColor(Color.BLACK);
		
		if(currentStep) {
			textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, large_textsize);
	    	textView.setTypeface(null, Typeface.BOLD);
	    	
	    	if(checked) {
	    		textView.setTextColor(Color.GRAY);    
	    	}
	    	
	    	return;
		}
	    
		textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, default_textsize);
		
		if(checked) {
			textView.setTextColor(Color.GRAY);    
		}
		else if(overdue) {
			textView.setTextColor(Color.RED); 
		}
    }
    
    @Override
    public View getView(int position, View v, ViewGroup parent)
    {
        View mView = v ;
        if(mView == null){
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = vi.inflate(id, null);
        }

        RecipeStepData step_data = items.get(position);
        
        if(step_data == null)
        	return mView;
        
        String time_string = DateFormat.getTimeFormat(mContext).format(
				step_data.ReminderTime.getTimeInMillis());
		// check that we are dealing with today, if not display the date
		Calendar now = new GregorianCalendar();
		if (step_data.ReminderTime.get(Calendar.DATE) != now
				.get(Calendar.DATE)) {
			time_string = DateFormat.getDateFormat(mContext).format(
					step_data.ReminderTime.getTimeInMillis())
					+ " " + time_string;
		}
		String next_step_text = String.format(
				mContext.getString(R.string.next_step_text_format),
				time_string, step_data.menuItemName, step_data.step.Description);
		
		CheckedTextView  textView = (CheckedTextView)mView.findViewById(android.R.id.text1);
		textView.setText(next_step_text);
		
		ListView lv = (ListView)parent;
		SetTextViewStyle(textView, lv, position);

        return mView;
    }

}

package uk.co.islovely.cooking.scheduler;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;

public class RecipeStepData implements Parcelable {
	public RecipeStep step;
	public String menuItemName;
	public Calendar ReminderTime; // time at which to put up a reminder to start this step
	
	public RecipeStepData(RecipeStep st, String menu_item_name, Calendar reminder) {
		step = st;
		menuItemName = menu_item_name;
		ReminderTime = reminder;
	}
	
	public int describeContents() {
		return 0;
	}
	
	public String toString() {
		return "step:"+step.toString()+"\nmenuItemName:"+menuItemName+"\nReminderTime:"+GlobalApplication.formatCalendar(ReminderTime);
	}
	
	public String toString(Context context) {
		//return "step:"+step.toString()+"\nmenuItemName:"+menuItemName+"\nReminderTime:"+GlobalApplication.formatCalendar(ReminderTime);
		return DateFormat.getTimeFormat(context).format(ReminderTime.getTimeInMillis()) + ": " + menuItemName + " - " + step.toString();
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(step, flags);
		dest.writeString(menuItemName);
		dest.writeLong(ReminderTime.getTimeInMillis());
	}
	
	public static final Parcelable.Creator<RecipeStepData>CREATOR = new Parcelable.Creator<RecipeStepData>() {
		
		public RecipeStepData createFromParcel(Parcel source) {
			RecipeStep step = source.readParcelable(RecipeStep.class.getClassLoader());
			String name = source.readString();
			Calendar reminder_time = new GregorianCalendar();
			reminder_time.setTimeInMillis(source.readLong());
			return new RecipeStepData(step, name, reminder_time);
		}
		
		public RecipeStepData[] newArray(int size) {
			return new RecipeStepData[size];
		}
	};
}
package uk.co.islovely.cooking.scheduler;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

class FinishTime implements Parcelable {
	private String TAG = "finishTime";
	boolean ASAP;
	Calendar mFinishTime;

	FinishTime(boolean asap) {
		ASAP = true;
		mFinishTime = null;
	}

	FinishTime(Calendar finish_time) {
		ASAP = false;
		mFinishTime = finish_time;
	}

	public String toString() {
		return ASAP ? "ASAP" : mFinishTime.toString();
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(ASAP);
		if(!ASAP) {
			dest.writeLong(mFinishTime.getTimeInMillis());
		}
	}
	
	public static final Parcelable.Creator<FinishTime> CREATOR = new Parcelable.Creator<FinishTime>() {
		
		public FinishTime createFromParcel(Parcel source) {
//			boolean asap = (Boolean) source.readValue(null);
//			if(asap) {
//				return new FinishTime(asap);
//			}
//			
//			Calendar finish_time = new GregorianCalendar();
//			long finish_time_millis = source.readLong();
//			finish_time.setTimeInMillis(finish_time_millis);
			return new FinishTime(source);
		}
		
		public FinishTime[] newArray(int size) {
			return new FinishTime[size];
		}
	};
	
	private FinishTime(Parcel source) {
		ASAP = (Boolean) source.readValue(null);
		if(!ASAP) {
			long finish_time_millis = source.readLong();
			mFinishTime = new GregorianCalendar();
			mFinishTime.setTimeInMillis(finish_time_millis);
		}
     }
}

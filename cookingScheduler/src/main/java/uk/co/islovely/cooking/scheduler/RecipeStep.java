package uk.co.islovely.cooking.scheduler;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class RecipeStep implements Parcelable {
	public String Description;
	public int TimeTaken; 
	public static final int INVALID_TIME = -1;
	
	public RecipeStep(String desc, int time)
	{
		Description = desc;
		TimeTaken = time;
	}
	
	public RecipeStep(JSONObject obj) {
		try {
			Description = obj.getString("Description");
			TimeTaken = obj.getInt("TimeTaken");
		} catch (JSONException e) {
			GlobalApplication.Assert(false, e);
		}
	}
	
	public JSONObject getJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("Description", Description);
            obj.put("TimeTaken", TimeTaken);
        } catch (JSONException e) {
        	GlobalApplication.Assert(false, e);
        }
        return obj;
    }
	
	public String toString() {
		return Description;
		//return "Description:"+Description+"\nTimeTaken:"+TimeTaken;
	}
	
	public boolean Matches(RecipeStep rhs) {
		if(Description.compareTo(rhs.Description) != 0) {
			return false;
		}
		
		if(TimeTaken != rhs.TimeTaken)
			return false;
		
		return true;
	}
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(Description);
		dest.writeInt(TimeTaken);
	}
	
	public static final Parcelable.Creator<RecipeStep>CREATOR = new Parcelable.Creator<RecipeStep>() {
		
		public RecipeStep createFromParcel(Parcel source) {
			String description = source.readString();
			int time = source.readInt();
			return new RecipeStep(description, time);
		}
		
		public RecipeStep[] newArray(int size) {
			return new RecipeStep[size];
		}
	};
}

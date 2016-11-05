package uk.co.islovely.cooking.scheduler;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class MenuItem implements Parcelable {
	public String Name;
	public ArrayList<RecipeStep> Steps;
	
	public MenuItem(String name, ArrayList<RecipeStep> steps) {
		Name = name;
		Steps = steps;
	}
	
	public MenuItem(JSONObject obj) {
		try {
			Name = obj.getString("Name");
			Steps = new ArrayList<RecipeStep>();
			JSONArray jsArray = obj.getJSONArray("Steps");
			for (int i=0;i<jsArray.length();i++) {	
				RecipeStep step = new RecipeStep(jsArray.getJSONObject(i));
				Steps.add(step);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int GetTotalTime() {
		int time = 0;
		for (RecipeStep step : Steps) {
			if(step.TimeTaken == RecipeStep.INVALID_TIME)
				continue;
			
			time += step.TimeTaken;
		}
		
		return time;
	}
	
	public String toString() {
		return Name + " (" + GetTotalTime() + " mins)";
	}
	
	public JSONObject getJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("Name", Name);
            JSONArray jsArray = new JSONArray();
			for (int i=0;i<Steps.size();i++) {
				jsArray.put(Steps.get(i).getJSONObject());
			}
            obj.put("Steps", jsArray);
        } catch (JSONException e) {
        	e.printStackTrace();
        }
        return obj;
    }
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(Name);
		dest.writeList(Steps);
	}
	
	public static final Parcelable.Creator<MenuItem> CREATOR = new Parcelable.Creator<MenuItem>() {
		
		public MenuItem createFromParcel(Parcel source) {
			String name = source.readString();
			ArrayList<RecipeStep> steps = new ArrayList<RecipeStep>();
			source.readList(steps, RecipeStep.class.getClassLoader());
			return new MenuItem(name, steps);
		}
		
		public MenuItem[] newArray(int size) {
			return new MenuItem[size];
		}
	};
}

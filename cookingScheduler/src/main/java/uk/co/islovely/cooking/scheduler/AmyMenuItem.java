package uk.co.islovely.cooking.scheduler;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class AmyMenuItem implements Parcelable {
	public String Name;
	public ArrayList<RecipeStep> Steps;
	static String key4 = "osj4h1fUbe4goILZnHFuDwvoaTPi+HgXpl/+rtT8suyXvGlZ5V1wj7ooXhrtuUsSlPl7s+P/Uc1zCIswiQtsIfhm0R0h9IIzartJq";
	
	public AmyMenuItem(String name, ArrayList<RecipeStep> steps) {
		Name = name;
		Steps = steps;
	}
	
	public AmyMenuItem(JSONObject obj) {
		try {
			Name = obj.getString("Name");
			Steps = new ArrayList<RecipeStep>();
			JSONArray jsArray = obj.getJSONArray("Steps");
			for (int i=0;i<jsArray.length();i++) {	
				RecipeStep step = new RecipeStep(jsArray.getJSONObject(i));
				Steps.add(step);
			}
		} catch (JSONException e) {
			GlobalApplication.Assert(false, e);
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
		CharSequence item_string = String
				.format(MainActivity.getContext().getString(R.string.menu_item_string_format),
						Name, GetTotalTime());		
		
		return item_string.toString();
	}
	
	public String DebugPrint() {
		String debug_string = toString() + ":\n";
		for(RecipeStep step : Steps) {
			debug_string += step.toString() + "\n";
		}
		return  debug_string;
	}
	
	public boolean Matches(AmyMenuItem rhs) {
		if(Name.compareTo(rhs.Name) != 0) {
			return false;
		}
		
		if(Steps.size() != rhs.Steps.size())
			return false;
		
		for(int i=0; i<Steps.size(); ++i) {
			if(!Steps.get(i).Matches(rhs.Steps.get(i)))
				return false;
		}
		
		return true;
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
        	GlobalApplication.Assert(false, e);
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
	
	public static final Parcelable.Creator<AmyMenuItem> CREATOR = new Parcelable.Creator<AmyMenuItem>() {
		
		public AmyMenuItem createFromParcel(Parcel source) {
			String name = source.readString();
			ArrayList<RecipeStep> steps = new ArrayList<RecipeStep>();
			source.readList(steps, RecipeStep.class.getClassLoader());
			return new AmyMenuItem(name, steps);
		}
		
		public AmyMenuItem[] newArray(int size) {
			return new AmyMenuItem[size];
		}
	};
}

package uk.co.islovely.cooking.scheduler;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class RecipeStepAdapter extends ArrayAdapter<RecipeStep> {

	// declaring our ArrayList of items
	private ArrayList<RecipeStep> objects;

	/* here we must override the constructor for ArrayAdapter
	* the only variable we care about now is ArrayList<Item> objects,
	* because it is the list of objects we want to display.
	*/
	public RecipeStepAdapter(Context context, int textViewResourceId, ArrayList<RecipeStep> objects) {
		super(context, textViewResourceId, objects);
		this.objects = objects;
	}
    
	
	/*
	 * we are overriding the getView method here - this is what defines how each
	 * list item will look.
	 */
	public View getView(int position, View convertView, ViewGroup parent){
	
		// assign the view we are converting to a local variable
		View v = convertView;

		// first check to see if the view is null. if so, we have to inflate it.
		// to inflate it basically means to render, or show, the view.
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.recipe_step, parent, false);
		}

		//v.getBackground().setLevel(3000);
		
		if(position >= objects.size()) {
			return v;
		}
		
		/*
		 * Recall that the variable position is sent in as an argument to this method.
		 * The variable simply refers to the position of the current object in the list. (The ArrayAdapter
		 * iterates through the list we sent it)
		 * 
		 * Therefore, i refers to the current Item object.
		 */
		RecipeStep i = objects.get(position);

		if (i != null) {

			// This is how you obtain a reference to the TextViews.
			// These TextViews are created in the XML files we defined.

			TextView name = (TextView) v.findViewById(R.id.recipeStepName);
			TextView time = (TextView) v.findViewById(R.id.recipeStepTime);
			
			// check to see if each individual textview is null.
			// if not, assign some text!
			if (name != null){
				name.setText(i.Description);
			}
			if (time != null){
				time.setText(""+i.TimeTaken);
			}
		}

		// the view must be returned to our activity
		return v;

	}

}

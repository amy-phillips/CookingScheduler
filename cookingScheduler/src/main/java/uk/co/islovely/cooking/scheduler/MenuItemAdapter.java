package uk.co.islovely.cooking.scheduler;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class MenuItemAdapter extends ArrayAdapter<AmyMenuItem> {

	// declaring our ArrayList of items
	private ArrayList<AmyMenuItem> objects;
	private MenuItemAdapterCallback menuItemCallback;
	
	public interface MenuItemAdapterCallback {
        public void editPressed(int position);
        public void deletePressed(int position);
        public void sharePressed(int position);
    }
	public void setCallback(MenuItemAdapterCallback callback){
		menuItemCallback = callback;
    }
	
	/* here we must override the constructor for ArrayAdapter
	* the only variable we care about now is ArrayList<Item> objects,
	* because it is the list of objects we want to display.
	*/
	public MenuItemAdapter(Context context, int textViewResourceId, ArrayList<AmyMenuItem> objects) {
		super(context, textViewResourceId, objects);
		this.objects = objects;
	}
    
	
	/*
	 * we are overriding the getView method here - this is what defines how each
	 * list item will look.
	 */
	public View getView(final int position, View convertView, ViewGroup parent){
	
		// assign the view we are converting to a local variable
		View v = convertView;

		// first check to see if the view is null. if so, we have to inflate it.
		// to inflate it basically means to render, or show, the view.
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.menu_item, parent, false);
		}

		//v.getBackground().setLevel(3000);
		
		if(position >= objects.size()) {
			return v;
		}
		
		ImageView edit = (ImageView)v.findViewById(R.id.editMenuItem);
		ImageView share = (ImageView)v.findViewById(R.id.shareMenuItem);
		ImageView delete = (ImageView)v.findViewById(R.id.deleteMenuItem);
		TextView name = (TextView)v.findViewById(R.id.menuItemName);
		
		if(edit != null) {
			edit.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View arg0) {
	            	if(menuItemCallback != null) {
	            		menuItemCallback.editPressed(position);
	            	}
	            }
	        });
		}
		// clicking on the item name goes to edit too
		if(name != null) {
			name.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View arg0) {
	            	if(menuItemCallback != null) {
	            		menuItemCallback.editPressed(position);
	            	}
	            }
	        });
		}
		
		if(share != null) {
			share.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View arg0) {
	            	if(menuItemCallback != null) {
	            		menuItemCallback.sharePressed(position);
	            	}
	            }
	        });
		}
		
		if(delete != null) {
			delete.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View arg0) {
	            	if(menuItemCallback != null) {
	            		menuItemCallback.deletePressed(position);
	            	}
	            }
	        });
		}
       
		/*
		 * Recall that the variable position is sent in as an argument to this method.
		 * The variable simply refers to the position of the current object in the list. (The ArrayAdapter
		 * iterates through the list we sent it)
		 * 
		 * Therefore, i refers to the current Item object.
		 */
		AmyMenuItem i = objects.get(position);

		if (i != null) {

			// This is how you obtain a reference to the TextViews.
			// These TextViews are created in the XML files we defined.

			//TextView name = (TextView) v.findViewById(R.id.menuItemName);
			
			// check to see if each individual textview is null.
			// if not, assign some text!
			if (name != null){	
				name.setText(i.toString());
			}
		}

		// the view must be returned to our activity
		return v;

	}

}

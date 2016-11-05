package uk.co.islovely.cooking.scheduler;

import java.util.ArrayList;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

public class AddMenuItemActivity extends ActionBarActivity {
	private static final String TAG = "AddMenuItemActivity";
	private ArrayList<RecipeStep> recipeSteps=new ArrayList<RecipeStep>();
	RecipeStepAdapter adapter;

	boolean editingItem;
	String ParsedURL;
	
	private DragSortListView mDslv;
    private DragSortController mController;
    
	static String key2 = "//CYIdmRS7yAJvs9RHSu42JbzSskqWeuaQiXFcuQ7fETG5Uq1kk2cLlXSY40OGWBDpCdX2aO";
	
    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    if (from != to) {
                    	RecipeStep item = adapter.getItem(from);
                        adapter.remove(item);
                        adapter.insert(item, to);
                    }
                }
            };

    private DragSortListView.RemoveListener onRemove = 
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {
                    adapter.remove(adapter.getItem(which));
                    UpdateAddStepsMessageVisibility();
                }
            };
   
            
            
    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    public DragSortController buildController(DragSortListView dslv) {
        // defaults are
        //   dragStartMode = onDown
        //   removeMode = flingRight
        DragSortController controller = new DragSortController(dslv);
        controller.setDragHandleId(R.id.drag_handle);
        controller.setClickRemoveId(R.id.click_remove);
        controller.setRemoveEnabled(true);
        controller.setSortEnabled(true);
        controller.setDragInitMode(DragSortController.ON_DRAG);
        controller.setRemoveMode(DragSortController.CLICK_REMOVE);
        return controller;
    }  
    

    
	 @Override
     public void onCreate(Bundle savedInstanceState) {         

		Log.d(TAG, "OnCreate " + savedInstanceState);
        super.onCreate(savedInstanceState);    
        
        setContentView(R.layout.add_or_edit_menu_item);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(false);
        
        mDslv = (DragSortListView) this.findViewById(R.id.recipeStepsListView);
        
        mController = buildController(mDslv);
        mDslv.setFloatViewManager(mController);
        mDslv.setOnTouchListener(mController);
        mDslv.setDragEnabled(true);

        mDslv.setDropListener(onDrop);
        mDslv.setRemoveListener(onRemove);
        mDslv.setOnItemClickListener(new OnItemClickListener() {
        	   @Override
        	   public void onItemClick(AdapterView<?> adapterview, View view, int position, long arg) {
        		   final RecipeStep step = (RecipeStep) mDslv.getItemAtPosition(position);
        		   
        		   DoAddStepDialog(step, R.string.edit);	   
        	   } 
        	});
        
        // only extract the item we're editing if we're not resuming, 
        // otherwise we rely on the restored state in case the user changed anything
        if(savedInstanceState == null) {
	        editingItem = false;
	        ParsedURL = null;
	        Intent sender = getIntent();
			if (sender.hasExtra("menu_item")) // we are editing a menu item rather than adding a new one
			{
				editingItem = true;
				AmyMenuItem item_to_edit = sender.getParcelableExtra("menu_item");
				for(RecipeStep step: item_to_edit.Steps) {
					recipeSteps.add(new RecipeStep(step.Description, step.TimeTaken));
				}
				ab.setTitle(item_to_edit.Name);
		        //ab.setSubtitle("sub-title");
				
				Log.d(TAG, "editing item " + item_to_edit.Name);
			}
			else {
				// let's try and get a name for this new item
				DoRenameItemDialog(R.string.new_item_name);
			}
	        
	        Button report_button = (Button)findViewById(R.id.failedParse);
	        if(sender.hasExtra("parsed_url")) {
	        	ParsedURL = sender.getExtras().getString("parsed_url");
	        	report_button.setVisibility(View.VISIBLE);
	        }
	        else {
	        	// hide the report button because we didn't parse a url
	        	report_button.setVisibility(View.GONE);
	        }
	        
	        UpdateAddStepsMessageVisibility();
        }
        else {
        	// we grab all the stored data and bosh it back into place
        	ExtractState(savedInstanceState);
        }
  
        adapter = new RecipeStepAdapter(this, R.id.recipeStepName, recipeSteps);
        mDslv.setAdapter(adapter);
    }
	 
	 void UpdateAddStepsMessageVisibility() {
		// if there are no steps display a message saying how to add steps
        TextView add_steps_message = (TextView)findViewById(R.id.add_steps_instructions);
        if(recipeSteps.isEmpty()) {
        	String button_tag = "BUTTON";
        	String instructions = getString(R.string.to_add_steps_press_button);
        	SpannableString ss = new SpannableString(instructions); 
            Drawable d = getResources().getDrawable(R.drawable.ic_action_add_to_queue); 
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight()); 
            ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM); 
            int offset = instructions.indexOf(button_tag);
            ss.setSpan(span, offset, offset+button_tag.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE); 
            add_steps_message.setText(ss); 

        	add_steps_message.setVisibility(View.VISIBLE);
        } else {
        	add_steps_message.setVisibility(View.GONE);
        }
	 }
	
	 @Override
	 public boolean onCreateOptionsMenu(Menu menu) {
	     // Inflate the menu items for use in the action bar
	     MenuInflater inflater = getMenuInflater();
	     inflater.inflate(R.menu.add_menu_item_menu, menu);
	     return super.onCreateOptionsMenu(menu);
	 }
	 
	 @Override
	public void onPause () {
		 // TODO save user edits to storage
		 super.onPause();
	 }
	 
	 @Override
	 protected void onSaveInstanceState(Bundle state) {
	     super.onSaveInstanceState(state);
	     Log.v(TAG, "onSaveInstanceState");

	     String recipe_name = getSupportActionBar().getTitle().toString();
	     state.putSerializable("uk.co.islovely.cooking.name", recipe_name);
	     state.putSerializable("uk.co.islovely.cooking.editingItem", editingItem);
	     state.putSerializable("uk.co.islovely.cooking.ParsedURL", ParsedURL);
	     state.putParcelableArrayList("uk.co.islovely.cooking.steps", recipeSteps);
	     
	     Log.v(TAG, "editingItem " + editingItem);
	     Log.v(TAG, "ParsedURL " + ParsedURL);
	     Log.v(TAG, "name " + recipe_name);
	     Log.v(TAG, "steps " + recipeSteps);
	     
	 }
	 
	 @Override
	 protected void onRestoreInstanceState(Bundle savedInstanceState) {
	     super.onRestoreInstanceState(savedInstanceState);
	     Log.v(TAG, "onRestoreInstanceState");
	     
	     // we extract the state in onCreate (before this), so no need to extract it here too
	     //ExtractState(savedInstanceState);
	 }
	 
	 private void ExtractState(Bundle savedInstanceState) {
	     String name = savedInstanceState.getString("uk.co.islovely.cooking.name");
	     getSupportActionBar().setTitle(name);
	     
	     editingItem = savedInstanceState.getBoolean("uk.co.islovely.cooking.editingItem");
	     ParsedURL = savedInstanceState.getString("uk.co.islovely.cooking.ParsedURL");
	     recipeSteps = savedInstanceState.getParcelableArrayList("uk.co.islovely.cooking.steps");
	     
	     Button report_button = (Button)findViewById(R.id.failedParse);
	     if(ParsedURL == null) {
	    	 report_button.setVisibility(View.GONE);
	     }
	     else {
	    	 report_button.setVisibility(View.VISIBLE);
	     }
	     
	     UpdateAddStepsMessageVisibility();
	     
	     Log.v(TAG, "editingItem " + editingItem);
	     Log.v(TAG, "ParsedURL " + ParsedURL);
	     Log.v(TAG, "name " + name);
	     Log.v(TAG, "steps " + recipeSteps);
	 }

	 @Override
	 public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_save:
			OnAddItem();
			return true;
		case R.id.action_cancel:
			CancelAddItem();
			return true;
		case android.R.id.home:
		case R.id.action_rename:
			DoRenameItemDialog(R.string.rename);
			return true;
		case R.id.menu_add_step:
			AddStep();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	 }
	 
	 public void AddStep() {
		 recipeSteps.add(new RecipeStep("", 1)); 
		 adapter.notifyDataSetChanged();
		 
		 //pop up a dialog to edit the new step
		 DoAddStepDialog(recipeSteps.get(recipeSteps.size()-1), R.string.new_step);
	 }
	 
	 public void OnAddItem() {
		 // Prepare data intent 
		  Intent data = new Intent();
		  data.putExtra("name", getSupportActionBar().getTitle().toString());
		  data.putParcelableArrayListExtra("steps", recipeSteps);
		  
		  // Activity finished ok, return the data
		  setResult(RESULT_OK, data);
		  super.finish();
	    	
	 }
	 
	 public void CancelAddItem() {
		 int sure_cancel = editingItem ? R.string.sure_cancel_edit_item : R.string.sure_cancel_add_item;
		 new AlertDialog.Builder(this)
	        .setTitle(getString(R.string.back))
	        .setMessage(getString(sure_cancel))
	        .setNegativeButton(R.string.oops_no, null)
	        .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {

	            public void onClick(DialogInterface dialog, int arg1) {
	            	dialog.dismiss();
	            	
	        		Intent data = new Intent();
	        		setResult(RESULT_CANCELED, data);
	        		AddMenuItemActivity.super.finish();
	            }
	        }).create().show();
			
	 }
	 
	 private void DoRenameItemDialog(int title) {
    	 
		   ContextThemeWrapper wrapper = new ContextThemeWrapper(AddMenuItemActivity.this, R.style.DialogBaseTheme);
		   LayoutInflater localInflater = LayoutInflater.from(getApplicationContext()).cloneInContext(wrapper);
		   
		   final View alertView = localInflater.inflate(R.layout.rename_item_dialog, null); // null is fine in this instance http://www.doubleencore.com/2013/05/layout-inflation-as-intended/ 
		   
		   EditText name = (EditText) alertView.findViewById(R.id.recipeName);
		   name.setText(getSupportActionBar().getTitle());   
		   name.setSelection(name.getText().length());
		   name.setFocusableInTouchMode(true);
		   name.setFocusable(true);
		   name.requestFocus();
		   
		   AlertDialog.Builder builder = new AlertDialog.Builder(wrapper);
		   builder.setTitle(getString(title))
		   		.setView(alertView)
	        	.setNegativeButton(R.string.oops_no, null)
	        	.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	        		public void onClick(DialogInterface dialog, int arg1) {		            	
	 	            	EditText name = (EditText) alertView.findViewById(R.id.recipeName);
	 	            	getSupportActionBar().setTitle(name.getText());
	 	            	dialog.dismiss();
	 	            }
 	        });
		   
		   AlertDialog dialog = builder.create();
		   dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE); // bring up keyboard
		   dialog.show();
	 }
	 
	 private void DoAddStepDialog(final RecipeStep step, int title) {
		 ContextThemeWrapper wrapper = new ContextThemeWrapper(AddMenuItemActivity.this, R.style.DialogBaseTheme);
		   LayoutInflater localInflater = LayoutInflater.from(getApplicationContext()).cloneInContext(wrapper);
		   
		   final View alertView = localInflater.inflate(R.layout.edit_menu_item_step, null); // null is fine in this instance http://www.doubleencore.com/2013/05/layout-inflation-as-intended/ 
		   
		   EditText name = (EditText) alertView.findViewById(R.id.editRecipeStepName);
		   name.setText(step.Description);
		   name.setSelection(name.getText().length());
		   name.setFocusableInTouchMode(true);
		   name.setFocusable(true);
		   name.requestFocus();
//		   InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
//		   imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT);
//		   getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		   
		   EditText time = (EditText) alertView.findViewById(R.id.editRecipeStepTime);
		   time.setText(""+step.TimeTaken);
		   
		   AlertDialog.Builder builder = new AlertDialog.Builder(wrapper);
		   builder.setTitle(getString(title))
		   		.setView(alertView)
	        	.setNegativeButton(R.string.oops_no, null)
	        	.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
	        		public void onClick(DialogInterface dialog, int arg1) {  	
	  	            	EditText name = (EditText) alertView.findViewById(R.id.editRecipeStepName);
	  	            	step.Description = name.getText().toString();
	        		    EditText time = (EditText) alertView.findViewById(R.id.editRecipeStepTime);
	        		    step.TimeTaken = Integer.parseInt(time.getText().toString());     		    
	        		    
	        		    adapter.notifyDataSetChanged();
	        		    UpdateAddStepsMessageVisibility();
	        		    
	        		    dialog.dismiss();
	  	            }
  	        });
		   
		   AlertDialog dialog = builder.create();
		   dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE); //bring up keyboard
		   dialog.show();
	 }
	 
	 @Override
	 public void onBackPressed() {
		 CancelAddItem();
	 }
	 
	 public void onReportToDeveloper(View v) {
		// launch into email program
		/* Create the Intent */
		 final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

		 /* Fill it with Data */
		 emailIntent.setType("plain/text");
		 emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"cooking@thinkysaurus.com"});
		 emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Failed to parse recipe");
		 emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hello Amy!\nI just tried to parse the recipe at "+ParsedURL+", but it's not come out quite how I expected.\nPlease could you take a look, and fix it for me?\nThank you!\n");

		 /* Send it off to the Activity-Chooser */
		 startActivity(Intent.createChooser(emailIntent, "Send mail..."));
	 }
}

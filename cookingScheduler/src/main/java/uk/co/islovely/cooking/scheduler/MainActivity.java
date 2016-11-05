package uk.co.islovely.cooking.scheduler;

// TODO 
//check different phones/layouts

// TODO later
// add pictures to recipe steps/recipes
// search option
// organising recipes
// meat cooking times based on weight
// shared recipes on server

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements
		FinishTimeDialogFragment.FinishTimeDialogListener,
		MenuItemAdapter.MenuItemAdapterCallback,
		ActivityCompat.OnRequestPermissionsResultCallback {
	public static final String BROADCAST_ACTION = "uk.co.islovely.cooking.scheduler.time.picker";
	private static final String TAG = "MainActivity";
	static String keyfinal = "X+ILAW+IZ6uPwD5ZUGMPHTejNWNLQYdwMChS7rE9dBVLgTf3S0Nbi/gwIDAQAB";

	static ArrayList<AmyMenuItem> menuItems = new ArrayList<AmyMenuItem>();
	static ArrayList<AmyMenuItem> disabledMenuItems = new ArrayList<AmyMenuItem>();

	// DEFINING STRING ADAPTER WHICH WILL HANDLE DATA OF LISTVIEW
	static ArrayAdapter<AmyMenuItem> adapter;
	static int DefaultFinishTime = SettingsActivity.DEFAULT_FINISH_TIME_30_MINS;
	static boolean DisplayItemsThatTakeTooLong = false;
	static int WhatsChangedVersionDisplayed = 0;
	private static Context mContext;
	
	InAppBilling mInAppBilling = new InAppBilling();
	ProgressDialog InAppBillingPurchasingDialog;
	boolean InAppBillingForCookingTwoThingsAtOnce = true;
	String URLToParse;
	
	Tab cookTab;
	Tab editTab;
	
	Intent LaunchSummaryActivityIntent;
	private ProgressDialog parsing_recipe_progress;
	private int replaceItemDialogCount = 0;
	private int currentEditItemIndex = -1;
	
	public static final String SAVED_MENU_ITEMS_FILENAME = "menu_items.json";
	static final Object sDataLock = new Object(); // lock to prevent backup and
													// file read/write stepping
													// on each other's toes

	static final int ADD_MENU_ITEM = 1;
	static final int EDIT_MENU_ITEM = 2;

	static final int START_COOKING = 3;

	static final int SETTINGS = 4;

	static final int ABOUT = 5;

	private JSONObject MenuItemsToJSON(ArrayList<AmyMenuItem> menu_items)
			throws JSONException {
		JSONArray menu_items_json = new JSONArray();
		for (int i = 0; i < menu_items.size(); i++) {
			menu_items_json.put(menu_items.get(i).getJSONObject());
		}

		JSONObject json_to_write = new JSONObject();
		json_to_write.put("menu_items", menu_items_json);
		return json_to_write;
	}

	private void shareMenuItems(ArrayList<AmyMenuItem> menu_items) {
		try {
			JSONObject json_to_write = MenuItemsToJSON(menu_items);

			String FILENAME = "recipes.json";
			if (menu_items.size() == 1 && menu_items.get(0).Name.length() > 0) {
				FILENAME = menu_items.get(0).Name.replaceAll("[^a-zA-Z0-9.-]",
						"_") + ".json";
			}

			// to send as an attachment it must be saved as a file
			String filepath = WriteToExternalStoragePrivateFile(FILENAME, json_to_write);
            if(filepath == null)
                return;

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
// The intent does not have a URI, so declare the "text/plain" MIME type
            emailIntent.setType(HTTP.PLAIN_TEXT_TYPE);
            //emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"jon@example.com"}); // recipients
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
            //emailIntent.putExtra(Intent.EXTRA_TEXT, "Email message text");
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filepath)));
// You can also attach multiple items by passing an ArrayList of Uris

//			Intent sharingIntent = new Intent(
//					android.content.Intent.ACTION_SEND);
//			sharingIntent.setType("application/json");
//			sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
//					getString(R.string.share_subject));
//			sharingIntent.putExtra(android.content.Intent.EXTRA_STREAM, uri);

            startActivity(Intent.createChooser(emailIntent , "Send email..."));
		} catch (JSONException e) {
			GlobalApplication.Assert(false, e);
		}
	}

    private String WriteToExternalStoragePrivateFile(String filename,
                                                  JSONObject json_to_write) {
        // Create a path where we will place our private file on external
        // storage.
        File file = new File(getExternalFilesDir(null), filename);

        try {
            // Very simple code to copy a picture from the application's
            // resource into the external file.  Note that this code does
            // no error checking, and assumes the picture is small (does not
            // try to copy it in chunks).  Note that if external storage is
            // not currently mounted this will silently fail.
            OutputStream os = new FileOutputStream(file);
            os.write(json_to_write.toString().getBytes());
            os.close();

            return file.toString();
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + file, e);
        }

        return null;
    }

	private void SaveMenuItems() {

		ArrayList<AmyMenuItem> allMenuItems = new ArrayList<AmyMenuItem>();
		allMenuItems.addAll(menuItems);
		allMenuItems.addAll(disabledMenuItems);

		// save out all the current menu items
		try {
			JSONObject json_to_write = MenuItemsToJSON(allMenuItems);

//			ArrayList<AmyMenuItem> selected_items = GetCookTabFragment().GetSelectedMenuItems();
//			JSONArray selected_items_json = new JSONArray();
//			for (int i = 0; i < selected_items.size(); i++) {
//				selected_items_json.put(selected_items.get(i).getJSONObject());
//			}
//
//			json_to_write.put("selected_items", selected_items_json);
			json_to_write.put("default_finish_time", DefaultFinishTime);
			json_to_write.put("display_items_that_take_too_long",
					DisplayItemsThatTakeTooLong);
			json_to_write.put("whats_changed_version_displayed", WhatsChangedVersionDisplayed);

            synchronized (MainActivity.sDataLock) {
                Log.v(TAG, "SaveMenuItems to "
                        + getFileStreamPath(SAVED_MENU_ITEMS_FILENAME));
                FileOutputStream out_file = openFileOutput(
                        SAVED_MENU_ITEMS_FILENAME, MODE_PRIVATE);
                out_file.write(json_to_write.toString().getBytes());
                out_file.close();
            }

		} catch (FileNotFoundException e) {
			GlobalApplication.Assert(false, e);
		} catch (IOException e) {
			GlobalApplication.Assert(false, e);
		} catch (JSONException e) {
			GlobalApplication.Assert(false, e);
		}

		// request a backup
		BackupManager bm = new BackupManager(this);
		bm.dataChanged();
	}

	private String LoadFileIntoString(InputStream in) {
		Writer writer = new StringWriter();
		char[] buffer = new char[1024];
		try {
			Reader reader;
			reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
		} catch (UnsupportedEncodingException e) {
			GlobalApplication.Assert(false, e);
		} catch (IOException e) {
			GlobalApplication.Assert(false, e);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				GlobalApplication.Assert(false, e);
			}
		}
		return writer.toString();
	}

	ArrayList<AmyMenuItem> LoadMenuItemsFromJSON(JSONObject json)
			throws JSONException {
		
		//GlobalApplication.Assert(false, "testing acra...");

		ArrayList<AmyMenuItem> menu_items = new ArrayList<AmyMenuItem>();

		JSONArray menu_items_json = json.getJSONArray("menu_items");
		if (menu_items_json != null) {
			int len = menu_items_json.length();
			for (int i = 0; i < len; i++) {
				JSONObject obj = menu_items_json.getJSONObject(i);
				menu_items.add(new AmyMenuItem(obj));
			}
		}

		return menu_items;
	}

	void LoadDefaultSettingsFromString(String json_string) throws JSONException {
		JSONObject json = new JSONObject(json_string);

		menuItems.addAll(LoadMenuItemsFromJSON(json));

		// don't save selected items - we'll do it on pause and resume, but not between
		// instances - doesn't make sense and the fragment isn't loaded yet anyway!
//		JSONArray selected_items_json = json.getJSONArray("selected_items");
//		if (selected_items_json != null) {
//			ListView lv = (ListView) findViewById(android.R.id.list);
//
//			for (int i = 0; i < selected_items_json.length(); i++) {
//				try {
//					JSONObject obj = selected_items_json.getJSONObject(i);
//					AmyMenuItem checked_item = new AmyMenuItem(obj);
//
//					// find where this item is in the list
//					for (int item_index = 0; item_index < lv.getAdapter()
//							.getCount(); item_index++) {
//						AmyMenuItem menu_item = (AmyMenuItem) lv.getAdapter()
//								.getItem(item_index);
//						if (menu_item.Matches(checked_item)) {
//							lv.setItemChecked(item_index, true);
//							break;
//						}
//					}
//
//				} catch (JSONException e) {
//					GlobalApplication.Assert(false, e);
//				}
//			}
//		}

		DefaultFinishTime = json.optInt("default_finish_time", SettingsActivity.DEFAULT_FINISH_TIME_ASAP);
		DisplayItemsThatTakeTooLong = json.optBoolean("display_items_that_take_too_long", false);
		WhatsChangedVersionDisplayed = json.optInt("whats_changed_version_displayed", 0);
	}

	private void LoadSavedSettings() {

		// we load everything into menuItems, then move the disabled ones out
		// later
		menuItems.clear();
		disabledMenuItems.clear();

		// load any saved menu items
		try {
			String jsonString = "";
			synchronized (MainActivity.sDataLock) {
				Log.v(TAG, "Trying to LoadSavedSettings from "
						+ getFileStreamPath(SAVED_MENU_ITEMS_FILENAME));
				FileInputStream in = openFileInput(SAVED_MENU_ITEMS_FILENAME);
				jsonString = LoadFileIntoString(in);
			}
			LoadDefaultSettingsFromString(jsonString);
		} catch (FileNotFoundException e) {
			// no worries - no saved items
			LoadDefaultSettings();
		} catch (JSONException e) {
			GlobalApplication.Assert(false, e);
			LoadDefaultSettings();
		}
	}

	void LoadDefaultSettings() {
		Log.v(TAG, "Trying to LoadDefaultSettings from default_settings.json");


		try {
            InputStream in = getAssets().open("default_settings.json");
			String json_string = LoadFileIntoString(in);
			LoadDefaultSettingsFromString(json_string);
		} catch (IOException e) {
			GlobalApplication.Assert(false, e);
		} catch (JSONException e) {
			GlobalApplication.Assert(false, e);
		}
	}

	private void DealWithIntent(Intent intent) {
		if (intent.getAction().equalsIgnoreCase("android.intent.action.VIEW")) { // we
																					// are
																					// opening
																					// a
																					// json
																					// file
																					// to
																					// load
																					// recipes
			// try and retrieve data
			Uri recipe_data = intent.getData();
			ContentResolver cr = getContentResolver();
			try {
				Log.v(TAG, "Importing from " + recipe_data);
				InputStream in = cr.openInputStream(recipe_data);
				String json_string = LoadFileIntoString(in);
				JSONObject json = new JSONObject(json_string);

				replaceItemDialogCount = 0;

				ArrayList<AmyMenuItem> new_menu_items = LoadMenuItemsFromJSON(json);
				for (AmyMenuItem new_item : new_menu_items) {
					AddMenuItem(new_item, ADD_MENU_ITEM);
				}

				// if we didn't show any dialogs we are finished
				if (replaceItemDialogCount == 0) {
					OnAllReplaceItemDialogClosed();
				}

			} catch (FileNotFoundException e) {
				GlobalApplication.Assert(false, e);
			} catch (JSONException e) {
				GlobalApplication.Assert(false, e);
			}
		} else if (intent.getAction().equalsIgnoreCase(
				"android.intent.action.SEND")
				&& intent.hasExtra(Intent.EXTRA_TEXT)) {
			// try and retrieve data
			String s = intent.getStringExtra(Intent.EXTRA_TEXT);

			checkPremium(s);

		}
	}
	
	
	
	private void ParseURL(final String url) {
		DownloadTask task = new DownloadTask();
		task.execute(url);
	}
	
	private boolean TestingParsing = false;
	void TestPopularRecipeSites() {
		TestingParsing = true;
		
		new DownloadTask().execute("http://m.allrecipes.com/recipe/49353/lentils-and-rice-with-fried-onions-mujadarrah?prop24=mobile_rotd");
		new DownloadTask().execute("http://allrecipes.com/Recipe/Salsa-Chicken/Detail.aspx?soid=recs_recipe_2");
		new DownloadTask().execute("http://www.foodnetwork.com/recipes/ina-garten/broccoli-and-bow-ties-recipe.html");
		new DownloadTask().execute("http://www.foodnetwork.co.uk/recipes/real-meatballs-and-spaghetti-674.html");
		new DownloadTask().execute("http://www.food.com/recipe/easy-french-onion-soup-11181");
		new DownloadTask().execute("http://www.food.com/recipe/breakfast-wrap-chimichanga-made-your-way-436672");
		new DownloadTask().execute("http://www.thekitchn.com/recipe-makeahead-roasted-vegetable-burritos-recipes-from-the-kitchn-200281");
		new DownloadTask().execute("http://www.yummly.com/recipe/external/Parmesan-crusted-chicken-350742");
		new DownloadTask().execute("http://www.yummly.com/recipe/external/Agave-sweetened100_-whole-wheat-irish-soda-bread-309621");
		new DownloadTask().execute("http://www.chow.com/recipes/30949-michael-minas-rocky-mountain-chili");
		new DownloadTask().execute("http://www.simplyrecipes.com/recipes/potato_cheddar_guinness_soup/");
		new DownloadTask().execute("http://www.bettycrocker.com/recipes/no-bake-granola-jam-thumbprint-cookies/c3069e88-bf5f-4f43-b6c7-a0e856c52179");
		new DownloadTask().execute("http://www.epicurious.com/articlesguides/bestof/toprecipes/bestburgerrecipes/recipes/food/views/Asian-Pork-and-Mushroom-Burger-Wraps-242710");
		new DownloadTask().execute("http://www.myrecipes.com/recipe/chicken-pot-pie-50400000131724/");
		new DownloadTask().execute("http://agirlcalledjack.com/2014/03/20/pork-and-prune-burgers/");
		new DownloadTask().execute("http://www.indianfoodforever.com/indo-chinese/egg-fried-rice.html");
		new DownloadTask().execute("http://allrecipes.co.uk/m/recipe/6110/buttercream-icing.aspx");
		new DownloadTask().execute("http://www.bbcgoodfood.com/recipes/415618/iced-fairy-cakes");
		new DownloadTask().execute("https://www.donnahay.com.au/recipes/fast-weeknights/chilli-beef-bolognese");
	}

	private class DownloadTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(final String... urls) {

			Log.v(TAG, "doInBackground");

			try {
				runOnUiThread(new Runnable() {
					  public void run() {
						  CharSequence downloading = String.format(
									getString(R.string.downloading_recipe), urls[0]);
						  parsing_recipe_progress = ProgressDialog.show(MainActivity.this, getString(R.string.importing_recipe), downloading, true);
					  }
					});	
				
				//Document doc = Jsoup.connect(urls[0]).userAgent("Mozilla/4.0").get();
				Document doc;
				try {
					Log.v(TAG, "getting url " + urls[0]);
					doc = Jsoup.connect(urls[0]).get();
				} catch (Exception e) {
					runOnUiThread(new Runnable() {
						  public void run() {
							  parsing_recipe_progress.dismiss();
							  
							  CharSequence failed_download_page = String.format(
										getString(R.string.failed_download_page), urls[0]);
								Log.v(TAG, failed_download_page.toString());		
								Toast.makeText(MainActivity.this, failed_download_page, Toast.LENGTH_SHORT)
										.show();
								
								ShowFailedToParseDialog(urls[0]);
						  }
						});	
					
					return "";
				}

				final Elements title = doc.select("title");
				if(title != null) {
					Log.v(TAG, "title " + title.text());
				}
				
				runOnUiThread(new Runnable() {
					  public void run() {
						  CharSequence parsing = String.format(
									getString(R.string.parsing_recipe), title.text());
							parsing_recipe_progress.setMessage(parsing);
					  }
					});	
				
				Elements method = ParseMethod(doc);
				if (method == null || title == null || method.isEmpty()) {	
					runOnUiThread(new Runnable() {
						  public void run() {
							  parsing_recipe_progress.dismiss();
							  
							  CharSequence failed_parse_page = String.format(
										getString(R.string.failed_parse_page), urls[0]);
								Log.v(TAG, failed_parse_page.toString());		
								Toast.makeText(MainActivity.this, failed_parse_page, Toast.LENGTH_SHORT)
										.show();
								
								ShowFailedToParseDialog(urls[0]);
						  }
						});	
					
					
					return "";
				}
				
				ArrayList<RecipeStep> steps = new ArrayList<RecipeStep>();
				Pattern hours_pattern = Pattern.compile("(\\d+) hours", Pattern.CASE_INSENSITIVE);
				Pattern mins_pattern = Pattern.compile("(\\d+) mins", Pattern.CASE_INSENSITIVE);
				Pattern minutes_pattern = Pattern.compile("(\\d+) minutes", Pattern.CASE_INSENSITIVE);
				 
				for (Element span : method) {
					String step = span.text();
					Log.v(TAG, "recipe step: " + step);
					if (step == null || step.trim().length() == 0)
						continue;

					// try to figure out how long it will take
					int time_taken = 0;
					Matcher m = hours_pattern.matcher(step);
					while (m.find()) { // Find each match in turn;
					     String hours = m.group(1); 
					     Log.v(TAG, "hours time_string: " + hours);
					     time_taken += Integer.parseInt(hours) * 60;
					}
					 
					m = mins_pattern.matcher(step);
					while (m.find()) { // Find each match in turn;
					     String mins = m.group(1); 
					     Log.v(TAG, "mins time_string: " + mins);
					     time_taken += Integer.parseInt(mins);
					}
					
					m = minutes_pattern.matcher(step);
					while (m.find()) { // Find each match in turn;
					     String mins = m.group(1); 
					     Log.v(TAG, "minutes time_string: " + mins);
					     time_taken += Integer.parseInt(mins);
					}
					
					if(time_taken == 0) {
						time_taken = 1;
					}

					steps.add(new RecipeStep(step, time_taken));
				}
				AmyMenuItem new_item = new AmyMenuItem(title.text(), steps);

				if(TestingParsing) {
					Log.v(TAG, "Parsing success: " + urls[0]);
					Log.v(TAG, title.text() + ": " + new_item.DebugPrint());
					parsing_recipe_progress.dismiss();
					return "";
				}
				currentEditItemIndex = -1;
				Intent i = new Intent(getContext(), AddMenuItemActivity.class);
				i.putExtra("menu_item", new_item);
				i.putExtra("parsed_url", urls[0]);
				startActivityForResult(i, ADD_MENU_ITEM);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				runOnUiThread(new Runnable() {
					  public void run() {
						  parsing_recipe_progress.dismiss();
					  }
					});	
			}
			return "";
		}
		
		private Elements SplitMethodIntoSteps(Element recipe_method) {
	
			Elements method = recipe_method.select("p");
			if (method != null && method.text().length() > 0) {
				return method;
			}
			
			method = recipe_method.select("li");
			if (method != null && method.text().length() > 0) {
				return method;
			}
			
			method = recipe_method.select("span");
			if (method != null && method.text().length() > 0) {
				return method;
			}
			
			Elements divs = recipe_method.select("div");
			if (divs != null && divs.text().length() > 0) {
				// we can have nested divs, so we select the childlike ones
				Elements children = new Elements();
				for(Element step : divs) {
					if(step.children().isEmpty()) {
						Log.v(TAG, "Adding child div "+step.text());
						children.add(step);
					}
					else {
						boolean any_child_is_a_div = false;
						// are any of the children divs?  If they are we will already be taking them as divs, so don't take them twice
						// If not we'll take the lot here
						for(Element div_child : step.children()) {
							if(div_child.hasClass("div")) {
								any_child_is_a_div = true;
								break;
							}
						}
						
						if(any_child_is_a_div) {
							Log.v(TAG, "Ignoring parent div "+step.text());
						} else {
							Log.v(TAG, "Adding parent div children "+step.text());
							children.add(step);
						}
						
					}
				}
				return children;
			}
			
			return new Elements();
		}
		
		private boolean TextIsMethod(Element sibling) {
			
			if(sibling.text().startsWith("Ingredients")) {
				return false;
			}
			
			if(sibling.text().startsWith("Key Info")) {
				return false;
			}
			
			if(sibling.text().startsWith("Prep time")) {
				return false;
			}
			
			if(sibling.text().startsWith("Cook time")) {
				return false;
			}

			if(sibling.text().startsWith("Serves")) {
				return false;
			}

			return true;
		}
		
		private Elements SplitMethodIntoSteps(Elements recipe) {
			Elements steps = new Elements();
			for(Element recipe_method : recipe) {
				steps.addAll(SplitMethodIntoSteps(recipe_method));
			}
			return steps;
		}
		
		private Elements ParseMethod(Document doc) throws IOException {
			Log.v(TAG, "searching for recipe-method...");
			// see http://www.ebizmba.com/articles/recipe-websites
	
			String[] TAGS_TO_SEARCH_FOR = new String[]{
					"[itemprop=recipeInstructions]", // www.foodnetwork.com uses itemprop="recipeInstructions"
					"[itemprop=instructions]"}; // myrecipes.com uses itemprop="instructions"
			
			for(String tag_to_match : TAGS_TO_SEARCH_FOR) {
				Elements recipe = doc.select(tag_to_match); 
				if (recipe != null && !recipe.isEmpty() && recipe.text().length() > 0) {
					Log.v(TAG, tag_to_match + ": " + recipe.text());

					return SplitMethodIntoSteps(recipe);
				}
			}
			
			String[] IDS_TO_SEARCH_FOR = new String[]{
					"msgDirections",
					"recipe-method", // bbcgoodfood uses the id recipe-method
					"instructions"}; // www.chow.com uses the id instructions
			
			for(String recipe_section_id : IDS_TO_SEARCH_FOR) {
				Element recipe_method = doc.getElementById(recipe_section_id); 
				if (recipe_method != null && recipe_method.text().length() > 0) {
					Log.v(TAG, recipe_section_id + ": " + recipe_method.text());

					return SplitMethodIntoSteps(recipe_method);
				}
			}
			
			String[] CLASSES_TO_SEARCH_FOR = new String[]{
					".recipe-description"}; //donna hay has a class recipe-description
			for(String recipe_class : CLASSES_TO_SEARCH_FOR) {
				Elements recipe = doc.select(recipe_class); 			
				if (recipe != null && !recipe.isEmpty() && recipe.text().length() > 0) {
					Log.v(TAG, "class("+recipe_class+"): " + recipe.text());

					return SplitMethodIntoSteps(recipe);
				}
			}
			
			
			
			String[] TEXT_TO_SEARCH_FOR = new String[]{
					":matchesOwn(^Directions)", /// allrecipes.com (and any website with the text 'Directions' for the method)
					":matchesOwn(^Method)", // www.foodnetwork.com (and any website with the text 'Method' for the method)
					":matchesOwn(^Instructions)", // www.kalynskitchen.com
					":matchesOwn(^Preparation method)"}; //allrecipes.co.uk
			
			for(String text_search : TEXT_TO_SEARCH_FOR) {
				Elements directions_text = doc.select(text_search);
				if (!directions_text.isEmpty()) {
					for(Element text_line : directions_text) {
						Log.v(TAG, text_search + ": " + text_line.text()); 
						
						// the directions might be a sibling element?
						Elements method = new Elements();
						
						while((text_line = text_line.nextElementSibling()) != null) {
							Log.v(TAG, "sibling: " + text_line.text()); 
							
							if(!TextIsMethod(text_line)) {
								Log.v(TAG, "Ignoring "+text_line.text());
								continue;
							}
							
							Elements new_steps = SplitMethodIntoSteps(text_line);
							//Log.v(TAG, "Adding " + new_steps);
							method.addAll(new_steps);
						} 
						
						if(!method.isEmpty() && method.text().length() > 0)
							return method;
					}
				}
			}
					
			// www.thekitchn.com uses the id recipe for the whole recipe - ingredients and all
			Element recipe_method = doc.getElementById("recipe"); 
			if (recipe_method != null && recipe_method.text().length() > 0) {
				Log.v(TAG, "recipe: " + recipe_method.text());

				return SplitMethodIntoSteps(recipe_method);
			}
			
			// www.yummly.com has a frame to display the recipe from another website - I hate you yummly!
			Element external_recipe_site = doc.getElementById("yFrame"); 
			if(external_recipe_site != null) {
				String external_site_url = external_recipe_site.attr("src");
				if(external_site_url != null && external_site_url.length() > 0) {
					Log.v(TAG, "Yummly iframe detected - redirecting to "+external_site_url);
					Document external_doc = Jsoup.connect(external_site_url).get();
					
					// ooh we're getting all recursive here, that might be bad
					return ParseMethod(external_doc);
					
				}
			}
			
			// agirlcalledjack is a PITA - we grab the section with class entry-content
			Elements recipe = doc.select(".entry-content"); 			
			if (recipe != null && !recipe.isEmpty() && recipe.text().length() > 0) {
				Log.v(TAG, ".entry-content: " + recipe.text());

				return SplitMethodIntoSteps(recipe);
			}
			
			// do we have a bunch of <li> tags?  clutching at straws here...
			Elements recipe_steps = doc.select("li"); 			
			if (recipe_steps != null && !recipe_steps.isEmpty() && recipe_steps.text().length() > 0) {
				Log.v(TAG, "li: " + recipe_steps.text());

				return SplitMethodIntoSteps(recipe_steps);
			}

			return null;
		}

	}

	@Override
	public void onNewIntent(Intent intent) {
		Log.v(TAG, "onNewIntent");

		super.onNewIntent(intent);

		DealWithIntent(intent);
	}

	public static Context getContext(){
        return mContext;
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mContext = this;

		ActionBar ab = getSupportActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		//To change the actionbar colour
		//ab.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.header_background)));
		//To change the tab bar color under the actionbar:
		ab.setStackedBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.light_grey)));
		//To change tab bar background:
		//ab.setStackedBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.tab_background)));
		
		
		Tab cookTab = ab.newTab()
                .setText(R.string.cook_tab)
                .setTabListener(new TabListener<CookTabFragment>(
                        this, "cook", CookTabFragment.class));
		ab.addTab(cookTab, 0, false);

		editTab = ab.newTab()
            .setText(R.string.edit_tab)
            .setTabListener(new TabListener<EditTabFragment>(
                    this, "edit", EditTabFragment.class));
		ab.addTab(editTab, 1, false);
		
		if(savedInstanceState == null) {
			// we select the cook tab
			getSupportActionBar().setSelectedNavigationItem(0);
			
			LoadSavedSettings();
			
			try {
				int current_version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
				if(WhatsChangedVersionDisplayed != current_version) {
					showWhatsNewDialog(WhatsChangedVersionDisplayed);
				}
				
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
			ExtractState(savedInstanceState);
		}
		
		Intent sender = getIntent();
		DealWithIntent(sender);

		mInAppBilling.Initialise(this);
		
		//TestPopularRecipeSites();
	}
	
	 @Override
	 protected void onSaveInstanceState(Bundle savedInstanceState) {
	     super.onSaveInstanceState(savedInstanceState);
	     Log.v(TAG, "onSaveInstanceState");

	     savedInstanceState.putInt("uk.co.islovely.cooking.selected_tab", getSupportActionBar().getSelectedNavigationIndex());
	     
	     savedInstanceState.putParcelableArrayList("uk.co.islovely.cooking.menu_items", menuItems);
	     savedInstanceState.putParcelableArrayList("uk.co.islovely.cooking.disabled_menu_items", disabledMenuItems);
	     savedInstanceState.putInt("uk.co.islovely.cooking.default_finish_time", DefaultFinishTime);
	     savedInstanceState.putBoolean("uk.co.islovely.cooking.display_items_that_take_too_long", DisplayItemsThatTakeTooLong);
	     savedInstanceState.putInt("uk.co.islovely.cooking.whats_changed_version_displayed", WhatsChangedVersionDisplayed);
	     savedInstanceState.putBoolean("uk.co.islovely.cooking.two_things", InAppBillingForCookingTwoThingsAtOnce);
	     savedInstanceState.putString("uk.co.islovely.cooking.url_to_parse", URLToParse);
	     savedInstanceState.putInt("uk.co.islovely.cooking.replace_item_dialog_count", replaceItemDialogCount);
	     savedInstanceState.putInt("uk.co.islovely.cooking.edit_item_index", currentEditItemIndex);
	     
//	     Log.v(TAG, "menuItems " + menuItems);
//	     Log.v(TAG, "disabledMenuItems " + disabledMenuItems);
//	     Log.v(TAG, "DefaultFinishTime " + DefaultFinishTime);
//	     Log.v(TAG, "DisplayItemsThatTakeTooLong " + DisplayItemsThatTakeTooLong);
//	     Log.v(TAG, "WhatsChangedVersionDisplayed " + WhatsChangedVersionDisplayed);
//	     Log.v(TAG, "InAppBillingForCookingTwoThingsAtOnce " + InAppBillingForCookingTwoThingsAtOnce);
//	     Log.v(TAG, "URLToParse " + URLToParse);
//	     Log.v(TAG, "replaceItemDialogCount " + replaceItemDialogCount);
//	     Log.v(TAG, "currentEditItemIndex " + currentEditItemIndex);
//	     Log.v(TAG, "finish_time " + finish_time);
	 }
	 
	 @Override
	 protected void onRestoreInstanceState(Bundle savedInstanceState) {
	     super.onRestoreInstanceState(savedInstanceState);
	     Log.v(TAG, "onRestoreInstanceState");
	     
	     // we extract the state in onCreate (before this), so no need to extract it here too
	     //ExtractState(savedInstanceState);
	 }
	 
	 private void ExtractState(Bundle savedInstanceState) {
		 
		// Restore the previously serialized current tab position.
		 if (savedInstanceState.containsKey("uk.co.islovely.cooking.selected_tab")) {
			 getSupportActionBar().setSelectedNavigationItem(savedInstanceState.getInt("uk.co.islovely.cooking.selected_tab"));
		 }
		 
		 // adapter is inked to menuItems, so make sure we don't change it to a different object!
		 ArrayList<AmyMenuItem> extracted_items = savedInstanceState.getParcelableArrayList("uk.co.islovely.cooking.menu_items");
		 if(extracted_items != menuItems) {
			 menuItems.clear();
			 menuItems.addAll(extracted_items);
		 }
		 disabledMenuItems = savedInstanceState.getParcelableArrayList("uk.co.islovely.cooking.disabled_menu_items");
		 DefaultFinishTime = savedInstanceState.getInt("uk.co.islovely.cooking.default_finish_time");
		 DisplayItemsThatTakeTooLong = savedInstanceState.getBoolean("uk.co.islovely.cooking.display_items_that_take_too_long");
		 WhatsChangedVersionDisplayed = savedInstanceState.getInt("uk.co.islovely.cooking.whats_changed_version_displayed");
		 InAppBillingForCookingTwoThingsAtOnce = savedInstanceState.getBoolean("uk.co.islovely.cooking.two_things");
		 URLToParse = savedInstanceState.getString("uk.co.islovely.cooking.url_to_parse");
		 replaceItemDialogCount = savedInstanceState.getInt("uk.co.islovely.cooking.replace_item_dialog_count");
		 currentEditItemIndex = savedInstanceState.getInt("uk.co.islovely.cooking.edit_item_index");
		 
//	     Log.v(TAG, "menuItems " + menuItems);
//	     Log.v(TAG, "disabledMenuItems " + disabledMenuItems);
//	     Log.v(TAG, "DefaultFinishTime " + DefaultFinishTime);
//	     Log.v(TAG, "DisplayItemsThatTakeTooLong " + DisplayItemsThatTakeTooLong);
//	     Log.v(TAG, "WhatsChangedVersionDisplayed " + WhatsChangedVersionDisplayed);		 
//	     Log.v(TAG, "InAppBillingForCookingTwoThingsAtOnce " + InAppBillingForCookingTwoThingsAtOnce);
//	     Log.v(TAG, "URLToParse " + URLToParse);
//	     Log.v(TAG, "replaceItemDialogCount " + replaceItemDialogCount);
//	     Log.v(TAG, "currentEditItemIndex " + currentEditItemIndex);
//	     Log.v(TAG, "finish_time " + finish_time);
	 }


	private String GetRenameSuggestion(AmyMenuItem new_item) {
		for (int suffix = 0;; ++suffix) {
			String suggestion = new_item.Name + "-" + suffix;
			boolean already_in_use = false;

			// does this suggestion already exist?
			for (AmyMenuItem item : menuItems) {
				if (item.Name.equalsIgnoreCase(suggestion)) {
					already_in_use = true;
					break;
				}
			}

			if (already_in_use)
				continue;

			for (AmyMenuItem item : disabledMenuItems) {
				if (item.Name.equalsIgnoreCase(suggestion)) {
					already_in_use = true;
					break;
				}
			}

			if (already_in_use)
				continue;

			return suggestion;
		}
	}
	
	void ShowFailedToParseDialog(final String url) {
		CharSequence failed_parse = String.format(
				getString(R.string.failed_parse_page), url);

		new AlertDialog.Builder(this)
				.setTitle(getString(R.string.failed_parse_title))
				.setMessage(failed_parse.toString())
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.email_developer,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
								// launch into email program
								/* Create the Intent */
								 final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

								 /* Fill it with Data */
								 emailIntent.setType("plain/text");
								 emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"cooking@thinkysaurus.com"});
								 emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Failed to parse recipe");
								 emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hello Amy!\nI just failed to parse the recipe at "+url+", please can you fix it for me?\nThank you!\n");

								 /* Send it off to the Activity-Chooser */
								 startActivity(Intent.createChooser(emailIntent, "Send mail..."));
							}
						}).create().show();
	}

	private void ShowReplaceDialog(final AmyMenuItem new_item,
			final int item_to_replace_index, boolean current_item_is_disabled) {
		final ArrayList<AmyMenuItem> menu_items = current_item_is_disabled ? disabledMenuItems
				: menuItems;

		// this is so that we don't sort the array or mess with it while the
		// dialogs are being displayed (if we did then the dialogs would write
		// all over each other's data)
		// instead we sort at the end
		replaceItemDialogCount++;
		Log.v(TAG, "replaceItemDialogCount incremented to "
				+ replaceItemDialogCount);

		CharSequence sure_replace = String.format(
				getString(R.string.sure_replace_menu_item), new_item.Name);
		final String rename_suggestion = GetRenameSuggestion(new_item);
		CharSequence rename_to = String.format(
				getString(R.string.rename_menu_item_to), rename_suggestion);

		new AlertDialog.Builder(this)
				.setTitle(getString(R.string.replace_menu_item))
				.setMessage(sure_replace.toString())
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
								CharSequence skipping_item = String.format(
										getString(R.string.skipping_menu_item),
										new_item.Name);
								Toast.makeText(MainActivity.this,
										skipping_item, Toast.LENGTH_SHORT)
										.show();
								Log.v(TAG, skipping_item.toString());
								OnReplaceItemDialogClose();
							}
						})
				.setNeutralButton(rename_to,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
								CharSequence adding_item = String.format(
										getString(R.string.saving_menu_item),
										rename_suggestion);
								Toast.makeText(MainActivity.this, adding_item,
										Toast.LENGTH_SHORT).show();
								Log.v(TAG, adding_item.toString());
								Log.v(TAG, "name changed from " + new_item.Name
										+ " to " + rename_suggestion);
								new_item.Name = rename_suggestion;
								menu_items.add(new_item);
								OnReplaceItemDialogClose();
							}
						})
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
								// if user clicks very quickly they can choose
								// two buttons from the same dialog (or the same
								// button twice) make sure we don't overwrite
								// anything we shouldn't!
								if (menu_items.get(item_to_replace_index).Name
										.equalsIgnoreCase(new_item.Name)) {
									CharSequence replacing_item = String
											.format(getString(R.string.replacing_menu_item),
													new_item.Name);
									Toast.makeText(MainActivity.this,
											replacing_item, Toast.LENGTH_SHORT)
											.show();
									Log.v(TAG, replacing_item.toString());

									menu_items.set(item_to_replace_index,
											new_item);
								} else {
									// something has gone wrong
									CharSequence skipping_item = String
											.format(getString(R.string.skipping_menu_item),
													rename_suggestion);
									Toast.makeText(MainActivity.this,
											skipping_item, Toast.LENGTH_SHORT)
											.show();
									Log.v(TAG,
											"NOT replacing "
													+ menu_items
															.get(item_to_replace_index).Name
													+ " with " + new_item.Name);
									Log.v(TAG, skipping_item.toString());
								}

								OnReplaceItemDialogClose();
							}
						}).create().show();
	}

    public void onRequestPermissionsResult (int requestCode,
                                     String[] permissions,
                                     int[] grantResults) {
        mInAppBilling.onPermissionsGranted(grantResults[0]);
    }

	public boolean AskForPermission(String perm) {

		int hasPerm = ContextCompat.checkSelfPermission(this,
				perm);
		if (hasPerm != PackageManager.PERMISSION_GRANTED) {
			Log.e(TAG, "No permission for getting accounts");
			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					perm)) {

				// Show an expanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.
				Toast.makeText(this, getString(R.string.needs_accounts_permissions), Toast.LENGTH_SHORT).show();
                return false;
			} else {

				// No explanation needed, we can request the permission.
                int requestCode = 1;
				ActivityCompat.requestPermissions(this,
						new String[]{perm},
                        requestCode);
			}
		}

        return true;
	}

	private void OnReplaceItemDialogClose() {
		replaceItemDialogCount--;
		Log.v(TAG, "replaceItemDialogCount decremented to "
				+ replaceItemDialogCount);
		// if you click quickly you can call two onclick callbacks for different
		// buttons on 2.3 :(
		// so make sure we never go negative here!
		replaceItemDialogCount = Math.max(replaceItemDialogCount, 0);
		if (replaceItemDialogCount == 0) {
			OnAllReplaceItemDialogClosed();
		}
	}

	private void OnAllReplaceItemDialogClosed() {
		Log.v(TAG, "OnAllReplaceItemDialogClosed");

		GetCookTabFragment().DisableMenuItemsThatTakeTooLong();
		adapter.notifyDataSetChanged();
		SaveMenuItems();
	}

	private void AddMenuItem(final AmyMenuItem new_item, int requestCode) {
		Log.v(TAG, "AddMenuItem " + new_item.Name);

		// is this a dup?
		for (int i = 0; i < menuItems.size(); ++i) {
			AmyMenuItem current_item = menuItems.get(i);

			if (current_item.Name.equalsIgnoreCase(new_item.Name)) {

				// if these are exactly equal we can skip updating the item
				if (current_item.Matches(new_item)) {
					CharSequence skipping_item = String.format(
							getString(R.string.skipping_menu_item),
							new_item.Name);
					Toast.makeText(this, skipping_item, Toast.LENGTH_SHORT)
							.show();
					Log.v(TAG, skipping_item.toString());
					return;
				}

				// pop up a replace dialog
				ShowReplaceDialog(new_item, i, false);

				return;
			}
		}

		for (int i = 0; i < disabledMenuItems.size(); ++i) {
			AmyMenuItem current_item = disabledMenuItems.get(i);

			if (current_item.Name.equalsIgnoreCase(new_item.Name)) {
				// if these are exactly equal we can skip updating the item
				if (current_item.Matches(new_item)) {
					CharSequence skipping_item = String.format(
							getString(R.string.skipping_menu_item),
							new_item.Name);
					Toast.makeText(this, skipping_item, Toast.LENGTH_SHORT)
							.show();
					Log.v(TAG, skipping_item.toString());
					return;
				}

				// pop up a replace dialog
				ShowReplaceDialog(new_item, i, true);

				return;
			}
		}

		// are we actually adding, or are we editing?
		String adding = getString(R.string.adding_menu_item);
		if(requestCode == EDIT_MENU_ITEM) {
			adding = getString(R.string.saving_menu_item);
		} 
		CharSequence adding_item = String.format(adding, new_item.Name);
		Toast.makeText(this, adding_item, Toast.LENGTH_SHORT).show();
		Log.v(TAG, adding_item.toString());
		menuItems.add(new_item);
	}

	public static void CheckForDuplicateItems() {
		// check for duplicates in menuItems
		for (int i = 0; i < menuItems.size(); ++i) {
			AmyMenuItem item = menuItems.get(i);
			for (int j = i + 1; j < menuItems.size(); ++j) {
				AmyMenuItem other_item = menuItems.get(j);
				if (item.Matches(other_item)) {
					GlobalApplication.Assert(false,
							"Duplicate item in menuItems " + item.toString());
					menuItems.remove(j);
					j--;
				}
			}

			for (int j = 0; j < disabledMenuItems.size(); ++j) {
				AmyMenuItem other_item = disabledMenuItems.get(j);
				if (item.Matches(other_item)) {
					GlobalApplication.Assert(false,
							"Duplicate item in menuItems and disabledMenuItems "
									+ item.toString());
					disabledMenuItems.remove(j);
					j--;
				}
			}
		}

		// check for duplicates in disabledMenuItems
		for (int i = 0; i < disabledMenuItems.size(); ++i) {
			AmyMenuItem item = disabledMenuItems.get(i);
			for (int j = i + 1; j < disabledMenuItems.size(); ++j) {
				AmyMenuItem other_item = disabledMenuItems.get(j);
				if (item.Matches(other_item)) {
					GlobalApplication.Assert(false,
							"Duplicate item in menuItems and disabledMenuItems "
									+ item.toString());
					disabledMenuItems.remove(j);
					j--;
				}
			}
		}
	}

	public void onDisabledItemsWarningClicked(View v) {
		showSettings();
	}
	
	public void UpdateInAppBillingUI() {
		// let's borrow the disabled items text
		TextView in_app_billing_status = (TextView) findViewById(R.id.finishTimeHeader);
		if(in_app_billing_status == null) {
			// haven't set up cook tab fragment yet
			return;
		}
		
		switch (mInAppBilling.CurrentState) {
		case Ready: {
			in_app_billing_status.setVisibility(View.GONE);

			if (InAppBillingPurchasingDialog != null
					&& InAppBillingPurchasingDialog.isShowing()) {
				InAppBillingPurchasingDialog.dismiss();

				// did user just buy?
				if (mInAppBilling.IsPaidVersion) {
					if(InAppBillingForCookingTwoThingsAtOnce) {
						startActivityForResult(LaunchSummaryActivityIntent,
								START_COOKING);
					}
					else {
						ParseURL(URLToParse);
					}
				}
			}
		}
			break;
		case Initialising: {
			in_app_billing_status.setText(R.string.in_app_billing_initialising);
			in_app_billing_status.setVisibility(View.VISIBLE);
		}
			break;
		case Purchasing: {
			in_app_billing_status.setText(R.string.in_app_billing_purchasing);
			in_app_billing_status.setVisibility(View.VISIBLE);
		}
			break;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_settings:
			showSettings();
			return true;
		case R.id.menu_parse_recipe:
			showParseRecipe();
			return true;
		case R.id.menu_about:
			showAbout();
			return true;
		case R.id.menu_share_all:
			ArrayList<AmyMenuItem> allMenuItems = new ArrayList<AmyMenuItem>();
			allMenuItems.addAll(menuItems);
			allMenuItems.addAll(disabledMenuItems);
			shareMenuItems(allMenuItems);
			return true;
		case R.id.menu_deselect_all:
			final ListView lv = (ListView) findViewById(android.R.id.list);
			for (int item_index = 0; item_index < lv.getCount(); item_index++) {
				lv.setItemChecked(item_index, false);		
			}
			return true;
		case R.id.menu_whats_new:
			showWhatsNewDialog(0); // show all versions
			return true;
		case R.id.menu_export_schedule:
			showExportScheduleDialog();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	


	public void addItems(View v) {

		Intent i = new Intent(this, AddMenuItemActivity.class);
		startActivityForResult(i, ADD_MENU_ITEM);
	}

	private void showSettings() {

		Intent i = new Intent(this, SettingsActivity.class);
		i.putExtra("default_finish_time", DefaultFinishTime);
		i.putExtra("display_items_that_take_too_long",
				DisplayItemsThatTakeTooLong);
		startActivityForResult(i, SETTINGS);
	}
	
	private void showWhatsNewDialog(int previous_version) {
		int current_version;
		try {
			current_version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			Log.v(TAG, "showWhatsNewDialog previous_version "+previous_version+", current_version "+current_version+"\n");
			Resources res = getResources();
			String[] new_in_this_version = res.getStringArray(R.array.version_about);
			String version_string = "";
			for(int i=current_version-1; i>=previous_version; --i) {
				if(i < current_version-1) 
					version_string += "\n\n";
				version_string += new_in_this_version[i];
			}
			
			ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.DialogBaseTheme);
			new AlertDialog.Builder(wrapper)
			.setTitle(getString(R.string.menu_whats_new))
			.setMessage(version_string)
			.setPositiveButton(android.R.string.ok, null).create().show();
			
			WhatsChangedVersionDisplayed = current_version;
			SaveMenuItems();
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void showExportScheduleDialog() {
		FinishTime finish_time = GetCookTabFragment().ParseFinishTime();
		final ArrayList<AmyMenuItem> selected_items = GetCookTabFragment().GetSelectedMenuItems();
		final Calendar finish_time_calendar;
		
		// check we have some items selected
		if (selected_items.size() == 0) {
			new AlertDialog.Builder(this)
					.setTitle(getString(R.string.menu_export_schedule))
					.setMessage(
							getString(R.string.select_items_for_export_schedule))
					.setPositiveButton(android.R.string.ok, null).create()
					.show();
			return;
		}
				
		
		if (finish_time.ASAP) {
			// work out when we can finish
			int time_taken = 0;
			for (AmyMenuItem selected_item : selected_items) {
				time_taken = Math.max(time_taken, selected_item.GetTotalTime());
			}
			finish_time_calendar = new GregorianCalendar();
			finish_time_calendar.add(Calendar.MINUTE, time_taken);
		} else {
			finish_time_calendar = finish_time.mFinishTime;
		}
		// take the time part only of finish time (we'll assume we're just cooking on one day!)
		final String finish_time_string = getString(R.string.finish_time) + " " + DateFormat.getTimeFormat(this).format(finish_time_calendar.getTimeInMillis()) + "\n";
	
		String menu_items_string2 = getString(R.string.menu_items_heading) + "\n";
		for (AmyMenuItem selected_item : selected_items) {
			menu_items_string2 += selected_item.toString() + "\n";
		}
		
		final String menu_items_string = menu_items_string2;

		ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.DialogBaseTheme);
		new AlertDialog.Builder(wrapper)
		.setTitle(getString(R.string.menu_export_schedule))
		.setMessage(menu_items_string + "\n" + finish_time_string)
		.setPositiveButton(R.string.export, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				exportSchedule(finish_time_calendar, selected_items, menu_items_string, finish_time_string);
			}
		})
		.setNegativeButton(android.R.string.cancel, null).create().show();

	}
	
	private void exportSchedule(Calendar finish_time, ArrayList<AmyMenuItem> selected_items, String menu_items_string, String finish_time_string) {
		
		ArrayList<RecipeStepData> sortedSteps = SummaryActivity.FillInSortedSteps(this,selected_items,finish_time);	
		
		String steps_string = getString(R.string.cooking_steps_heading) + "\n";
	
		for(int i=0; i<sortedSteps.size(); ++i) {
			RecipeStepData step_data = sortedSteps.get(i);
			steps_string += step_data.toString(this) + "\n";
		}
		
		// launch into email program
		/* Create the Intent */
		 final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

		 /* Fill it with Data */
		 emailIntent.setType("plain/text");
		 emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Cooking Schedule");
		 emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, menu_items_string + "\n" + finish_time_string + "\n" + steps_string);

		 /* Send it off to the Activity-Chooser */
		 startActivity(Intent.createChooser(emailIntent, "Send mail..."));
	}
	
	private void showParseRecipe() {
		final EditText txtUrl = new EditText(this);
		
		ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.DialogBaseTheme);
		
		new AlertDialog.Builder(wrapper)
		.setTitle(getString(R.string.choose_parse_title))
		.setMessage(R.string.choose_parse_text)
		.setView(txtUrl)
		.setNegativeButton(android.R.string.cancel, null)
		.setPositiveButton(R.string.parse,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
						String url = txtUrl.getText().toString();
						checkPremium(url);
					}
				}).create().show();
	}
	
	private void checkPremium(final String url) {
		InAppBillingForCookingTwoThingsAtOnce = false;
		URLToParse = url;
		// check user has paid if they want to cook more than 2 items
		if (!mInAppBilling.IsPaidVersion) {
			new AlertDialog.Builder(this)
					.setTitle(getString(R.string.importing_recipe))
					.setMessage(getString(R.string.parsing_upgrade))
					.setNegativeButton(android.R.string.no, null)
					.setNeutralButton(R.string.skinflint, 
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface arg0, int arg1) {
									ParseURL(url);
								}
							})
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface arg0, int arg1) {
									mInAppBilling.onUpgradeAppButtonClicked();

									if (mInAppBilling.CurrentState == InAppBilling.EState.Purchasing) {
										// wait for the in-app-billing to
										// complete
										InAppBillingPurchasingDialog = ProgressDialog
												.show(getContext(),
														getString(R.string.in_app_billing_purchasing),
														getString(R.string.please_wait),
														true);
									}

									// did user just buy?
									if (mInAppBilling.IsPaidVersion) {
										ParseURL(url);
									}
								}
							}).create().show();

			// we don't launch the parsing activity until user has finished in
			// app purchasing
		}
	}

	public void showAbout() {

		Intent i = new Intent(this, AboutActivity.class);
		startActivityForResult(i, ABOUT);
	}

	public void startCooking(View v) {
		FinishTime finish_time = GetCookTabFragment().ParseFinishTime();

		ArrayList<AmyMenuItem> selected_items = GetCookTabFragment().GetSelectedMenuItems();

		// check we have some items selected
		if (selected_items.size() == 0) {
			new AlertDialog.Builder(this)
					.setTitle(getString(R.string.start_cooking))
					.setMessage(
							getString(R.string.select_items_for_start_cooking))
					.setPositiveButton(android.R.string.ok, null).create()
					.show();
			return;
		}

		LaunchSummaryActivityIntent = new Intent(this, SummaryActivity.class);
		if (finish_time.ASAP) {
			// work out when we can finish
			int time_taken = 0;
			for (AmyMenuItem selected_item : selected_items) {
				time_taken = Math.max(time_taken, selected_item.GetTotalTime());
			}
			Calendar asap_finish_time = new GregorianCalendar();
			asap_finish_time.add(Calendar.MINUTE, time_taken);
			LaunchSummaryActivityIntent.putExtra("finish_time",
					asap_finish_time.getTimeInMillis());
		} else {
			LaunchSummaryActivityIntent.putExtra("finish_time",
					finish_time.mFinishTime.getTimeInMillis());
		}
		LaunchSummaryActivityIntent.putParcelableArrayListExtra(
				"selected_items", selected_items);

		// check user has paid if they want to cook more than 2 items
		if (!mInAppBilling.IsPaidVersion && (selected_items.size() > 2)) {
			InAppBillingForCookingTwoThingsAtOnce = true;
			new AlertDialog.Builder(this)
					.setTitle(getString(R.string.start_cooking))
					.setMessage(getString(R.string.more_2_items_upgrade))
					.setNegativeButton(android.R.string.no, null)
					.setNeutralButton(R.string.skinflint, 
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface arg0, int arg1) {
									startActivityForResult(
											LaunchSummaryActivityIntent,
											START_COOKING);
								}
							})
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface arg0, int arg1) {
									mInAppBilling.onUpgradeAppButtonClicked();

									if (mInAppBilling.CurrentState == InAppBilling.EState.Purchasing) {
										// wait for the in-app-billing to
										// complete
										InAppBillingPurchasingDialog = ProgressDialog
												.show(getContext(),
														getString(R.string.in_app_billing_purchasing),
														getString(R.string.please_wait),
														true);
									}

									// did user just buy?
									if (mInAppBilling.IsPaidVersion) {
										startActivityForResult(
												LaunchSummaryActivityIntent,
												START_COOKING);
									}
								}
							}).create().show();

			// we don't launch the summary activity until user has finished in
			// app purchasing
			return;
		}

		startActivityForResult(LaunchSummaryActivityIntent, START_COOKING);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		// does this belong to in app billing? We'll let it know just in case...
		if (mInAppBilling.handleActivityResult(requestCode, resultCode, data)) {
			Log.d(TAG,
					"onActivityResult ignored because in app billing nicked it");
			return;
		}

		switch (requestCode) {
		case ADD_MENU_ITEM:
			switch (resultCode) {
			case RESULT_CANCELED:

			case RESULT_OK:
				if (data.hasExtra("name") && data.hasExtra("steps")) {
					String item_name = data.getExtras().getString("name");
					ArrayList<RecipeStep> steps = data.getExtras()
							.getParcelableArrayList("steps");

					AmyMenuItem new_item = new AmyMenuItem(item_name, steps);
					AddMenuItem(new_item, ADD_MENU_ITEM);

					GetCookTabFragment().DisableMenuItemsThatTakeTooLong();
					// adapter.notifyDataSetChanged();

					SaveMenuItems();
				}
			}
			break;
		case EDIT_MENU_ITEM:
			switch (resultCode) {
			case RESULT_CANCELED:

			case RESULT_OK:
				if (data.hasExtra("name") && data.hasExtra("steps")) {
					String item_name = data.getExtras().getString("name");
					ArrayList<RecipeStep> steps = data.getExtras()
							.getParcelableArrayList("steps");

					// we remove the old item, then add a new item so that we
					// can check for name collisions
					menuItems.remove(currentEditItemIndex);
					AmyMenuItem new_item = new AmyMenuItem(item_name, steps);
					AddMenuItem(new_item, EDIT_MENU_ITEM);
					GetCookTabFragment().DisableMenuItemsThatTakeTooLong();
					adapter.notifyDataSetChanged();

					SaveMenuItems();
				}
			}
			break;
		case SETTINGS:
			switch (resultCode) {
			case RESULT_CANCELED:

			case RESULT_OK:
				if (data.hasExtra("default_finish_time")) {
					int default_finish_time = data.getExtras().getInt(
							"default_finish_time");
					if (default_finish_time != DefaultFinishTime) {
						DefaultFinishTime = default_finish_time;
						GetCookTabFragment().DisplayDefaultFinishTime();
						SaveMenuItems();
					}
					boolean display_items_that_take_too_long = data.getExtras()
							.getBoolean("display_items_that_take_too_long");
					if (DisplayItemsThatTakeTooLong != display_items_that_take_too_long) {
						Log.v(TAG, "Setting DisplayItemsThatTakeTooLong " + DisplayItemsThatTakeTooLong + " => " + display_items_that_take_too_long);
						DisplayItemsThatTakeTooLong = display_items_that_take_too_long;
						GetCookTabFragment().DisableMenuItemsThatTakeTooLong();
						SaveMenuItems();
					}
				}
			}
			break;
		}
	}

	public void showFinishTimeDialog(View v) {
		FinishTime finish_time = GetCookTabFragment().ParseFinishTime();
		// Create an instance of the dialog fragment and show it
		DialogFragment dialog = new FinishTimeDialogFragment();
		Bundle args = new Bundle();
		args.putBoolean("as_soon_as_possible", finish_time.ASAP);
		if (finish_time.ASAP) {
			// we pass through the default finish time in case user changes from
			// asap
			args.putLong("finish_time", GetDefaultFinishTime()
					.getTimeInMillis());
		} else {
			args.putLong("finish_time",
					finish_time.mFinishTime.getTimeInMillis());
		}
		dialog.setArguments(args);
		dialog.show(getSupportFragmentManager(), "FinishTimeDialogFragment");
	}

	public static Calendar GetDefaultFinishTime() {
		Calendar finish_time = new GregorianCalendar();
		switch (DefaultFinishTime) {
		case SettingsActivity.DEFAULT_FINISH_TIME_30_MINS:
			finish_time.add(Calendar.MINUTE, 30);
			return finish_time;
		case SettingsActivity.DEFAULT_FINISH_TIME_1_HOUR:
			finish_time.add(Calendar.HOUR_OF_DAY, 1);
			return finish_time;
		case SettingsActivity.DEFAULT_FINISH_TIME_ASAP:
			return finish_time;
		}

		return finish_time;
	}

	// The dialog fragment receives a reference to this Activity through the
	// Fragment.onAttach() callback, which it uses to call the following methods
	// defined by the FinishTimeDialogFragment.FinishTimeDialogListener
	// interface
	// @Override
	public void onDialogPositiveClick(DialogFragment dialog) {
		// User touched the dialog's positive button
		FinishTimeDialogFragment ft_dialog = (FinishTimeDialogFragment) dialog;
		if (ft_dialog.FinishTimeIsASAP()) {
			GetCookTabFragment().DisplayFinishTime(getString(R.string.as_soon_as_possible));
		} else {
			Calendar finish_time = ft_dialog.GetFinishTime();
			GetCookTabFragment().DisplayFinishTime(finish_time);
		}
	}

	// @Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		// User touched the dialog's negative button
		// nothing to do here... move right along...
	}

	// @Override
	public void onDialogNeutralClick(DialogFragment dialog) {
		// User touched the dialog's reset finish time button
		GetCookTabFragment().DisplayDefaultFinishTime();
	}

	CookTabFragment GetCookTabFragment() {
		return (CookTabFragment) getSupportFragmentManager().findFragmentByTag("cook");
	}
	
	public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
	    private Fragment mFragment;
	    private final ActionBarActivity mActivity;
	    private final String mTag;
	    private final Class<T> mClass;

	    /** Constructor used each time a new tab is created.
	      * @param activity  The host Activity, used to instantiate the fragment
	      * @param tag  The identifier tag for the fragment
	      * @param clz  The fragment's Class, used to instantiate the fragment
	      */
	    public TabListener(Activity activity, String tag, Class<T> clz) {
	        mActivity = (ActionBarActivity)activity;
	        mTag = tag;
	        mClass = clz;
	    }

	    /* The following are each of the ActionBar.TabListener callbacks */

	    public void onTabSelected(Tab tab, FragmentTransaction ft) {
	    	Fragment preInitializedFragment = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);

	        // Check if the fragment is already initialized
	        if (mFragment == null && preInitializedFragment == null) {
	        	Log.v(TAG, "added "+mTag.toString());
	        	// If not, instantiate and add it to the activity
	            mFragment = Fragment.instantiate(mActivity, mClass.getName());
	            ft.add(android.R.id.content, mFragment, mTag);  
	        } else if (mFragment != null) {
	        	Log.v(TAG, "attached "+mTag.toString());
	            // If it exists, simply attach it in order to show it
	            ft.attach(mFragment);
	        } else if (preInitializedFragment != null) {
	        	Log.v(TAG, "attached pre-initialised fragment "+mTag.toString());
	            ft.attach(preInitializedFragment);
	            mFragment = preInitializedFragment;
	        }
	    }

	    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	        if (mFragment != null) {
	        	Log.v(TAG, "detached "+mTag.toString());
	            // Detach the fragment, because another one is being attached
	            ft.detach(mFragment);
	        }
	    }

	    public void onTabReselected(Tab tab, FragmentTransaction ft) {
	        // User selected the already selected tab. Usually do nothing.
	    }
	}

	  public static class CookTabFragment extends Fragment {
		  View cookTabView;

		    @Override
		    public View onCreateView(LayoutInflater inflater, ViewGroup container,
		        Bundle savedInstanceState) {
		    	cookTabView = inflater.inflate(R.layout.main_screen_cook_tab, container, false);
		    	
				adapter = new ArrayAdapter<AmyMenuItem>(getActivity(),
						android.R.layout.simple_list_item_multiple_choice, menuItems);
				final ListView lv = (ListView) cookTabView.findViewById(android.R.id.list);
				lv.setAdapter(adapter);

				lv.setItemsCanFocus(false);
				lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
				registerForContextMenu(lv);
				
				// this will disable items that take too long
				if(savedInstanceState == null) {
					DisplayDefaultFinishTime();
				} else {
					ExtractState(savedInstanceState);
				}
				
				//eep!
				MainActivity activity = (MainActivity)getActivity();
				activity.UpdateInAppBillingUI();

				return cookTabView;
		    }

			 @Override
			public void onSaveInstanceState(Bundle savedInstanceState) {
			     super.onSaveInstanceState(savedInstanceState);
			     Log.v(TAG, "CookTabFragment onSaveInstanceState");
			     
			     if(cookTabView == null) {
			    	 // we're in edit
			    	 return;
			     }

			     FinishTime finish_time = ParseFinishTime();
			     
			     savedInstanceState.putParcelable("uk.co.islovely.cooking.finish_time", finish_time);
	
			     Log.v(TAG, "finish_time " + finish_time);
			 }

			 private void ExtractState(Bundle savedInstanceState) {
				 Log.v(TAG, "CookTabFragment ExtractState");
				 
				 FinishTime finish_time = savedInstanceState.getParcelable("uk.co.islovely.cooking.finish_time");
				 
				 DisplayFinishTime(finish_time);
				 
			     Log.v(TAG, "finish_time " + finish_time);
			 }

			 private ArrayList<AmyMenuItem> GetSelectedMenuItems() {
				ArrayList<AmyMenuItem> selected_items = new ArrayList<AmyMenuItem>();

				final ListView lv = (ListView) cookTabView.findViewById(android.R.id.list);
				SparseBooleanArray checkedItems = lv.getCheckedItemPositions();
				if (checkedItems != null) {
					for (int i = 0; i < checkedItems.size(); i++) {
						if (checkedItems.valueAt(i)) {
							// skip anything selected that doesn't finish in time
							int item_position = checkedItems.keyAt(i);
							if (item_position >= lv.getAdapter().getCount())
								continue;

							selected_items.add((AmyMenuItem) lv.getAdapter().getItem(
									item_position));
						}
					}
				}

				return selected_items;
			}
		    
			public void DisableMenuItemsThatTakeTooLong() {
				Calendar now = new GregorianCalendar();
				FinishTime finish_time = ParseFinishTime();

				CheckForDuplicateItems();

				if (finish_time.ASAP || DisplayItemsThatTakeTooLong) {
					// nothing disabled if we finish asap
					menuItems.addAll(disabledMenuItems);
					disabledMenuItems.clear();
				} else {
					float max_time_minutes = (float) (finish_time.mFinishTime
							.getTimeInMillis() - now.getTimeInMillis())
							/ (60.f * 1000.f);
					// pop everything in menuItems, then move anything disabled out
					menuItems.addAll(disabledMenuItems);
					disabledMenuItems.clear();
					for (int i = 0; i < menuItems.size(); ++i) {
						AmyMenuItem item = menuItems.get(i);
						if (item.GetTotalTime() > max_time_minutes) {
							disabledMenuItems.add(item);
							menuItems.remove(i);
							--i;
						}
					}
				}

				CheckForDuplicateItems();

				Comparator<AmyMenuItem> sort_by_name = new Comparator<AmyMenuItem>() {
					public int compare(AmyMenuItem a, AmyMenuItem b) {
						return a.Name.compareTo(b.Name);
					}
				};

				Collections.sort(menuItems, sort_by_name);
				Collections.sort(disabledMenuItems, sort_by_name);

				CheckForDuplicateItems();

				adapter.notifyDataSetChanged();

				TextView disabled_items_warning = (TextView) cookTabView.findViewById(R.id.disabledItemsWarning);
				if (disabledMenuItems.size() > 0) {
					disabled_items_warning.setVisibility(View.VISIBLE);

					int count = disabledMenuItems.size();
					String warning = getResources().getQuantityString(
							R.plurals.disabled_items_warning_text_format, count, count);
					disabled_items_warning.setText(warning);
				} else {
					disabled_items_warning.setVisibility(View.GONE);
				}
			}
		    
		    void DisplayDefaultFinishTime() {
				if (DefaultFinishTime == SettingsActivity.DEFAULT_FINISH_TIME_ASAP) {
					DisplayFinishTime(getString(R.string.as_soon_as_possible));
				} else {
					DisplayFinishTime(GetDefaultFinishTime());
				}
			}
		    
			private FinishTime ParseFinishTime() {
				if(cookTabView == null) {
					Log.v(TAG, "Bad things - no cook tab\n");
					return new FinishTime(GetDefaultFinishTime());
				}
				
				Button finish_time_button = (Button) cookTabView.findViewById(R.id.finishTime);
				String finish_time_string = finish_time_button.getText().toString();
				if (finish_time_string.equals(getString(R.string.as_soon_as_possible))) {
					return new FinishTime(true);
				}
				String finish_time_string_lines[] = finish_time_string.split("\\n"); // 1st
																						// line
																						// is
																						// date,
																						// second
																						// is
																						// time

				Calendar finish_time = new GregorianCalendar();
				Date parsed_time;
				
				if(finish_time_string_lines.length > 1) {
					Date parsed_date;
					try {
						parsed_time = DateFormat.getTimeFormat(getActivity()).parse(
								finish_time_string_lines[1]);
						parsed_date = DateFormat.getDateFormat(getActivity()).parse(
								finish_time_string_lines[0]);
					} catch (ParseException e) {
						GlobalApplication.Assert(false, e);
						return new FinishTime(GetDefaultFinishTime());
					}
					
					finish_time.set(Calendar.DATE, parsed_date.getDate());
				}
				else if(finish_time_string_lines.length == 1) {
					// we just have the time - so date is today
					try {
						parsed_time = DateFormat.getTimeFormat(getActivity()).parse(
								finish_time_string_lines[0]);
					} catch (ParseException e) {
						GlobalApplication.Assert(false, e);
						return new FinishTime(GetDefaultFinishTime());
					}
					
					
				}
				else {
					return new FinishTime(GetDefaultFinishTime());
				}
				
				finish_time.set(Calendar.HOUR_OF_DAY, parsed_time.getHours());
				finish_time.set(Calendar.MINUTE, parsed_time.getMinutes());

				Log.d(TAG, "ParseFinishTime read in " + finish_time_string);
				return new FinishTime(finish_time);
			}
			
			public boolean dateIsToday(Calendar time) {
				Calendar now = new GregorianCalendar();
				int now_date = now.get(Calendar.DATE);
				int time_date = time.get(Calendar.DATE);
				return now_date == time_date;
			}

			// Display the selected time in the TextView
			public void DisplayFinishTime(Calendar time) {
				String finish_time_string;
				// is the date today?  if so don't display it
				if(dateIsToday(time)) {
					finish_time_string = DateFormat.getTimeFormat(getActivity()).format(time.getTimeInMillis());
				} else {
					finish_time_string = DateFormat.getDateFormat(getActivity()).format(
							time.getTimeInMillis())
							+ "\n"
							+ DateFormat.getTimeFormat(getActivity()).format(time.getTimeInMillis());
				}
				DisplayFinishTime(finish_time_string);

				DisableMenuItemsThatTakeTooLong();
			}

			public void DisplayFinishTime(String finish_time_string) {
				Button finish_time_button = (Button) cookTabView.findViewById(R.id.finishTime);
				finish_time_button.setText(finish_time_string);

				DisableMenuItemsThatTakeTooLong();
			}
			
			public void DisplayFinishTime(FinishTime finish_time) {
				if(finish_time.ASAP){
					DisplayFinishTime(getString(R.string.as_soon_as_possible));
				} else {
					DisplayFinishTime(finish_time.mFinishTime);
				}
			}
	  }
	  
	  public static class EditTabFragment extends Fragment {
		    View editTabView;
		    
		    @Override
		    public View onCreateView(LayoutInflater inflater, ViewGroup container,
		        Bundle savedInstanceState) {
	
		    	editTabView = inflater.inflate(R.layout.main_screen_edit_tab, container, false);
		    	
		    	//eep!
				MainActivity activity = (MainActivity)getActivity();
				
				// make sure all items are available for edit
				menuItems.addAll(disabledMenuItems);
				disabledMenuItems.clear();
				
				adapter = new MenuItemAdapter(getActivity(),
						R.layout.menu_item, menuItems);
				((MenuItemAdapter) adapter).setCallback(activity);
				
				final ListView lv = (ListView) editTabView.findViewById(android.R.id.list);
				lv.setAdapter(adapter);

				lv.setItemsCanFocus(false);
				lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
				registerForContextMenu(lv);

				activity.UpdateInAppBillingUI();

				return editTabView;
		    }
	  }
	  
	  

	@Override
	public void deletePressed(final int position) {
		CharSequence sure_delete = String.format(
				getString(R.string.sure_delete_menu_item),
				menuItems.get(position).Name);
		new AlertDialog.Builder(this)
				.setTitle(getString(R.string.delete_menu_item))
				.setMessage(sure_delete.toString())
				.setNegativeButton(android.R.string.no, null)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0,
									int arg1) {
								menuItems.remove(position);
								adapter.notifyDataSetChanged();
								SaveMenuItems();
							}
						}).create().show();
	}

	@Override
	public void sharePressed(int position) {
		currentEditItemIndex = position;
		ArrayList<AmyMenuItem> menu_item = new ArrayList<AmyMenuItem>();
		menu_item.add(menuItems.get(currentEditItemIndex));
		shareMenuItems(menu_item);
	}

	@Override
	public void editPressed(int position) {
		currentEditItemIndex = position;
		Intent i = new Intent(this, AddMenuItemActivity.class);
		i.putExtra("menu_item", menuItems.get(currentEditItemIndex));
		startActivityForResult(i, EDIT_MENU_ITEM);
	}

}

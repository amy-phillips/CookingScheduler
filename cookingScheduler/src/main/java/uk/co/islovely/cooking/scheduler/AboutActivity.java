package uk.co.islovely.cooking.scheduler;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class AboutActivity extends Activity {
	
	static String key1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAr";
	
	 @Override
     public void onCreate(Bundle savedInstanceState) {         

        super.onCreate(savedInstanceState);    
        setContentView(R.layout.about);

    }
	 
	 public void CancelAbout() {
		 Intent data = new Intent();
		 setResult(RESULT_CANCELED, data);
		 AboutActivity.super.finish();
	 }
	 
	 public void OnCancel(View v) {
		 CancelAbout();
	 }
	 
	 @Override
	 public void onBackPressed() {
		 CancelAbout();
	 }
	 
	 public void SendEmail(View v) {
		 /* Create the Intent */
		 final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

		 /* Fill it with Data */
		 emailIntent.setType("plain/text");
		 emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"cooking@thinkysaurus.com"});
		 emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Feedback about Cooking Scheduler");
		 emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hello Amy!\n");

		 /* Send it off to the Activity-Chooser */
		 startActivity(Intent.createChooser(emailIntent, "Send mail..."));
	 }
	 
	 public void LaunchHelpSite(View v) {
		 String url = getString(R.string.cooking_scheduler_website);
		 Intent i = new Intent(Intent.ACTION_VIEW);
		 i.setData(Uri.parse(url));
		 startActivity(i); 
	 }
}

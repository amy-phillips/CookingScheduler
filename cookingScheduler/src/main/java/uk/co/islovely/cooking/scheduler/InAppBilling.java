package uk.co.islovely.cooking.scheduler;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.IabResult;
import com.example.android.trivialdrivesample.util.Inventory;
import com.example.android.trivialdrivesample.util.Purchase;

public class InAppBilling {
	private static final String TAG = "InAppBilling";
	static final String SKU_PREMIUM = "paid_version";//"android.test.item_unavailable";//"android.test.refunded";//"android.test.canceled";//"android.test.purchased";//;
	// (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;
    
    IabHelper InAppBillingHelper;
    boolean IsPaidVersion = false;
    MainActivity mActivity;
    
    enum EState {
    	Initialising,
        CheckingPermissions,
    	Purchasing,
    	Ready
    }
    
    public EState CurrentState;
    
    void SetState(EState new_state) {
    	Log.d(TAG, "SetState from "+CurrentState+" to "+new_state);
    	CurrentState = new_state;
    	mActivity.UpdateInAppBillingUI();
    }
    
    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "IAB Query inventory finished.");
            SetState(EState.Ready);
            
            if (result.isFailure()) {
            	GlobalApplication.Assert(false, "Failed to query in app billing inventory: " + result);
                return;
            }

            Log.d(TAG, "IAB Query inventory was successful.");
            
            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */
            
            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
            IsPaidVersion = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            Log.d(TAG, "User is " + (IsPaidVersion ? "paid version" : "free version"));
        }
    };
    
    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        
        /*
         *  verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         * 
         * WARNING: Locally generating a random string when starting a purchase and 
         * verifying it here might seem like a good approach, but this will fail in the 
         * case where the user purchases an item on one device and then uses your app on 
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         * 
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         * 
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on 
         *    one device work on other devices owned by the user).
         * 
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        // already checked permissions before we sent the request that this is the payload for

        AccountManager am = AccountManager.get(mActivity); // "this" references the current Context
        Account[] accounts = am.getAccountsByType("com.google");
        
        //Log.d(TAG, "verifyDeveloperPayload " + payload);
        
        // do any of these accounts match?
        for(Account account : accounts) {
        	Log.d(TAG, "Checking account " + account.name);
        	if(payload.compareTo(account.name) == 0) {
        		return true;
        	}
        }
        
        return false;
    }
    
	public void Initialise(MainActivity activity) {
		mActivity = activity;
		
		SetState(EState.Initialising);
		
		/* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
         * (that you got from the Google Play developer console). This is not your
         * developer public key, it's the *app-specific* public key.
         *
         * Instead of just storing the entire literal string here embedded in the
         * program,  construct the key at runtime from pieces or
         * use bit manipulation (for example, XOR with some other string) to hide
         * the actual key.  The key itself is not secret information, but we don't
         * want to make it easy for an attacker to replace the public key with one
         * of their own and then fake messages from the server.
         */
		
        String base64EncodedPublicKey = AboutActivity.key1 + AddMenuItemActivity.key2 + AlarmReceiver.key3 + AmyMenuItem.key4 + "iBwev0Yh9drAfVv5De4ycLLeJgaaEOauVyobM0nHHY17ezdb" + MainActivity.keyfinal;
        
        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        InAppBillingHelper = new IabHelper(mActivity, base64EncodedPublicKey);
        
        // enable debug logging (for a production application, you should set this to false). 
        InAppBillingHelper.enableDebugLogging(false);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting IAB setup.");
        InAppBillingHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "IAB Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    GlobalApplication.Assert(false, "Problem setting up in-app billing: " + result);
                    return;
                }

                // Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "IAB Setup successful. Querying inventory.");
                InAppBillingHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
	}
	
    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            
            if (result.isFailure()) {
                //GlobalApplication.Assert(false, "Error purchasing: " + result);
            	Log.d(TAG, "Error purchasing: " + result);
            	SetState(EState.Ready);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
            	GlobalApplication.Assert(false, "Error purchasing. Authenticity verification failed.");
            	SetState(EState.Ready);
                return;
            }

            Log.d(TAG, "Purchase successful.");

        	if (purchase.getSku().equals(SKU_PREMIUM)) {
                // bought the premium upgrade!
                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
                IsPaidVersion = true;
            }
        	
        	// do this after we set paidversion to true
        	SetState(EState.Ready);
        }
    };

    public void onPermissionsGranted(int grant_result) {
        AccountManager am = AccountManager.get(mActivity); // "this" references the current Context
        Account[] accounts = am.getAccountsByType("com.google");

        String payload = "google account name";
        if(accounts.length >= 1) {
            // we'll assume that the first listed account is the primary account!
            payload = accounts[0].name;
        }
        else {
            GlobalApplication.Assert(false, "No google account to use for payload?");
            SetState(EState.Ready);
            return;
        }

        SetState(EState.Purchasing);
        InAppBillingHelper.launchPurchaseFlow(mActivity, SKU_PREMIUM, RC_REQUEST,
                mPurchaseFinishedListener, payload);
    }
	
    // User clicked the "Upgrade to Premium" button.
    public void onUpgradeAppButtonClicked() {
        Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
        SetState(EState.CheckingPermissions);

        if(!mActivity.AskForPermission(android.Manifest.permission.GET_ACCOUNTS)) {
            SetState(InAppBilling.EState.Ready);
        }
    }
    
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
    	return InAppBillingHelper.handleActivityResult(requestCode, resultCode, data);
    }

}

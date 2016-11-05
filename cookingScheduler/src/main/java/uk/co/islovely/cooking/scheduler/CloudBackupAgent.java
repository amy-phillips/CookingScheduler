package uk.co.islovely.cooking.scheduler;

import java.io.IOException;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class CloudBackupAgent extends BackupAgentHelper {
	private static final String TAG = "CloudBackupAgent";
	
    // A key to uniquely identify the set of backup data
    static final String FILES_BACKUP_KEY = "menu_files";

    // Allocate a helper and add it to the backup agent
    public void onCreate() {
        FileBackupHelper helper = new FileBackupHelper(this, MainActivity.SAVED_MENU_ITEMS_FILENAME);
        addHelper(FILES_BACKUP_KEY, helper);
    }
    
    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
              ParcelFileDescriptor newState) throws IOException {
        // Hold the lock while the FileBackupHelper performs backup
        synchronized (MainActivity.sDataLock) {
            super.onBackup(oldState, data, newState);
        }
        Log.d(TAG, "Backed up");
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
            ParcelFileDescriptor newState) throws IOException {
        // Hold the lock while the FileBackupHelper restores the file
        synchronized (MainActivity.sDataLock) {
            super.onRestore(data, appVersionCode, newState);
        }
        Log.d(TAG, "Restored");
    }
}

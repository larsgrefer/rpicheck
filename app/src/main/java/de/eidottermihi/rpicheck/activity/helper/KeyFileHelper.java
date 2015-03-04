package de.eidottermihi.rpicheck.activity.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.eidottermihi.rpicheck.R;

/**
 * Created by larsgrefer on 18.11.14.
 */
public class KeyFileHelper {

	//Random Numbers to avoid conflicts
	public static final int REQUEST_OPEN_KEY_FILE = 3409;


	public static Intent getOpenKeyFileIntent() {
		Intent openKeyFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
		openKeyFileIntent.setType("*/*");

		return openKeyFileIntent;
	}

	public static void startOpenKeyFileIntent(final Activity activity) {
		Intent intent = getOpenKeyFileIntent();

		if (intent.resolveActivity(activity.getPackageManager()) != null) {
			logDebug("activity found -> starting intent");
			activity.startActivityForResult(intent, REQUEST_OPEN_KEY_FILE);
		} else {
			//No file picker available
			logDebug("no matching activity found -> suggest file picker installation");
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
			dialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent installFilePickerIntent = new Intent(Intent.ACTION_VIEW);
					installFilePickerIntent.setData(Uri.parse("market://details?layoutId=com.estrongs.android.pop"));
					activity.startActivity(installFilePickerIntent);
				}
			});
			dialogBuilder.setNegativeButton(android.R.string.no, null);
			dialogBuilder.setTitle(R.string.dialog_installFilePicker_title);
			dialogBuilder.setMessage(R.string.dialog_installFilePicker_message);
			dialogBuilder.create().show();
		}
	}

	public static String getKeyFileContentFromActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
		if (resultCode == activity.RESULT_OK && requestCode == REQUEST_OPEN_KEY_FILE) {
			logDebug("intent.getDataString(): " + data.getDataString());
			try {
				return openFile(data.getData(), activity);
			} catch (java.io.IOException e) {
				e.printStackTrace();
				Toast.makeText(activity, R.string.toast_error_openKeyFile, Toast.LENGTH_SHORT).show();
			}
		}
		return null;
	}

	public static String openFile(Uri fileOrContentUri, Activity activity) throws IOException {
		logDebug("openFile( " + fileOrContentUri.toString() + " )");
		InputStream inputStream = null;
		try {
			inputStream = activity.getContentResolver().openInputStream(fileOrContentUri);
			String fileContent = CharStreams.toString(new InputStreamReader(inputStream));
			logDebug("fileContent: " + fileContent);
			return fileContent;
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
	}

	private static void logDebug(String message) {
		Log.d(KeyFileHelper.class.getSimpleName(), message);
	}
}

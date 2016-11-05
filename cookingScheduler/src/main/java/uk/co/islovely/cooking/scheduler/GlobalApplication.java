package uk.co.islovely.cooking.scheduler;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import org.acra.*;
import org.acra.annotation.*;

/* do ACRA imports */
@ReportsCrashes(
        mailTo = "cooking_crash@thinkysaurus.com",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text)
public class GlobalApplication extends Application {

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);

		// The following line triggers the initialization of ACRA
		//ACRA.init(this);
	}

	public static void Assert(boolean condition, String report_string) {
		if(condition)
			return;

		ACRA.getErrorReporter().handleException(null);
	}

	public static void Assert(boolean condition, Exception caught_exception) {
		if(condition)
			return;

		ACRA.getErrorReporter().handleException(caught_exception);
	}

	@SuppressLint("SimpleDateFormat")
	public static String formatCalendar(Calendar calendar) {
		SimpleDateFormat format = new SimpleDateFormat("EEEE, MMMM d, yyyy 'at' hh:mm a");
		return format.format(calendar.getTime());
	}
}

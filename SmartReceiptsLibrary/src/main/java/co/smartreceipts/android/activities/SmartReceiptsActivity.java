package co.smartreceipts.android.activities;

import co.smartreceipts.android.fragments.ReportInfoFragment;
import co.smartreceipts.android.model.Trip;
import wb.android.dialog.BetterDialogBuilder;
import wb.android.ui.UpNavigationSlidingPaneLayout;
import wb.android.util.AppRating;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import co.smartreceipts.android.BuildConfig;
import co.smartreceipts.android.R;
import co.smartreceipts.android.fragments.ReceiptsChartFragment;
import co.smartreceipts.android.fragments.ReceiptsFragment;
import co.smartreceipts.android.fragments.ReceiptsListFragment;
import co.smartreceipts.android.fragments.TripFragment;
import co.smartreceipts.android.model.Attachment;
import co.smartreceipts.android.persistence.Preferences;

public class SmartReceiptsActivity extends WBActivity implements Navigable, Attachable {

	// logging variables
	static final String TAG = "SmartReceiptsActivity";

	// Camera Request Extras
	public static final String STRING_DATA = "strData";
	public static final int DIR = 0;
	public static final int NAME = 1;

	// Action Send Extras
	public static final String ACTION_SEND_URI = "actionSendUri";

	// Receiver Settings
	protected static final String FILTER_ACTION = "co.smartreceipts.android";

	// AppRating (Use a combination of launches and a timer for the app rating
	// to ensure that we aren't prompting new users too soon
	private static final int LAUNCHES_UNTIL_PROMPT = 30;
	private static final int DAYS_UNTIL_PROMPT = 7;

	private boolean mIsDualPane;
	private Attachment mAttachment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        mIsDualPane = getResources().getBoolean(R.bool.isTablet);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
            Log.d(TAG, "savedInstanceState == null");
            getSupportFragmentManager().beginTransaction().replace(R.id.content_list, getTripsFragment(), TripFragment.TAG).addToBackStack(TripFragment.TAG).commit();
			AppRating.initialize(this).setMinimumLaunchesUntilPrompt(LAUNCHES_UNTIL_PROMPT).setMinimumDaysUntilPrompt(DAYS_UNTIL_PROMPT).hideIfAppCrashed(true).setPackageName(getPackageName()).showDialog(true).onLaunch();
		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");

		if (!getSmartReceiptsApplication().getPersistenceManager().getStorageManager().isExternal()) {
			Toast.makeText(SmartReceiptsActivity.this, getSmartReceiptsApplication().getFlex().getString(this, R.string.SD_WARNING), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");

		// Present dialog for viewing an attachment
		final Attachment attachment = new Attachment(getIntent(), getContentResolver());
		setAttachment(attachment);
		if (attachment.isValid() && attachment.isDirectlyAttachable()) {
			final Preferences preferences = getSmartReceiptsApplication().getPersistenceManager().getPreferences();
			final int stringId = attachment.isPDF() ? R.string.pdf : R.string.image;
			if (preferences.showActionSendHelpDialog()) {
				BetterDialogBuilder builder = new BetterDialogBuilder(this);
				builder.setTitle(getString(R.string.dialog_attachment_title, getString(stringId))).setMessage(getString(R.string.dialog_attachment_text, getString(stringId))).setPositiveButton(R.string.dialog_attachment_positive, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				}).setNegativeButton(R.string.dialog_attachment_negative, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						preferences.setShowActionSendHelpDialog(false);
						preferences.commit();
						dialog.cancel();
					}
				}).show();
			}
			else {
				Toast.makeText(this, getString(R.string.dialog_attachment_text, getString(stringId)), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	protected void onDestroy() {
        // TODO: Handle destruction of this DB more generally
		getSmartReceiptsApplication().getPersistenceManager().getDatabase().onDestroy();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (mIsDualPane) {
			getMenuInflater().inflate(R.menu.menu_trip, menu);
		}
		else {
		    // TODO: Leverage the fragment for this instead
            getMenuInflater().inflate(R.menu.menu_trip, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_main_settings) {
			// getSmartReceiptsApplication().getSettings().showSettingsMenu(this);
			SRNavUtils.showSettings(this);
			return true;
		}
		else if (item.getItemId() == R.id.menu_main_export) {
			final Fragment tripsFragment = getSupportFragmentManager().findFragmentByTag(TripFragment.TAG);
			getSmartReceiptsApplication().getSettings().showExport(tripsFragment);
			return true;
		}
		else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public void viewReceiptsAsList(Trip trip) {
		if (!mIsDualPane) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_list, ReportInfoFragment.newInstance(trip), ReportInfoFragment.TAG).addToBackStack(ReportInfoFragment.TAG).commit();
		} else {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_details, ReportInfoFragment.newInstance(trip), ReportInfoFragment.TAG).addToBackStack(ReportInfoFragment.TAG).commit();
        }
	}

	/**
	 * Returns the attachment that is generated via the main activity
	 * 
	 * @return
	 */
	@Override
	public Attachment getAttachment() {
		return mAttachment;
	}

	/**
	 * Stores the main attachment details for later
	 */
	@Override
	public void setAttachment(Attachment attachment) {
		mAttachment = attachment;
	}

	/**
	 * @return - an instance of our {@link TripFragment} class. Allows for subclass flexibility
	 */
	protected TripFragment getTripsFragment() {
		return TripFragment.newInstance();
	}

	/**
	 * @return - an instance of our {@link ReceiptsListFragment} class. Allows for subclass flexibility
	 */
	protected ReceiptsListFragment getReceiptsListFragment(Trip trip) {
		return ReceiptsFragment.newListInstance(trip);
	}

}
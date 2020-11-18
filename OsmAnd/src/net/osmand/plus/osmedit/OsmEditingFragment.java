package net.osmand.plus.osmedit;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.measurementtool.LoginBottomSheetFragment;
import net.osmand.plus.osmedit.ValidateOsmLoginDetailsTask.ValidateOsmLoginListener;
import net.osmand.plus.osmedit.oauth.OsmOAuthAuthorizationAdapter;
import net.osmand.plus.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import static net.osmand.plus.myplaces.FavoritesActivity.TAB_ID;
import static net.osmand.plus.osmedit.OsmEditingPlugin.OSM_EDIT_TAB;

public class OsmEditingFragment extends BaseSettingsFragment implements OnPreferenceChanged, ValidateOsmLoginListener,
		OsmAuthorizationListener {

	public static final String TAG = OsmEditingFragment.class.getName();

	private static final Log log = PlatformUtil.getLog(OsmEditingFragment.class);

	public static final String OPEN_PLUGIN = "open_plugins";
	private static final String OSM_LOGOUT = "osm_logout";
	private static final String OPEN_OSM_EDITS = "open_osm_edits";
	public static final String OSM_LOGIN_DATA = "osm_login_data";
	private static final String OSM_EDITING_INFO = "osm_editing_info";

	private OsmOAuthAuthorizationAdapter authorizationAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		authorizationAdapter = app.getOsmOAuthHelper().getAuthorizationAdapter();
	}

	@Override
	protected void setupPreferences() {
		Preference osmEditingInfo = findPreference(OSM_EDITING_INFO);
		osmEditingInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupLoginPref();
		setupLogoutPref();

		setupOfflineEditingPref();
		setupOsmEditsDescrPref();
		setupOsmEditsPref();
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);

		TextView toolbarSubtitle = view.findViewById(R.id.toolbar_subtitle);
		toolbarSubtitle.setText(getPreferenceScreen().getSummary());
		AndroidUiHelper.updateVisibility(toolbarSubtitle, true);
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (OSM_EDITING_INFO.equals(preference.getKey())) {
			TextView titleView = (TextView) holder.findViewById(android.R.id.title);
			titleView.setTextSize(16);
		}
	}

	@Override
	public void loginValidationFinished(String warning) {
		updateAllSettings();
	}

	private void setupLoginPref() {
		Preference nameAndPasswordPref = findPreference(OSM_LOGIN_DATA);
		if (!isValidToken() && !isLoginExists()) {
			nameAndPasswordPref.setIcon(getContentIcon(R.drawable.ic_action_user_account));
			nameAndPasswordPref.setVisible(true);
		} else {
			nameAndPasswordPref.setVisible(false);
		}
	}

	private void setupLogoutPref() {
		boolean validToken = isValidToken();
		Preference nameAndPasswordPref = findPreference(OSM_LOGOUT);
		if (validToken || isLoginExists()) {
			String userName = validToken ? settings.USER_DISPLAY_NAME.get() : settings.USER_NAME.get();
			nameAndPasswordPref.setVisible(true);
			nameAndPasswordPref.setSummary(userName);
			nameAndPasswordPref.setIcon(getContentIcon(R.drawable.ic_action_user_account));
		} else {
			nameAndPasswordPref.setVisible(false);
		}
	}

	private boolean isValidToken() {
		return authorizationAdapter.isValidToken();
	}

	private boolean isLoginExists() {
		return !Algorithms.isEmpty(settings.USER_NAME.get()) && !Algorithms.isEmpty(settings.USER_PASSWORD.get());
	}

	private void setupOfflineEditingPref() {
		Drawable disabled = getContentIcon(R.drawable.ic_action_offline);
		Drawable enabled = getActiveIcon(R.drawable.ic_world_globe_dark);
		Drawable icon = getPersistentPrefIcon(enabled, disabled);

		SwitchPreferenceEx offlineEditingPref = findPreference(settings.OFFLINE_EDITION.getId());
		offlineEditingPref.setDescription(getString(R.string.offline_edition_descr));
		offlineEditingPref.setIcon(icon);
	}

	private void setupOsmEditsDescrPref() {
		String menu = getString(R.string.shared_string_menu);
		String myPlaces = getString(R.string.shared_string_my_places);
		String osmEdits = getString(R.string.osm_edits);
		String osmEditsPath = getString(R.string.ltr_or_rtl_triple_combine_via_dash, menu, myPlaces, osmEdits);
		String osmEditsPathDescr = getString(R.string.osm_edits_view_descr, osmEditsPath);

		int startIndex = osmEditsPathDescr.indexOf(osmEditsPath);
		SpannableString titleSpan = new SpannableString(osmEditsPathDescr);
		if (startIndex != -1) {
			Typeface typeface = FontCache.getRobotoMedium(getContext());
			titleSpan.setSpan(new CustomTypefaceSpan(typeface), startIndex, startIndex + osmEditsPath.length(), 0);
		}

		Preference osmEditsDescription = findPreference("osm_edits_description");
		osmEditsDescription.setTitle(titleSpan);
	}

	private void setupOsmEditsPref() {
		Preference createProfile = findPreference(OPEN_OSM_EDITS);
		createProfile.setIcon(getActiveIcon(R.drawable.ic_action_folder));
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (OPEN_OSM_EDITS.equals(prefId)) {
			Bundle bundle = new Bundle();
			bundle.putInt(TAB_ID, OSM_EDIT_TAB);

			OsmAndAppCustomization appCustomization = app.getAppCustomization();
			Intent favorites = new Intent(preference.getContext(), appCustomization.getFavoritesActivity());
			favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			favorites.putExtra(MapActivity.INTENT_PARAMS, bundle);
			startActivity(favorites);
			return true;
		} else if (OSM_LOGIN_DATA.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				LoginBottomSheetFragment.showInstance(fragmentManager, this);
				return true;
			}
		} else if (OSM_LOGOUT.equals(prefId)) {
			if (isValidToken()) {
				settings.USER_ACCESS_TOKEN.resetToDefault();
				settings.USER_ACCESS_TOKEN_SECRET.resetToDefault();

				authorizationAdapter.resetToken();
			} else {
				settings.USER_NAME.resetToDefault();
				settings.USER_PASSWORD.resetToDefault();
			}
			app.showShortToastMessage(R.string.osm_edit_logout_success);
			updateAllSettings();
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onPreferenceChanged(String prefId) {
		updateAllSettings();
	}

	@Override
	public void authorizationCompleted() {
		updateAllSettings();
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			OsmEditingFragment fragment = new OsmEditingFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
package net.osmand.plus.osmedit.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmEditingFragment;
import net.osmand.plus.osmedit.OsmPoint;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.HashMap;
import java.util.Map;

import static net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment.*;

public class SendPoiBottomSheetFragment extends MenuBottomSheetDialogFragment {

    public static final String TAG = SendPoiBottomSheetFragment.class.getSimpleName();
    private static final Log LOG = PlatformUtil.getLog(SendPoiBottomSheetFragment.class);
    private OsmPoint[] poi;

    private SwitchCompat closeChangeSet;
    private EditText messageEditText;


    private boolean isLoginOAuth(OsmandSettings settings) {
        return !Algorithms.isEmpty(settings.USER_DISPLAY_NAME.get());
    }

    @Override
    public void createMenuItems(Bundle savedInstanceState) {
        OsmandApplication app = getMyApplication();
        if (app == null) {
            return;
        }
        poi = (OsmPoint[]) getArguments().getSerializable(OPENSTREETMAP_POINT);
        final boolean isNightMode = app.getDaynightHelper().isNightModeForMapControls();
        final View sendOsmPoiView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
                R.layout.send_poi_fragment, null);
        closeChangeSet = sendOsmPoiView.findViewById(R.id.close_change_set_checkbox);
        messageEditText = sendOsmPoiView.findViewById(R.id.message_field);
        String defaultChangeSet = createDefaultChangeSet(app);
        messageEditText.setText(defaultChangeSet);
        messageEditText.setSelection(messageEditText.getText().length());
        final TextView accountName = sendOsmPoiView.findViewById(R.id.user_name);
        OsmandSettings settings = app.getSettings();
        String userNameOAuth = settings.USER_DISPLAY_NAME.get();
        String userNameOpenID = settings.USER_NAME.get();
        String userName = isLoginOAuth(settings) ? userNameOAuth : userNameOpenID;
        accountName.setText(userName);
        closeChangeSet.setBackgroundResource(isNightMode ? R.drawable.layout_bg_dark : R.drawable.layout_bg);
        final int paddingSmall = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
        closeChangeSet.setPadding(paddingSmall, 0, paddingSmall, 0);
        closeChangeSet.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isNightMode) {
                    closeChangeSet.setBackgroundResource(
                            isChecked ? R.drawable.layout_bg_dark_solid : R.drawable.layout_bg_dark);
                } else {
                    closeChangeSet.setBackgroundResource(
                            isChecked ? R.drawable.layout_bg_solid : R.drawable.layout_bg);
                }
                closeChangeSet.setPadding(paddingSmall, 0, paddingSmall, 0);
            }
        });
        LinearLayout account = sendOsmPoiView.findViewById(R.id.account_container);
        account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                showOpenStreetMapScreen();
            }
        });
        final SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
                .setCustomView(sendOsmPoiView)
                .create();
        items.add(titleItem);
    }

    private void showOpenStreetMapScreen() {
        Bundle params = new Bundle();
        params.putBoolean(OsmEditingFragment.OPEN_PLUGIN, true);
        Context context = getView().getContext();
        Intent intent = getActivity().getIntent();
        MapActivity.launchMapActivityMoveToTop(context, intent != null ? intent.getExtras() : null, null, params);
    }

    public static void showInstance(@NonNull FragmentManager fm, @NonNull OsmPoint[] points) {
        try {
            if (!fm.isStateSaved()) {
                SendPoiBottomSheetFragment fragment = new SendPoiBottomSheetFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable(OPENSTREETMAP_POINT, points);
                fragment.setArguments(bundle);
                fragment.show(fm, TAG);
            }
        } catch (RuntimeException e) {
            LOG.error("showInstance", e);
        }
    }

    @Override
    protected UiUtilities.DialogButtonType getRightBottomButtonType() {
        return (UiUtilities.DialogButtonType.PRIMARY);
    }

    @Override
    protected void onRightBottomButtonClick() {
        ProgressDialogPoiUploader progressDialogPoiUploader = null;
        Activity activity = getActivity();
        if (activity instanceof MapActivity) {
            progressDialogPoiUploader = new SimpleProgressDialogPoiUploader((MapActivity) activity);
        } else if (getParentFragment() instanceof ProgressDialogPoiUploader) {
            progressDialogPoiUploader = (ProgressDialogPoiUploader) getParentFragment();
        }
        if (progressDialogPoiUploader != null) {
            String comment = messageEditText.getText().toString();
            if (comment.length() > 0) {
                for (OsmPoint osmPoint : poi) {
                    if (osmPoint.getGroup() == OsmPoint.Group.POI) {
                        ((OpenstreetmapPoint) osmPoint).setComment(comment);
                        break;
                    }
                }
            }
            progressDialogPoiUploader.showProgressDialog(poi, closeChangeSet.isChecked(), false);
        }
        dismiss();
    }

    @Override
    protected int getRightBottomButtonTextId() {
        return R.string.shared_string_upload;
    }

    private String createDefaultChangeSet(OsmandApplication app) {
        Map<String, PoiType> allTranslatedSubTypes = app.getPoiTypes().getAllTranslatedNames(true);
        if (allTranslatedSubTypes == null) {
            return "";
        }
        Map<String, Integer> addGroup = new HashMap<>();
        Map<String, Integer> editGroup = new HashMap<>();
        Map<String, Integer> deleteGroup = new HashMap<>();
        Map<String, Integer> reopenGroup = new HashMap<>();
        String comment = "";
        for (OsmPoint p : poi) {
            if (p.getGroup() == OsmPoint.Group.POI) {
                OsmPoint.Action action = p.getAction();
                String type = ((OpenstreetmapPoint) p).getEntity().getTag(Entity.POI_TYPE_TAG);
                if (type == null) {
                    continue;
                }
                PoiType localizedPoiType = allTranslatedSubTypes.get(type.toLowerCase().trim());
                if (localizedPoiType != null) {
                    type = Algorithms.capitalizeFirstLetter(localizedPoiType.getKeyName().replace('_', ' '));
                }
                if (action == OsmPoint.Action.CREATE) {
                    if (!addGroup.containsKey(type)) {
                        addGroup.put(type, 1);
                    } else {
                        addGroup.put(type, addGroup.get(type) + 1);
                    }
                } else if (action == OsmPoint.Action.MODIFY) {
                    if (!editGroup.containsKey(type)) {
                        editGroup.put(type, 1);
                    } else {
                        editGroup.put(type, editGroup.get(type) + 1);
                    }
                } else if (action == OsmPoint.Action.DELETE) {
                    if (!deleteGroup.containsKey(type)) {
                        deleteGroup.put(type, 1);
                    } else {
                        deleteGroup.put(type, deleteGroup.get(type) + 1);
                    }
                } else if (action == OsmPoint.Action.REOPEN) {
                    if (!reopenGroup.containsKey(type)) {
                        reopenGroup.put(type, 1);
                    } else {
                        reopenGroup.put(type, reopenGroup.get(type) + 1);
                    }
                }
            }
        }
        int modifiedItemsOutOfLimit = 0;
        for (int i = 0; i < 4; i++) {
            String action;
            Map<String, Integer> group;
            switch (i) {
                case 0:
                    action = getString(R.string.default_changeset_add);
                    group = addGroup;
                    break;
                case 1:
                    action = getString(R.string.default_changeset_edit);
                    group = editGroup;
                    break;
                case 2:
                    action = getString(R.string.default_changeset_delete);
                    group = deleteGroup;
                    break;
                case 3:
                    action = getString(R.string.default_changeset_reopen);
                    group = reopenGroup;
                    break;
                default:
                    action = "";
                    group = new HashMap<>();
            }

            if (!group.isEmpty()) {
                int pos = 0;
                for (Map.Entry<String, Integer> entry : group.entrySet()) {
                    String type = entry.getKey();
                    int quantity = entry.getValue();
                    if (comment.length() > 200) {
                        modifiedItemsOutOfLimit += quantity;
                    } else {
                        if (pos == 0) {
                            comment = comment.concat(comment.length() == 0 ? "" : "; ").concat(action).concat(" ")
                                    .concat(quantity == 1 ? "" : quantity + " ").concat(type);
                        } else {
                            comment = comment.concat(", ").concat(quantity == 1 ? "" : quantity + " ").concat(type);
                        }
                    }
                    pos++;
                }
            }
        }
        if (modifiedItemsOutOfLimit != 0) {
            comment = comment.concat("; ").concat(modifiedItemsOutOfLimit + " ")
                    .concat(getString(R.string.items_modified)).concat(".");
        } else if (!comment.equals("")) {
            comment = comment.concat(".");
        }
        return comment;
    }
}


package net.osmand.plus.settings.fragments;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.ExportSettingsCategory;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.FileSettingsItem;
import net.osmand.view.ThreeStateCheckbox;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.view.ThreeStateCheckbox.State.CHECKED;
import static net.osmand.view.ThreeStateCheckbox.State.MISC;
import static net.osmand.view.ThreeStateCheckbox.State.UNCHECKED;

public class ExportSettingsAdapter extends OsmandBaseExpandableListAdapter {

	private static final Log LOG = PlatformUtil.getLog(ExportImportSettingsAdapter.class.getName());

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;

	private List<ExportSettingsCategory> itemsTypes = new ArrayList<>();
	private Map<ExportSettingsType, List<?>> selectedItemsMap = new HashMap<>();
	private Map<ExportSettingsCategory, List<ExportDataObject>> itemsMap = new LinkedHashMap<>();

	private final OnItemSelectedListener listener;

	private final LayoutInflater themedInflater;

	private final boolean nightMode;
	private final int activeColorRes;
	private final int secondaryColorRes;

	ExportSettingsAdapter(OsmandApplication app, OnItemSelectedListener listener, boolean nightMode) {
		this.app = app;
		this.listener = listener;
		this.nightMode = nightMode;
		uiUtilities = app.getUIUtilities();
		themedInflater = UiUtilities.getInflater(app, nightMode);
		activeColorRes = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
		secondaryColorRes = nightMode ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View group = convertView;
		if (group == null) {
			group = themedInflater.inflate(R.layout.profile_data_list_item_group, parent, false);
		}
		final ExportSettingsCategory category = itemsTypes.get(groupPosition);
		final List<ExportDataObject> items = itemsMap.get(category);

		String title = app.getString(category.getTitleId());
		TextView titleTv = group.findViewById(R.id.title_tv);
		titleTv.setText(UiUtilities.createCustomFontSpannable(FontCache.getRobotoMedium(app), title, title));

		TextView subTextTv = group.findViewById(R.id.sub_text_tv);
		subTextTv.setText(getCategoryDescr(category));

		int selectedTypes = 0;
		for (int i = 0; i < items.size(); i++) {
			ExportDataObject object = items.get(i);
			if (selectedItemsMap.containsKey(object.getType())) {
				selectedTypes++;
			}
		}
		final ThreeStateCheckbox checkBox = group.findViewById(R.id.check_box);
		if (selectedTypes == 0) {
			checkBox.setState(UNCHECKED);
		} else {
			checkBox.setState(selectedTypes == items.size() ? CHECKED : MISC);
		}
		int checkBoxColor = checkBox.getState() == UNCHECKED ? secondaryColorRes : activeColorRes;
		CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(ContextCompat.getColor(app, checkBoxColor)));

		group.findViewById(R.id.check_box_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				checkBox.performClick();
				boolean selected = checkBox.getState() == CHECKED;
				if (selected) {
					for (ExportDataObject object : items) {
						if (!selectedItemsMap.containsKey(object.getType())) {
							selectedItemsMap.put(object.getType(), object.getItems());
						}
					}
				} else {
					for (ExportDataObject object : items) {
						selectedItemsMap.remove(object.getType());
					}
				}
				if (listener != null) {
					listener.onCategorySelected(category, selected);
				}
				notifyDataSetChanged();
			}
		});

		adjustIndicator(app, groupPosition, isExpanded, group, nightMode);
		AndroidUiHelper.updateVisibility(group.findViewById(R.id.divider), isExpanded);
		AndroidUiHelper.updateVisibility(group.findViewById(R.id.card_top_divider), true);
		AndroidUiHelper.updateVisibility(group.findViewById(R.id.vertical_divider), false);
		AndroidUiHelper.updateVisibility(group.findViewById(R.id.card_bottom_divider), !isExpanded);

		return group;
	}

	@Override
	public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View child = convertView;
		if (child == null) {
			child = themedInflater.inflate(R.layout.profile_data_list_item_group, parent, false);
		}
		final ExportDataObject currentItem = itemsMap.get(itemsTypes.get(groupPosition)).get(childPosition);
		List<?> selectedItems = selectedItemsMap.get(currentItem.getType());

		TextView titleTv = child.findViewById(R.id.title_tv);
		titleTv.setText(currentItem.getType().getTitleId());

		TextView subTextTv = child.findViewById(R.id.sub_text_tv);
		subTextTv.setText(getSelectedTypeDescr(currentItem));

		ImageView icon = child.findViewById(R.id.explist_indicator);
		setupIcon(icon, currentItem.getType().getIconRes(), selectedItems != null);

		final ThreeStateCheckbox checkBox = child.findViewById(R.id.check_box);
		if (selectedItems == null) {
			checkBox.setState(UNCHECKED);
		} else if (selectedItems.containsAll(currentItem.getItems())) {
			checkBox.setState(CHECKED);
		} else {
			boolean contains = false;
			for (Object object : currentItem.getItems()) {
				if (selectedItems.contains(object)) {
					contains = true;
					break;
				}
			}
			checkBox.setState(contains ? MISC : UNCHECKED);
		}

		int checkBoxColor = checkBox.getState() == UNCHECKED ? secondaryColorRes : activeColorRes;
		CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(ContextCompat.getColor(app, checkBoxColor)));
		child.findViewById(R.id.check_box_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				checkBox.performClick();
				boolean selected = checkBox.getState() == CHECKED;
				if (selected) {
					selectedItemsMap.put(currentItem.getType(), currentItem.getItems());
				} else {
					selectedItemsMap.remove(currentItem.getType());
				}
				if (listener != null) {
					listener.onTypeSelected(currentItem.getType(), selected);
				}
				notifyDataSetChanged();
			}
		});

		AndroidUiHelper.updateVisibility(child.findViewById(R.id.card_bottom_divider), isLastChild);

		return child;
	}

	@Override
	public int getGroupCount() {
		return itemsTypes.size();
	}

	@Override
	public int getChildrenCount(int i) {
		return itemsMap.get(itemsTypes.get(i)).size();
	}

	@Override
	public Object getGroup(int i) {
		return itemsMap.get(itemsTypes.get(i));
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return itemsMap.get(itemsTypes.get(groupPosition)).get(childPosition);
	}

	@Override
	public long getGroupId(int i) {
		return i;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return groupPosition * 10000 + childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int i, int i1) {
		return true;
	}

	private void setupIcon(ImageView icon, int iconRes, boolean itemSelected) {
		if (itemSelected) {
			int colorRes = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_osmand_light;
			icon.setImageDrawable(uiUtilities.getIcon(iconRes, colorRes));
		} else {
			icon.setImageDrawable(uiUtilities.getIcon(iconRes, secondaryColorRes));
		}
	}

	public void updateSettingsList(Map<ExportSettingsCategory, List<ExportDataObject>> itemsMap) {
		this.itemsMap = itemsMap;
		this.itemsTypes = new ArrayList<>(itemsMap.keySet());
		Collections.sort(itemsTypes);
		notifyDataSetChanged();
	}

	public void clearSettingsList() {
		this.itemsMap.clear();
		this.itemsTypes.clear();
		notifyDataSetChanged();
	}

	public boolean hasSelectedData() {
		return !selectedItemsMap.isEmpty();
	}

	public List<? super Object> getData() {
		List<Object> selectedItems = new ArrayList<>();
		for (List<?> items : selectedItemsMap.values()) {
			selectedItems.addAll(items);
		}
		return selectedItems;
	}

	private String getCategoryDescr(ExportSettingsCategory category) {
		long itemsSize = 0;
		int selectedTypes = 0;
		List<ExportDataObject> items = itemsMap.get(category);
		for (int i = 0; i < items.size(); i++) {
			ExportDataObject object = items.get(i);
			if (selectedItemsMap.containsKey(object.getType())) {
				selectedTypes++;
				itemsSize += calculateItemsSize(object.getItems());
			}
		}
		String description;
		if (selectedTypes == 0) {
			description = app.getString(R.string.shared_string_none);
		} else if (selectedTypes == items.size()) {
			description = app.getString(R.string.shared_string_all);
		} else {
			description = app.getString(R.string.ltr_or_rtl_combine_via_slash, String.valueOf(selectedTypes), String.valueOf(items.size()));
		}
		String formattedSize = AndroidUtils.formatSize(app, itemsSize);
		return itemsSize == 0 ? description : app.getString(R.string.ltr_or_rtl_combine_via_comma, description, formattedSize);
	}

	public static long calculateItemsSize(List<?> items) {
		long itemsSize = 0;
		for (Object item : items) {
			if (item instanceof FileSettingsItem) {
				itemsSize += ((FileSettingsItem) item).getSize();
			} else if (item instanceof File) {
				itemsSize += ((File) item).length();
			}
		}
		return itemsSize;
	}

	private String getSelectedTypeDescr(ExportDataObject dataObject) {
		long itemsSize = 0;
		int selectedTypes = 0;

		List<?> items = dataObject.getItems();
		List<?> selectedItems = selectedItemsMap.get(dataObject.getType());
		if (selectedItems != null) {
			for (int i = 0; i < items.size(); i++) {
				Object object = items.get(i);
				if (selectedItems.contains(object)) {
					selectedTypes++;
					if (object instanceof FileSettingsItem) {
						itemsSize += ((FileSettingsItem) object).getSize();
					} else if (object instanceof File) {
						itemsSize += ((File) object).length();
					}
				}
			}
		}
		String description;
		if (selectedTypes == 0) {
			description = app.getString(R.string.shared_string_none);
		} else if (selectedTypes == items.size()) {
			description = app.getString(R.string.shared_string_all);
			if (itemsSize == 0) {
				description = app.getString(R.string.ltr_or_rtl_combine_via_comma, description, String.valueOf(items.size()));
			}
		} else {
			description = app.getString(R.string.ltr_or_rtl_combine_via_slash, String.valueOf(selectedTypes), String.valueOf(items.size()));
		}
		String formattedSize = AndroidUtils.formatSize(app, itemsSize);
		return itemsSize == 0 ? description : app.getString(R.string.ltr_or_rtl_combine_via_comma, description, formattedSize);
	}

	interface OnItemSelectedListener {

		void onCategorySelected(ExportSettingsCategory type, boolean selected);

		void onTypeSelected(ExportSettingsType type, boolean selected);

	}
}
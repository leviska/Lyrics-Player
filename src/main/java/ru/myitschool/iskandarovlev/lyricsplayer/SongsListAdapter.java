package ru.myitschool.iskandarovlev.lyricsplayer;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;

/*
 * Адаптер для SongsListActivity
 */
public class SongsListAdapter extends BaseExpandableListAdapter implements CompoundButton.OnCheckedChangeListener {
	private ArrayList<String> artists;
	private ArrayList<ArrayList<Pair<String, String>>> songs;
	private ArrayList<ArrayList<Boolean>> checked = new ArrayList<>();
	private LayoutInflater lInflater;

	public SongsListAdapter(ArrayList<String> artists, ArrayList<ArrayList<Pair<String, String>>> songs, SongsListActivity context) {
		this.artists = new ArrayList<>(artists);
		this.songs = new ArrayList<>(songs);
		lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		for(int i = 0; i < artists.size(); i++) {
			ArrayList<Boolean> temp = new ArrayList<>();
			for(int j = 0; j < songs.get(i).size(); j++) {
				temp.add(false);
			}
			checked.add(temp);
		}
	}

	@Override
	public int getGroupCount() {
		return artists.size();
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return songs.get(groupPosition).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return artists.get(groupPosition);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return songs.get(groupPosition).get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = lInflater.inflate(R.layout.song_list_group_item, parent, false);
		}
		((TextView) view.findViewById(R.id.songlist_group_textview)).setText(artists.get(groupPosition));

		return view;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = lInflater.inflate(R.layout.song_list_item, parent, false);
		}
		((TextView) view.findViewById(R.id.song_list_item_textview)).setText(songs.get(groupPosition).get(childPosition).first);
		CheckBox checkBox = (CheckBox) view.findViewById(R.id.song_item_checkbox);
		checkBox.setOnCheckedChangeListener(this);
		checkBox.setTag(R.id.GROUP_TAG, groupPosition);
		checkBox.setTag(R.id.CHILD_TAG, childPosition);
		checkBox.setChecked(checked.get(groupPosition).get(childPosition));
		return view;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		checked.get((int) buttonView.getTag(R.id.GROUP_TAG)).set((int) buttonView.getTag(R.id.CHILD_TAG), isChecked);
	}

	public ArrayList<String> getChecked() {
		ArrayList<String> temp = new ArrayList<>();
		for(int i = 0; i < artists.size(); i++) {
			for(int j = 0; j < songs.get(i).size(); j++) {
				if(checked.get(i).get(j)) {
					temp.add(songs.get(i).get(j).second);
				}
			}
		}
		return temp;
	}
}

package ru.myitschool.iskandarovlev.lyricsplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/*
 * Адаптер для PlaylistManager активити
 */
public class PlaylistsManagerAdapter extends BaseAdapter {
	PlaylistsManager context;
	ArrayList<String> playlists;
	ArrayList<Integer> songs;
	LayoutInflater lInflater;

	public PlaylistsManagerAdapter(PlaylistsManager context, ArrayList<String> playlists, ArrayList<Integer> songs) {
		this.context = context;
		this.playlists = playlists;
		this.songs = songs;
		lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return playlists.size();
	}

	@Override
	public Object getItem(int position) {
		return playlists.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = lInflater.inflate(R.layout.playlists_manager_item, parent, false);
		}

		((TextView) view.findViewById(R.id.playlists_manager_item_name)).setText(playlists.get(position));
		((TextView) view.findViewById(R.id.playlists_manager_item_songs)).setText(context.getString(R.string.songs) + " " + songs.get(position));
		view.findViewById(R.id.playlists_manager_item_menu).setOnClickListener(context);
		view.findViewById(R.id.playlists_manager_item_menu).setTag(playlists.get(position));

		return view;
	}
}

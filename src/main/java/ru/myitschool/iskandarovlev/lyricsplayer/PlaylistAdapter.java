package ru.myitschool.iskandarovlev.lyricsplayer;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItemAdapter;

import java.util.ArrayList;

/*
 * Адаптер для PlaylistActivity
 */
public class PlaylistAdapter extends DragItemAdapter<Pair<Long, Pair<String, String>>, PlaylistAdapter.PlaylistViewHolder> {

	private int mGrabHandleId;
	private int mLayoutId;
	private PlaylistActivity context;

	public PlaylistAdapter(ArrayList<Pair<Long, Pair<String, String>>> data, int layoutId, int grabHandleId, boolean dragOnLongPress, PlaylistActivity context) {
		super(dragOnLongPress);
		mLayoutId = layoutId;
		mGrabHandleId = grabHandleId;
		this.context = context;
		setHasStableIds(true);
		setItemList(data);
	}

	@Override
	public PlaylistViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
		return new PlaylistViewHolder(view);
	}

	@Override
	public void onBindViewHolder(PlaylistViewHolder holder, int position) {
		super.onBindViewHolder(holder, position);
		String name = mItemList.get(position).second.first;
		String artist = mItemList.get(position).second.second;
		holder.tvName.setText(name);
		holder.tvArtist.setText(artist);
		holder.position = position;
		holder.ibMenu.setTag(position);
		holder.ibMenu.setOnClickListener(context);
	}

	@Override
	public long getItemId(int position) {
		return mItemList.get(position).first;
	}

	public class PlaylistViewHolder extends DragItemAdapter<Pair<Long, Pair<String, String>>, PlaylistAdapter.PlaylistViewHolder>.ViewHolder {
		public TextView tvName;
		public TextView tvArtist;
		public ImageButton ibMenu;
		public int position;

		public PlaylistViewHolder(final View itemView) {
			super(itemView, mGrabHandleId);
			tvName = (TextView) itemView.findViewById(R.id.playlist_item_song_name);
			tvArtist = (TextView) itemView.findViewById(R.id.playlist_item_song_artist);
			ibMenu = (ImageButton) itemView.findViewById(R.id.playlist_item_menu);
		}

		@Override
		public void onItemClicked(View view) {
			context.setSong(position);
		}
	}
}

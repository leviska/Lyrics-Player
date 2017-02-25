package ru.myitschool.iskandarovlev.lyricsplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.TreeSet;

/*
 * Активити для добавления новых песен в плейлист
 */

public class SongsListActivity extends AppCompatActivity {
	private final String LOG_TAG = "SongsListActivity";
	//ListView
	private ExpandableListView lv;
	private SongsListAdapter adapter;
	private String playlistName;
	//Объекты для работы с сервисом
	private boolean bound;
	private ServiceConnection sConn;

	/*
	 * Жизненный цикл активити
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_songs_list);
		Log.d(LOG_TAG, "onCreate: Running");

		playlistName = getIntent().getStringExtra("name");
		lv = (ExpandableListView) findViewById(R.id.songslist_listview);

		sConn = new ServiceConnection() {//Подключаемся к сервису просто для того, чтобы он работал

			public void onServiceConnected(ComponentName name, IBinder binder) {
				Log.d(LOG_TAG, "onServiceConnected: Running");
				bound = true;
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.d(LOG_TAG, "onServiceDisconnected: Running");
				bound = false;
			}
		};

		new PrepareLayout().execute(); //Потому что это может быть долго
	}

	@Override
	protected void onStart() {
		Log.d(LOG_TAG, "onStart: Running");
		super.onStart();
		Intent intent = new Intent("ru.myitschool.lyricplayer.playbackservice");
		bindService(intent, sConn, BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		Log.d(LOG_TAG, "onStop: Running");
		super.onStop();
		if (!bound) return;
		unbindService(sConn);
	}

	/*
	 *
	 */

	//Закончил добавление
	public void doneAdding(View view) {
		Log.d(LOG_TAG, "doneAdding: Running");
		ArrayList<String> temp = adapter.getChecked();
		if(playlistName == null || playlistName.isEmpty()) playlistName = getString(R.string.default_playlist);
		Playlist playlist = FileManager.loadPlaylist(playlistName, true, this);
		for(int i = 0; i < temp.size(); i++) {
			Song song = new Song(temp.get(i));
			if(!playlist.getSongsList().contains(song)) {
				ArrayList<Song> temp_arr = playlist.getSongsList();
				temp_arr.add(song);
				playlist.setSongsList(temp_arr);
			}
		}
		FileManager.savePlaylist(playlist, this);
		Intent intent = new Intent(this, PlaylistActivity.class);
		intent.putExtra("isNew", true);
		intent.putExtra("name", playlistName);
		Log.d(LOG_TAG, playlistName);
		startActivity(intent);
	}

	//Загрузка
	class PrepareLayout extends AsyncTask<Void, Void, SongsListAdapter> {

		@Override
		protected SongsListAdapter doInBackground(Void... params) {
			Log.d(LOG_TAG, "doInBackground: Running");
			//Алгоритм, делающий из (String[имена] и String[исполнители]) (String[исполнители] и String[*исполнители*][имена])
			ArrayList<Song> songs = FileManager.loadStandardPlaylist(true).getSongsList();
			TreeSet<String> artists_set = new TreeSet<>();
			ArrayList<ArrayList<Pair<String, String>>> songs_names = new ArrayList<>(artists_set.size());
			for (int i = 0; i < songs.size(); i++) {
				artists_set.add(songs.get(i).getArtist());
			}
			for (int i = 0; i < artists_set.size(); i++) {
				songs_names.add(new ArrayList<Pair<String, String>>());
			}
			for (int i = 0; i < songs.size(); i++) {
				String artist = songs.get(i).getArtist();
				String name = songs.get(i).getName();
				String path = songs.get(i).getPath().getAbsolutePath();
				songs_names.get(artists_set.headSet(artist).size()).add(new Pair<>(name, path));
			}
			ArrayList<String> normal_artists = new ArrayList<>(artists_set);
			return new SongsListAdapter(normal_artists, songs_names, SongsListActivity.this);
		}

		@Override
		protected void onPostExecute(SongsListAdapter songListAdapter) {
			Log.d(LOG_TAG, "onPostExecute: Running");
			adapter = songListAdapter;
			super.onPostExecute(adapter);
			findViewById(R.id.songslist_progress).setVisibility(View.INVISIBLE);
			lv.setAdapter(songListAdapter);
			lv.setVisibility(View.VISIBLE);
		}
	}
}

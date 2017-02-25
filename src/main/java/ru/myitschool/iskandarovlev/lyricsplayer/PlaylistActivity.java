package ru.myitschool.iskandarovlev.lyricsplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.woxthebox.draglistview.DragItem;
import com.woxthebox.draglistview.DragListView;

import java.io.File;
import java.util.ArrayList;

/*
 * Активити редактирования текущего плейлиста
 */

public class PlaylistActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, DragListView.DragListListener, View.OnLongClickListener, View.OnClickListener {
	private String LOG_TAG = "PlaylistActivity";
	//Объекты для работы с сервисом
	private PlaybackService service;
	private Intent intent;
	private boolean bound;
	private ServiceConnection sConn;
	private BroadcastReceiver br;
	//Плейлист
	private Playlist playlist;
	//ListView-объекты
	private DragListView lv;
	private PlaylistAdapter listAdapter;
	//Является ли плейлист тем же, что и в сервисе
	private boolean isNew = false;

	/*
	 * Жизненный цикл активити
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "onCreate: Running");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playlist);

		//Инициализация navigation drawer
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_playlist);
		setSupportActionBar(toolbar);
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.addDrawerListener(toggle);
		toggle.syncState();
		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view_playlist);
		navigationView.setNavigationItemSelectedListener(this);

		//Инициализация listview
		lv = (DragListView) findViewById(R.id.playlist_songs);

		//Получение информации о плейлисте
		final Intent dataIntent = getIntent();
		isNew = dataIntent.getBooleanExtra("isNew", false);

		intent = new Intent("ru.myitschool.lyricplayer.playbackservice"); //Подключение к сервису
		sConn = new ServiceConnection() {

			public void onServiceConnected(ComponentName name, IBinder binder) {
				Log.d(LOG_TAG, "onServiceConnected: Running");
				service = ((PlaybackService.PlaybackServiceBinder) binder).getService();
				bound = true;
				TextView playlistNameTV = (TextView) findViewById(R.id.playlist_name);
				playlistNameTV.setOnLongClickListener(PlaylistActivity.this);
				if(isNew) {
					String playlistName = dataIntent.getStringExtra("name");
					playlistNameTV.setText(playlistName);
					playlist = FileManager.loadPlaylist(playlistName, true, PlaylistActivity.this);
				}
				else {
					playlistNameTV.setText(service.getCurrentPlaylist().getPlaylistName());
					playlist = FileManager.loadPlaylist(service.getCurrentPlaylist().getPlaylistName(), true, PlaylistActivity.this);
				}
				prepareLayout();
				loadNavHeader();
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.d(LOG_TAG, "onServiceDisconnected: Running");
				bound = false;
			}
		};

		br = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) { //Для nav header
				Log.d(LOG_TAG, "receive: song changed");
				final Handler handler = new Handler();
				Runnable brRunWait = new Runnable() {
					@Override
					public void run() {
						while(!bound) handler.postDelayed(this, 100);
						loadNavHeader();
					}
				};
				handler.postDelayed(brRunWait, 100);
			}
		};
	}

	@Override
	protected void onStart() {
		Log.d(LOG_TAG, "onStart: Running");
		super.onStart();
		bindService(intent, sConn, BIND_AUTO_CREATE);
		// создаем фильтр для BroadcastReceiver
		IntentFilter intFilt = new IntentFilter(PlaybackService.BROADCAST_ACTION);
		// регистрируем (включаем) BroadcastReceiver
		registerReceiver(br, intFilt);
	}

	@Override
	protected void onStop() {
		Log.d(LOG_TAG, "onStop: Running");
		super.onStop();
		if (!bound) return;
		unbindService(sConn);
		FileManager.savePlaylist(playlist, this);
		unregisterReceiver(br);
	}

	/*
	 * Navigation drawer функции
	 */

	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		Log.d(LOG_TAG, "onNavigationItemSelected: Running");
		int id = item.getItemId();
		Intent intent;
		switch(id) {
			case R.id.main_activity_menu:
				intent = new Intent(this, MainActivity.class);
				startActivity(intent);
				break;
			case R.id.current_playlist_menu:
				if(isNew) {
					intent = new Intent(this, PlaylistActivity.class);
					intent.putExtra("isNew", false);
					startActivity(intent);
				}
				break;
			case R.id.playlists_manager_item_menu:
				intent = new Intent(this, PlaylistsManager.class);
				startActivity(intent);
				break;
		}
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return false;
	}

	private void loadNavHeader() {
		Log.d(LOG_TAG, "loadNavHeader: Running");
		Song currentSong = service.getSong();
		View header = ((NavigationView)findViewById(R.id.nav_view_playlist)).getHeaderView(0);
		((TextView) header.findViewById(R.id.song_name_navhead)).setText(currentSong.getName());
		((TextView) header.findViewById(R.id.artist_navhead)).setText(currentSong.getArtist());
	}

	@Override
	public boolean onKeyDown(int keycode, KeyEvent e) { //Кнопка меню открывает и закрывает боковое меню
		switch (keycode) {
			case KeyEvent.KEYCODE_MENU:
				Log.d(LOG_TAG, "onKeyDown: Running");
				DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
				if (drawer.isDrawerOpen(GravityCompat.START)) {
					drawer.closeDrawer(GravityCompat.START);
				} else {
					drawer.openDrawer(GravityCompat.START);
				}
				return true;
		}
		return super.onKeyDown(keycode, e);
	}


	/*
	 * ListView функции
	 */

	@Override
	public void onItemDragStarted(int position) {}

	@Override
	public void onItemDragging(int itemPosition, float x, float y) {}

	@Override
	public void onItemDragEnded(int fromPosition, int toPosition) {
		Log.d(LOG_TAG, "onItemDragEnded: Running");
		ArrayList<Song> songs_temp = playlist.getSongsList();
		Song temp = songs_temp.get(fromPosition);
		songs_temp.remove(fromPosition);
		songs_temp.add(toPosition, temp);
		playlist.setSongsList(songs_temp);
		if(!isNew) {
			service.setCurrentPlaylist(playlist);
			int oldId = service.getCurrentSongId();
			int newId = oldId;
			if(oldId == fromPosition) newId = toPosition;
			else if(oldId < fromPosition && oldId >= toPosition) newId++;
			else if(oldId > fromPosition && oldId <= toPosition) newId--;
			service.setCurrentSongId(newId);
		}
	}

	private void prepareLayout() {
		Log.d(LOG_TAG, "prepareLayout: Running");
		lv.getRecyclerView().setVerticalScrollBarEnabled(true);
		lv.setDragListListener(this);
		lv.setLayoutManager(new LinearLayoutManager(this));
		lv.setCanDragHorizontally(false);
		lv.setCustomDragItem(new MyDragItem(this, R.layout.playlist_item));
		ArrayList<String> names = new ArrayList<>();
		ArrayList<String> artists = new ArrayList<>();
		ArrayList<Pair<Long, Pair<String, String>>> data = new ArrayList<>();
		for(int i = 0; i <  playlist.getSongsList().size(); i++) {
			names.add(playlist.getSongsList().get(i).getName());
			artists.add(playlist.getSongsList().get(i).getArtist());
			Pair<String, String> temp_1 = new Pair<>(names.get(i), artists.get(i));
			Pair<Long, Pair<String, String>> temp_2 = new Pair<>((long)i, temp_1);
			data.add(temp_2);
		}
		listAdapter = new PlaylistAdapter(data, R.layout.playlist_item, R.id.playlistLayout, true, PlaylistActivity.this);
		lv.setAdapter(listAdapter, true);
	}

	/*
	 * Обработчики нажатий
	 */

	//Обработчик нажатий на кнопку меню около каждой песни
	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
			case R.id.playlist_item_menu:
				Log.d(LOG_TAG, "onClick: Running");
				PopupMenu popupMenu = new PopupMenu(this, v);
				popupMenu.inflate(R.menu.playlist_song_menu);
				popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
							case R.id.playlist_menu_play:
								setSong((int) v.getTag());
								return true;
							case R.id.playlist_menu_delete:
								ArrayList<Song> temp = playlist.getSongsList();
								int thisId = (int) v.getTag();
								temp.remove(thisId);
								playlist.setSongsList(temp);
								if(!isNew) {
									service.setCurrentPlaylist(playlist);
									if(thisId == service.getCurrentSongId()) service.nextSong();
									else if (thisId < service.getCurrentSongId()) service.setCurrentSongId(service.getCurrentSongId() - 1);
								}
								listAdapter.removeItem((int) v.getTag());
								listAdapter.notifyDataSetChanged();
								return true;
							default:
								return false;
						}
					}
				});
				popupMenu.show();
				break;
		}
	}

	//Обработчик долгого нажатия на имя плейлиста
	@Override
	public boolean onLongClick(final View v) {
		if(v.getId() == R.id.playlist_name) {
			Log.d(LOG_TAG, "onLongClick: Running");
			final ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.playlist_name_switcher);
			switcher.showNext();
			final EditText editName = (EditText) findViewById(R.id.playlist_edit_name);
			editName.setText(((TextView) v).getText());
			editName.requestFocus();
			final InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			keyboard.showSoftInput(editName, 0);
			editName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						editName.clearFocus();
						return true;
					}
					return false;
				}
			});
			editName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View view, boolean hasFocus) {
					if (!hasFocus) {
						((TextView) v).setText(editName.getText());
						switcher.showPrevious();
						String oldName = playlist.getPlaylistName();
						new File(Environment.getExternalStorageDirectory().toString() + "/Playlists/" + oldName + ".m3u8").renameTo(new File(Environment.getExternalStorageDirectory().toString() + "/Playlists/" + editName.getText().toString() + ".m3u8"));
						playlist.setPlaylistName(editName.getText().toString());
						if(!isNew) service.setCurrentPlaylist(playlist);
						keyboard.hideSoftInputFromWindow(editName.getWindowToken(), 0);
					}
				}
			});
		}
		return false;
	}

	//При нажатии на песню, вызывается эта функция, которая воспроизводит нужную песню и меняет плейлист, если надо
	public void setSong(int position) {
		Log.d(LOG_TAG, "setSong: Running");
		if(bound) {
			if(isNew) service.setCurrentPlaylist(playlist);
			FileManager.savePlaylist(playlist, this);
			service.setSong(position);
			service.continuePlayback();
		}
		else Toast.makeText(this, "Ошибка: нет подключения к сервису", Toast.LENGTH_LONG).show();
		intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}

	//Обработчик нажатия на кнопку добавления песен
	public void addSongs(View view) {
		Log.d(LOG_TAG, "addSongs: Running");
		Intent intent = new Intent(this, SongsListActivity.class);
		intent.putExtra("name", playlist.getPlaylistName());
		startActivity(intent);
	}

	//Обработчик нажатия на кнопку возврата
	@Override
	public void onBackPressed() { //При нажатии кнопки возврата проверяем, если открыто боковое меню - закрываем, если нет - открываем нужное активити
		Log.d(LOG_TAG, "onBackPressed: Running");
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			if(isNew) {
				Intent intent = new Intent(this, PlaylistsManager.class);
				startActivity(intent);
			}
			else {
				Intent intent = new Intent(this, MainActivity.class);
				startActivity(intent);
			}
		}
	}

	//Класс для listview
	private static class MyDragItem extends DragItem {

		public MyDragItem(Context context, int layoutId) {
			super(context, layoutId);
		}

		@Override
		public void onBindDragView(View clickedView, View dragView) {
			CharSequence name = ((TextView) clickedView.findViewById(R.id.playlist_item_song_name)).getText();
			CharSequence artist = ((TextView) clickedView.findViewById(R.id.playlist_item_song_artist)).getText();
			((TextView) dragView.findViewById(R.id.playlist_item_song_name)).setText(name);
			((TextView) dragView.findViewById(R.id.playlist_item_song_artist)).setText(artist);
			dragView.setBackgroundColor(ContextCompat.getColor(dragView.getContext(), R.color.colorPlaylistItem));
		}
	}

}

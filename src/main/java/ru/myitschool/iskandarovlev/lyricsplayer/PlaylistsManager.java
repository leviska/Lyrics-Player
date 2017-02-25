package ru.myitschool.iskandarovlev.lyricsplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

/*
 * Активити для просмотра текущих плейлистов
 */

public class PlaylistsManager extends AppCompatActivity implements AdapterView.OnItemClickListener, NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
	private final String LOG_TAG = "PlaylistsManager";
	//Путь до sd карты
	private final String MEDIA_PATH = Environment.getExternalStorageDirectory().toString();
	//Плейлисты
	private ArrayList<String> playlists = new ArrayList<>();
	//Переменные, для работы с сервисом
	private PlaybackService service;
	private boolean bound = false;
	ServiceConnection sConn;
	private BroadcastReceiver br;

	/*
	 * Жизненный цикл активити
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manager);
		Log.d(LOG_TAG, "onCreate: Running");

		//Инициализация navigation drawer
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_manager);
		setSupportActionBar(toolbar);
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.addDrawerListener(toggle);
		toggle.syncState();
		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view_manager);
		navigationView.setNavigationItemSelectedListener(this);

		//Подключение к сервису
		sConn = new ServiceConnection() {
			public void onServiceConnected(ComponentName componentName, IBinder binder) {
				Log.d(LOG_TAG, "onServiceConnected: Running");
				service = ((PlaybackService.PlaybackServiceBinder) binder).getService();
				bound = true;
				loadNavHeader();
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.d(LOG_TAG, "onServiceDisconnected: Running");
				bound = false;
			}
		};
		prepareListview();

		//Для nav header
		br = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
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
		bindService(new Intent("ru.myitschool.lyricplayer.playbackservice"), sConn, BIND_AUTO_CREATE);

		IntentFilter intFilt = new IntentFilter(PlaybackService.BROADCAST_ACTION);
		registerReceiver(br, intFilt);
	}

	@Override
	protected void onStop() {
		Log.d(LOG_TAG, "onStop: Running");
		super.onStop();
		unregisterReceiver(br);
		if (!bound) return;
		unbindService(sConn);
	}

	//Подготовка listview
	private void prepareListview() {
		Log.d(LOG_TAG, "prepareListview: Running");

		File directory = new File(MEDIA_PATH + "/Playlists/");
		File[] listFiles = directory.listFiles();
		playlists = new ArrayList<>();
		ArrayList<Integer> songs = new ArrayList<>();

		//Сканирование всех плейлистов
		if (listFiles != null && listFiles.length > 0) {
			for (File file : listFiles) {
				if (!file.isDirectory() && file.getName().endsWith(".m3u8")) {
					playlists.add(file.getName().substring(0, file.getName().indexOf(".m3u8")));
				}
			}
		}

		if(playlists != null && !playlists.isEmpty()) {
			//Подсчет песен
			for(String str : playlists) {
				Playlist playlist = FileManager.loadPlaylist(str, false, this);
				songs.add(playlist.getSongsList().size());
			}
			//Вывод
			ListView listView = (ListView) findViewById(R.id.playlist_manager_list);
			PlaylistsManagerAdapter adapter = new PlaylistsManagerAdapter(this, playlists, songs);
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(this);
		}
		else {
			Toast.makeText(this, "Плейлисты не найдены. Перезапустите приложение", Toast.LENGTH_LONG).show();
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
		}
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
				intent = new Intent(this, PlaylistActivity.class);
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
		View header = ((NavigationView) findViewById(R.id.nav_view_manager)).getHeaderView(0);
		((TextView) header.findViewById(R.id.song_name_navhead)).setText(currentSong.getName());
		((TextView) header.findViewById(R.id.artist_navhead)).setText(currentSong.getArtist());
	}

	@Override
	public void onBackPressed() { //При нажатии кнопки возврата проверяем, если открыто боковое меню - закрываем, если нет - действие по умолчанию
		Log.d(LOG_TAG, "onBackPressed: Running");
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
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
	 * Обработчики нажатий
	 */

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Log.d(LOG_TAG, "onItemClick: Running");
		Intent intent = new Intent(this, PlaylistActivity.class);
		intent.putExtra("isNew", true);
		intent.putExtra("name", ((TextView) view.findViewById(R.id.playlists_manager_item_name)).getText() + "");
		startActivity(intent);
	}

	//Нажатие на кнопку меню у плейлиста
	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
			case R.id.playlists_manager_item_menu:
				PopupMenu popupMenu = new PopupMenu(this, v);
				popupMenu.inflate(R.menu.playlists_manager_menu);
				popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						final String name = v.getTag().toString();
						switch (item.getItemId()) {
							case R.id.playlist_menu_edit:
								Intent intent = new Intent(PlaylistsManager.this, PlaylistActivity.class);
								intent.putExtra("isNew", true);
								intent.putExtra("name", name);
								startActivity(intent);
								return true;
							case R.id.playlist_menu_delete: {
								File file = new File(MEDIA_PATH + "/Playlists/" + name + ".m3u8");
								if(file.exists()) file.delete();
								if(service.getCurrentPlaylist().getPlaylistName().equals(name)) {
									int i = playlists.indexOf(name);
									if(i == 0 && playlists.size() == 1) i = -1;
									else if(i == playlists.size() - 1) i--;
									else i++;
									Log.d(LOG_TAG, "i = " + i);
									if(i >= 0)	service.setCurrentPlaylist(FileManager.loadPlaylist(playlists.get(i), true, PlaylistsManager.this));
									else service.setCurrentPlaylist(FileManager.loadStandardPlaylist(true));
									service.pausePlayback();
									service.setSong(0);
								}
								prepareListview();
								return true;
							}
							default:
								return false;
						}
					}
				});
				popupMenu.show();
				break;
		}
	}

	//Создание нового плейлиста
	public void addNewPlaylist(View view) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(getString(R.string.playlist_new_name_text));
		final EditText input = new EditText(this);
		input.setHint(R.string.playlist_new_name);
		alert.setView(input);

		alert.setPositiveButton("Ок", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String name = input.getText().toString();
				if (name.isEmpty()) {
					name = getString(R.string.playlist_new_name);
					if (new File(MEDIA_PATH + "/Playlists/" + name + ".m3u8").exists()) {
						name += " (1)";
						for (int i = 2; true; i++) {
							if (new File(MEDIA_PATH + "/Playlists/" + name + ".m3u8").exists()) {
								name = name.replace("(" + (i - 1) + ")", "(" + i + ")");
							}
							else break;
						}
					}
				}
				Playlist playlist = new Playlist(name);
				FileManager.savePlaylist(playlist, PlaylistsManager.this);
				Intent intent = new Intent(PlaylistsManager.this, SongsListActivity.class);
				intent.putExtra("name", name);
				startActivity(intent);
			}
		});

		alert.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});

		alert.show();
	}
}

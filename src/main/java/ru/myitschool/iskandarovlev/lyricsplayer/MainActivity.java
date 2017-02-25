package ru.myitschool.iskandarovlev.lyricsplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.util.ArrayList;

/*
 * Главное активити
 * Управление воспроизведением, просмотр текстов, просмотр информации о песне
 */

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, SeekBar.OnSeekBarChangeListener, View.OnLongClickListener {
	private final String LOG_TAG = "LyricPlayer";
	//Переменные проигрывания музыки
	private Song currentSong;
	private boolean isPlaying;
	private boolean isShuffle;
	private boolean isRepeat;
	//View-объекты
	private ImageButton playBtn;
	private ImageButton repeatBtn;
	private ImageButton shuffleBtn;
	private SeekBar seekBar;
	private TextView currentDuration;
	private TextView totalDuration;
	private TextView songName;
	private TextView songArtist;
	private TextView lyricsOutput;
	private ImageView coverView;
	private ProgressBar loadLyrics;
	private Button researchLyrics;
	private Button nextLyrics;
	//Объекты для работы с сервисом
	private PlaybackService service;
	private Intent intent;
	private boolean bound;
	private ServiceConnection sConn;
	private BroadcastReceiver br;
	//Другие переменные
	private Handler handler = new Handler(); //Обновляет seekbar

	/*
	 * Жизненный цикл активити
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d(LOG_TAG, "onCreate: Running");

		//Инициализация navigation drawer
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
		setSupportActionBar(toolbar);
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.addDrawerListener(toggle);
		toggle.syncState();
		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view_main);
		navigationView.setNavigationItemSelectedListener(this);

		//Инициализация view-объектов
		playBtn = (ImageButton) findViewById(R.id.play_btn);
		repeatBtn = (ImageButton) findViewById(R.id.repeat_btn);
		shuffleBtn = (ImageButton) findViewById(R.id.shuffle_btn);
		currentDuration = (TextView) findViewById(R.id.current_duration);
		totalDuration = (TextView) findViewById(R.id.total_duration);
		songName = (TextView) findViewById(R.id.song_name);
		songArtist = (TextView) findViewById(R.id.song_artist);
		songName.setOnLongClickListener(this);
		songArtist.setOnLongClickListener(this);
		lyricsOutput = (TextView) findViewById(R.id.lyrics_output);
		coverView = (ImageView) findViewById(R.id.cover_view);
		loadLyrics = (ProgressBar) findViewById(R.id.loading_lyrics);
		seekBar = (SeekBar) findViewById(R.id.seek_bar);
		researchLyrics = (Button) findViewById(R.id.reload_btn);
		nextLyrics = (Button) findViewById(R.id.ignore_reload_btn);
		seekBar.setOnSeekBarChangeListener(this);


		//Разное
		intent = new Intent("ru.myitschool.lyricplayer.playbackservice"); //Подключение к сервису
		sConn = new ServiceConnection() {

			public void onServiceConnected(ComponentName name, IBinder binder) {
				Log.d(LOG_TAG, "onServiceConnected: Running");
				service = ((PlaybackService.PlaybackServiceBinder) binder).getService();
				bound = true;
				isPlaying = service.isPlaying();
				isPlaying = !isPlaying;
				onPlayBtnListener(new View(MainActivity.this));
				boolean isStarted = service.isStarted();
				if (!isStarted) {
					startService(intent);
				}
				prepareLayout();
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.d(LOG_TAG, "onServiceDisconnected: Running");
				bound = false;
			}
		};

		br = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) { //Обновление navigation header
				Log.d(LOG_TAG, "receive: song changed");
				Runnable brRunWait = new Runnable() {
					@Override
					public void run() {
						while(!bound) handler.postDelayed(this, 100);
						prepareLayout();
					}
				};
				handler.postDelayed(brRunWait, 100);
			}
		};

		handler.postDelayed(setCoverViewHeight, 100); //Устанавливаем высоту imageview обложки равным высоте scrollview. Я не знаю, почему оно не работает сразу, этому методу надо подождать :/
	}

	@Override
	protected void onStart() {
		Log.d(LOG_TAG, "onStart: Running");
		super.onStart();
		bindService(intent, sConn, BIND_AUTO_CREATE);

		IntentFilter intFilt = new IntentFilter(PlaybackService.BROADCAST_ACTION);
		registerReceiver(br, intFilt);
	}

	@Override
	protected void onStop() {
		Log.d(LOG_TAG, "onStop: Running");
		super.onStop();
		if (bound) {
			if (!isPlaying) {
				stopService(intent);
				service.stopService();
			}
			unbindService(sConn);
		}
		unregisterReceiver(br);
		handler.removeCallbacks(updateSeekBar);
	}

	/*
     * Перехватчики нажатий кнопок
     */

	public void onPlayBtnListener(View view) 	{
		Log.d(LOG_TAG, "onPlayBtnListener: Running");
		if (isPlaying) {
			playBtn.setImageResource(R.drawable.ic_action_play);
			service.pausePlayback();
			isPlaying = false;
			handler.removeCallbacks(updateSeekBar);
		} else {
			playBtn.setImageResource(R.drawable.ic_action_pause);
			service.continuePlayback();
			isPlaying = true;
			handler.postDelayed(updateSeekBar, 100);
		}
	}

	public void onPreviousBtnListener(View view) {
		Log.d(LOG_TAG, "onPreviousBtnListener: Running");
		service.previousSong();
		isPlaying = false;
		onPlayBtnListener(new View(this));
	}

	public void onNextBtnListener(View view) {
		Log.d(LOG_TAG, "onNextBtnListener: Running");
		service.nextSong();
		isPlaying = false;
		onPlayBtnListener(new View(this));
	}

	public void onRepeatBtnListener(View view) {
		Log.d(LOG_TAG, "onRepeatBtnListener: Running");
		if (isRepeat) {
			isRepeat = false;
			repeatBtn.setImageResource(R.drawable.ic_action_repeat);
		} else {
			isRepeat = true;
			repeatBtn.setImageResource(R.drawable.ic_action_repeat_pressed);
			isShuffle = false;
			shuffleBtn.setImageResource(R.drawable.ic_action_shuffle);
		}
		service.setRepeat(isRepeat);
		service.setShuffle(isShuffle);
	}

	public void onShuffleBtnListener(View view) {
		Log.d(LOG_TAG, "onShuffleBtnListener: Running");
		if (isShuffle) {
			isShuffle = false;
			shuffleBtn.setImageResource(R.drawable.ic_action_shuffle);
		} else {
			isShuffle = true;
			isRepeat = false;
			shuffleBtn.setImageResource(R.drawable.ic_action_shuffle_pressed);
			repeatBtn.setImageResource(R.drawable.ic_action_repeat);
		}
		service.setRepeat(isRepeat);
		service.setShuffle(isShuffle);
	}

	public void onReloadBtnListener(View view) {
		Log.d(LOG_TAG, "onReloadBtnListener: running");
		setLyricsOutput(currentSong, true);
	}

	public void onIgnoreReloadBtnListener(View view) {
		Log.d(LOG_TAG, "onIgnoreReloadBtnListener: running");
		if (currentSong.getLyricsSize() - 1 == currentSong.getChosenLyrics()) {
			currentSong.setChosenLyrics(0);
			lyricsOutput.setText(currentSong.getLyrics(currentSong.getChosenLyrics()));
			Toast.makeText(this, getString(R.string.lyrics_ended), Toast.LENGTH_LONG).show();
		} else {
			currentSong.setChosenLyrics(currentSong.getChosenLyrics() + 1);
			lyricsOutput.setText(currentSong.getLyrics(currentSong.getChosenLyrics()));
		}
	}

	@Override
	public boolean onLongClick(final View v) { //Для смены названия песен
		switch (v.getId()) {
			case R.id.song_name: {
				final ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.song_name_switcher);
				switcher.showNext();
				final EditText edittext = (EditText) findViewById(R.id.song_name_edit);
				edittext.setText(((TextView) v).getText());
				edittext.requestFocus();
				final InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				keyboard.showSoftInput(edittext, 0);
				edittext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_DONE) {
							edittext.clearFocus();
							return true;
						}
						return false;
					}
				});
				edittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View view, boolean hasFocus) {
						if (!hasFocus) {
							switcher.showPrevious();
							ArrayList<Song> playlist = service.getCurrentPlaylist().getSongsList();
							playlist.get(playlist.indexOf(currentSong)).setName(edittext.getText().toString());
							Playlist playlist_temp = service.getCurrentPlaylist();
							playlist_temp.setSongsList(playlist);
							service.setCurrentPlaylist(playlist_temp);
							FileManager.savePlaylist(playlist_temp, MainActivity.this);
							prepareLayout();
							keyboard.hideSoftInputFromWindow(edittext.getWindowToken(), 0);
						}
					}
				});
				break;
			}
			case R.id.song_artist: {
				final ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.song_artist_switcher);
				switcher.showNext();
				final EditText edittext = (EditText) findViewById(R.id.song_artist_edit);
				edittext.setText(((TextView) v).getText());
				edittext.requestFocus();
				final InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				keyboard.showSoftInput(edittext, 0);
				edittext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_DONE) {
							edittext.clearFocus();
							return true;
						}
						return false;
					}
				});
				edittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View view, boolean hasFocus) {
						if (!hasFocus) {
							switcher.showPrevious();
							ArrayList<Song> playlist = service.getCurrentPlaylist().getSongsList();
							playlist.get(playlist.indexOf(currentSong)).setArtist(edittext.getText().toString());
							Playlist playlist_temp = service.getCurrentPlaylist();
							playlist_temp.setSongsList(playlist);
							service.setCurrentPlaylist(playlist_temp);
							FileManager.savePlaylist(playlist_temp, MainActivity.this);
							prepareLayout();
							keyboard.hideSoftInputFromWindow(edittext.getWindowToken(), 0);
						}
					}
				});
				break;
			}

		}
		return false;
	}

	/*
	 * Другие перехватчики
	 */

	/*
	 * Seekbar
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		Log.d(LOG_TAG, "onStartTrackingTouch: running");
		handler.removeCallbacks(updateSeekBar);
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		Log.d(LOG_TAG, "onStopTrackingTouch: running");
		service.movePlayback((int) currentSong.getDuration() * seekBar.getProgress() / 100);
		handler.postDelayed(updateSeekBar, 100);
	}

	/*
	 * Navigation drawer функции
	 */

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

	@Override
	public boolean onNavigationItemSelected(MenuItem item) { //Выбор меню
		Log.d(LOG_TAG, "onNavigationItemSelected: Running");
		int id = item.getItemId();
		Intent intent;
		switch (id) {
			case R.id.current_playlist_menu:
				intent = new Intent(this, PlaylistActivity.class);
				intent.putExtra("isNew", false);
				startActivity(intent);
				break;
			case R.id.playlists_manager_item_menu:
				intent = new Intent(this, PlaylistsManager.class);
				startActivity(intent);
				break;
		}
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}

	private void loadNavHeader() { //Установка значений nav header
		Log.d(LOG_TAG, "loadNavHeader: Running");
		Song currentSong = service.getSong();
		View header = ((NavigationView) findViewById(R.id.nav_view_main)).getHeaderView(0);
		((TextView) header.findViewById(R.id.song_name_navhead)).setText(currentSong.getName());
		((TextView) header.findViewById(R.id.artist_navhead)).setText(currentSong.getArtist());
	}

	/*
	 * Объекты-интерфейсы
	 */

	private Runnable setCoverViewHeight = new Runnable() { //Для установки высоты показа картинки
		@Override
		public void run() {
			ScrollView cover_lyrics_scrollView = (ScrollView) findViewById(R.id.cover_lyrics_scrollView);
			ViewGroup.LayoutParams params = coverView.getLayoutParams();
			params.height = cover_lyrics_scrollView.getHeight();
			if (cover_lyrics_scrollView.getHeight() == 0)
				handler.postDelayed(setCoverViewHeight, 100);
			else coverView.setLayoutParams(params);
		}
	};

	private Runnable updateSeekBar = new Runnable() { //Обновление seekbar
		@Override
		public void run() {
			//Log.d(LOG_TAG, "updateSeekBar: running");
			int totalDutation = (int) currentSong.getDuration();
			int currentPosition = service.getCurrentPosition();
			int progress = (int) (((float) currentPosition / (float) totalDutation) * 100);
			seekBar.setProgress(progress);
			currentDuration.setText(getTimeFromMilliseconds(currentPosition));
			handler.postDelayed(updateSeekBar, 100);
		}
	};

	/*
	 * Другие функции
	 */

	private void prepareLayout() { //Подготовка layout
		Log.d(LOG_TAG, "prepareLayout: Running");
		currentSong = service.getSong();
		Log.d(LOG_TAG, "prepareLayout: " + currentSong.getName() + " : " + currentSong.getPath());
		currentDuration.setText("0:00");
		totalDuration.setText(getTimeFromMilliseconds(currentSong.getDuration()));
		songName.setText(currentSong.getName());
		songArtist.setText(currentSong.getArtist());
		Bitmap cover = currentSong.getCoverFromFile();
		if (cover != null) coverView.setImageBitmap(cover);
		else coverView.setImageResource(R.drawable.ic_action_cover);
		setLyricsOutput(currentSong, false);
		loadNavHeader();
	}

	public static String getTimeFromMilliseconds(long milliseconds) { //Получение из миллисекунд строку с временем
		//Log.d(LOG_TAG, "getTimeFromMilliseconds: Running");
		long s = roundUp(milliseconds, 1000);
		long m = roundUp(s, 60);
		long h = roundUp(m, 60);
		s %= 60;
		m %= 60;
		String time = "";
		if (h > 0) {
			time = h + ":";
		}
		time += m + ":";
		if (s < 10) time += "0";
		time += s;
		return time;
	}

	private static long roundUp(long first, long second) { //Округление
		//Log.d(LOG_TAG, "roundUp: Running");
		if (first / second < 1) return 0;
		else return (long) Math.ceil((double) first / second) - 1;
	}

	private void setLyricsOutput(final Song song, boolean force) { //Вывод текста. Force - принудительная загрузка из интернета (false - можно загружать с sd карты, true - только из интернета)
		Log.d(LOG_TAG, "setLyricsOutput: Running");
		ArrayList<String> loadedLyrics;
		if (force) loadedLyrics = null;
		else loadedLyrics = FileManager.loadLyrics(currentSong.getPath().getName(), this);
		if (loadedLyrics == null) {
			lyricsOutput.setText(getResources().getString(R.string.search_lyrics));
			coverView.setVisibility(View.INVISIBLE);
			loadLyrics.setVisibility(View.VISIBLE);
			researchLyrics.setVisibility(View.INVISIBLE);
			nextLyrics.setVisibility(View.INVISIBLE);
			RunnableManager.createRunnable(song.getArtist(), song.getName(), song.getPath().getAbsolutePath(), this);
			RunnableManager.run();
		} else {
			nextLyrics.setVisibility(View.INVISIBLE);
			foundLyrics(loadedLyrics, currentSong.getName(), currentSong.getArtist(), currentSong.getPath().getAbsolutePath(), true);
		}
	}

	public void foundLyrics(ArrayList<String> lyrics, String name, String artist, String path, boolean isError) { //Когда найден текст
		Log.d(LOG_TAG, "foundLyrics: Running");
		if (currentSong.getPath().getAbsolutePath().equals(path)) {
			Log.d(LOG_TAG, "foundLyrics: " + lyrics.size());
			lyricsOutput.setText(lyrics.get(0));
			currentSong.deleteLyrics();
			for (int i = 0; i < lyrics.size(); i++) {
				currentSong.setLyrics(lyrics.get(i), i);
				currentSong.setChosenLyrics(0);
			}
			coverView.setVisibility(View.VISIBLE);
			loadLyrics.setVisibility(View.INVISIBLE);
			researchLyrics.setVisibility(View.VISIBLE);
			if (lyrics.size() > 1) nextLyrics.setVisibility(View.VISIBLE);
			if (!isError) {
				FileManager.saveLyrics(lyrics, name, artist, currentSong.getPath().getName(), this);
			}
		} else {
			Log.d(LOG_TAG, "foundLyrics: miss song");
		}
	}
}

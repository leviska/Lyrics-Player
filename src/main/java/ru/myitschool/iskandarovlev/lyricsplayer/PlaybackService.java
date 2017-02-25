package ru.myitschool.iskandarovlev.lyricsplayer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Random;

/*
 * Класс сервиса воспроизведения
 * Работает с медиаплеерем, пытается быть неубиваемым...
 */

public class PlaybackService extends Service implements MediaPlayer.OnCompletionListener {
	private final String LOG_TAG = "PlaybackService";
	//Медиаплеер
	private MediaPlayer mp;
	//Информация о воспроизведении
	private Playlist currentPlaylist;
	private int currentSongId;
	private boolean isRepeat;
	private boolean isShuffle;
	//Для bind'инга
	private PlaybackServiceBinder binder = new PlaybackServiceBinder();
	private boolean isStarted = false;
	//Уведомления
	private final int notificationID = 1410;
	private	NotificationCompat.Builder builder;
	//Путь до sd карты
	private final String MEDIA_PATH = Environment.getExternalStorageDirectory().toString();
	//При изменении текущей песни нужно обновлять информацию в боковом меню, для этого делаем broadcast общение
	public final static String BROADCAST_ACTION = "ru.myitschool.iskandarovlev.navheadertext";

	/*
	 * Жизненный цикл сервиса
	 */

	public void onCreate() {
		Log.d(LOG_TAG, "onCreate: running");
		super.onCreate();

		mp = new MediaPlayer();
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
		currentSongId = 0;
		isRepeat = false;
		isShuffle = false;

		mp.setOnCompletionListener(this);
		loadCurrentPlaylist();
	}

	public void onDestroy() {
		Log.d(LOG_TAG, "onDestroy: running");
		saveCurrentPlaylist();
		mp.stop();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(LOG_TAG, "onBind: running");
		return binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int id) {
		super.onStartCommand(intent, flags, id);
		Log.d(LOG_TAG, "onStartCommand: running");
		if(!isStarted) {
			isStarted = true;
			//Создание уведомления для startForeground
			if(currentPlaylist == null || currentPlaylist.getSongsList() == null || currentPlaylist.getSongsList().isEmpty()) {
				builder = new NotificationCompat.Builder(this)
						.setContentTitle("Loading")
						.setContentText("Wait for loading song")
						.setSmallIcon(R.drawable.ic_action_play);
			}
			else {
				builder = new NotificationCompat.Builder(this)
						.setContentTitle(currentPlaylist.getSong(currentSongId).getName())
						.setContentText(currentPlaylist.getSong(currentSongId).getArtist())
						.setSmallIcon(R.drawable.ic_action_play);
			}
			Intent resultIntent = new Intent(this, MainActivity.class);
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
			stackBuilder.addParentStack(MainActivity.class);
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
			builder.setContentIntent(resultPendingIntent);
			startForeground(notificationID, builder.build());
		}
		return START_STICKY;
	}

	/*
     * Функции медиа плеера
     */

	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.d(LOG_TAG, "onCompletion: Running");
		if (isRepeat) return;
		mp.reset();
		if (isShuffle) {
			Random rn = new Random();
			int prevId = currentSongId;
			currentSongId = rn.nextInt(currentPlaylist.getSongsList().size() - 1);
			if(currentSongId >= prevId) currentSongId++;
		} else {
			if (currentSongId == currentPlaylist.getSongsList().size() - 1) currentSongId = 0;
			else currentSongId++;
		}
		setSources(currentSongId);
		mp.start();
		if (isRepeat) mp.setLooping(true);
	}

	private void setSources(int songId) { //Настройка mediaplayer и обновление уведомления
		Log.d(LOG_TAG, "setSources: Running");
		try {
			//Медиаплеер
			mp.reset();
			mp.setDataSource(currentPlaylist.getSong(songId).getPath().getAbsolutePath());
			mp.prepare();
			Log.d(LOG_TAG, "setSources: " + currentPlaylist.getSong(currentSongId).getPath().getAbsolutePath());
			//Broadcast
			Intent intent = new Intent(BROADCAST_ACTION);
			sendBroadcast(intent);
			//Уведомление
			NotificationManagerCompat nm = NotificationManagerCompat.from(this);
			builder = new NotificationCompat.Builder(this)
					.setContentTitle(currentPlaylist.getSong(songId).getName())
					.setContentText(currentPlaylist.getSong(songId).getArtist())
					.setSmallIcon(R.drawable.ic_action_play);
			Intent resultIntent = new Intent(this, MainActivity.class);
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
			stackBuilder.addParentStack(MainActivity.class);
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
			builder.setContentIntent(resultPendingIntent);
			nm.notify(notificationID, builder.build());
		} catch (Exception e) {
			Log.e(LOG_TAG, "setSources exception: ", e);
			Toast.makeText(this, "Ошибка подготовки текущей песни", Toast.LENGTH_LONG).show();
		}
	}

	/*
	 * Функции для вызова из активити
	 */

	//Управление воспроизведением
	public void pausePlayback() {
		Log.d(LOG_TAG, "pausePlayback: Running");
		if(mp.isPlaying()) mp.pause();
	}

	public void continuePlayback() {
		Log.d(LOG_TAG, "continuePlayback: Running");
		if(!mp.isPlaying()) mp.start();
	}

	public void movePlayback(int position) {
		Log.d(LOG_TAG, "movePlayback: Running");
		mp.seekTo(position);
	}

	//Управление повтором и случайным воспроизведением
	public void setShuffle(boolean shuffle) {
		Log.d(LOG_TAG, "setShuffle: Running");
		isShuffle = shuffle;
	}

	public void setRepeat(boolean repeat) {
		Log.d(LOG_TAG, "setRepeat: Running");
		isRepeat = repeat;
		mp.setLooping(isRepeat);
	}

	//Get-set функции
	public int getCurrentSongId() {
		Log.d(LOG_TAG, "getCurrentSongId: Running");
		return currentSongId;
	}

	public void setCurrentSongId(int currentSongId) {
		Log.d(LOG_TAG, "getCurrentSongId: Running");
		this.currentSongId = currentSongId;
	}


	public int getCurrentPosition() {
		//Log.d(LOG_TAG, "getCurrentPosition: Running");
		return mp.getCurrentPosition();
	}

	public void setCurrentPlaylist(Playlist playlist) {
		Log.d(LOG_TAG, "setCurrentPlaylist: Running");
		currentPlaylist = playlist;
	}

	public Playlist getCurrentPlaylist() {
		Log.d(LOG_TAG, "setCurrentPlaylist: Running");
		return currentPlaylist;
	}

	public Song getSong() {
		Log.d(LOG_TAG, "getSong: Running");
		return currentPlaylist.getSong(currentSongId);
	}

	public boolean isPlaying() {
		Log.d(LOG_TAG, "isPlaying: Running");
		return mp.isPlaying();
	}

	public boolean isStarted() {
		Log.d(LOG_TAG, "isStarted: Running");
		return isStarted;
	}

	//Управление порядком песен
	public void setSong(int id) {
		Log.d(LOG_TAG, "setSong: Running");
		if(id < 0) id = currentPlaylist.getSongsList().size() - 1;
		else if(id > currentPlaylist.getSongsList().size() - 1) id = 0;
		currentSongId = id;
		setSources(id);
	}

	public void nextSong() { //Да, я мог бы их писать в активити, но так эта функция выглядит очень простой, когда в активити она была бы большой и страшной и два раза бы обращалась к сервису, вместо одного.
		int newid = currentSongId;
		if(isShuffle) {
			Random rn = new Random();
			int oldid = newid;
			newid = rn.nextInt(currentPlaylist.getSongsList().size());
			if(newid >= oldid) newid++;
		}
		else newid++;
		setSong(newid);
	}

	public void previousSong() { //См. nextSong();
		setSong(currentSongId - 1);
	}

	//Остановка сервиса
	public void stopService() {
		Log.d(LOG_TAG, "isStarted: Running");
		isStarted = false;
	}

	/*
	 * Файловые функции
	 */

	private void loadCurrentPlaylist() {
		Log.d(LOG_TAG, "loadCurrentPlaylist: Running");
		FileInputStream inputStream;
		try {
			java.util.Scanner sc;
			inputStream = openFileInput(getResources().getString(R.string.last_txt));
			sc = new java.util.Scanner(inputStream).useDelimiter("\n");
			if (!sc.hasNext()) throw new FileNotFoundException("Empty last.txt");
			String playlistName = sc.next();
			if (!sc.hasNext()) currentSongId = 0;
			else currentSongId = sc.nextInt();
			inputStream.close();
			if (playlistName.equals(getResources().getString(R.string.last_txt)) || !(new File(MEDIA_PATH + "/Playlists/" + playlistName + ".m3u8").exists()))
				throw new FileNotFoundException("Not exists:" + MEDIA_PATH + "/Playlists/" + playlistName + ".m3u8");
			currentPlaylist = new Playlist(FileManager.loadPlaylist(playlistName, true, this));
		}
		catch (Exception e) {
			Log.d(LOG_TAG, "loadCurrentPlaylist", e);
			currentPlaylist = FileManager.loadStandardPlaylist(true);
			currentSongId = 0;
			saveCurrentPlaylist();
		}
		setSources(currentSongId);
	}

	private void saveCurrentPlaylist() {
		Log.d(LOG_TAG, "saveCurrentPlaylist: running");
		FileOutputStream outputStream;
		try {
			outputStream = openFileOutput(getResources().getString(R.string.last_txt), Context.MODE_PRIVATE);
			outputStream.write(currentPlaylist.getPlaylistName().getBytes());
			outputStream.write("\n".getBytes());
			outputStream.write(Integer.toString(currentSongId).getBytes());
			outputStream.close();
			FileManager.savePlaylist(currentPlaylist, this);
		}
		catch (Exception e) {
			Log.e(LOG_TAG, "saveCurrentPlaylist", e);
		}
	}


	/*
	 * Класс binder
	 */

	class PlaybackServiceBinder extends Binder {
		PlaybackService getService() {
			return PlaybackService.this;
		}
	}
}

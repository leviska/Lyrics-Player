package ru.myitschool.iskandarovlev.lyricsplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

/*
 * Класс песни
 * Ищет все возможные данные по заданному пути к файлу
 */
public class Song {
	/*
     * Данные
     */
	private String name;
	private String artist;
	private ArrayList<String> lyrics = new ArrayList<>();
	private long duration;
	private File path;
	private final String LOG_TAG = "SongClass";
	private int chosenLyrics;

	/*
     * Методы
     */
	public Song(String path) {
		Log.d(LOG_TAG, "constructor: Running");
		this.path = new File(path);
		Log.d(LOG_TAG, "constructor: " + path);
	}

	public void setMetadata() {
		Log.d(LOG_TAG, "setMetadata: Running");
		try {
			//Если пути нету, значит нету файла, значит нечего извлекать
			if (path == null) return;
			//MMR для получения данных из MP3 файла
			MediaMetadataRetriever mmr = new MediaMetadataRetriever();
			mmr.setDataSource(path.getPath());
			//Получаем имя, исполнителя, длительность
			name = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
			if (name == null || name.isEmpty()) name = path.getName().substring(0, path.getName().indexOf(".mp3"));

			artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
			if (artist == null || artist.isEmpty()) artist = "Неизвестный исполнитель";

			duration = Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		}
		catch (Exception e) {
			Log.d(LOG_TAG, "setMetadata", e);
		}
	}

	public Bitmap getCoverFromFile() {
		if(path == null) return null;
		Log.d(LOG_TAG, "getCoverFromFile: Running");
		//Получаем обложку
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		mmr.setDataSource(path.getPath());
		byte[] data = mmr.getEmbeddedPicture();
		Bitmap cover;
		if (data != null) cover = BitmapFactory.decodeByteArray(data, 0, data.length);
		else cover = null;
		return cover;
	}

	/*
	 * setMethods
	 */
	public void setName(String _name) {name = _name;}
	public void setArtist(String _artist) {artist = _artist;}
	public void setLyrics(String _lyrics, int index) {lyrics.add(index, _lyrics);}
	public void setPath(File _path) {path = _path;}
	public void setChosenLyrics(int _chosenLyrics) {chosenLyrics = _chosenLyrics;}
	/*
	 * getMethods
	 */
	public String getName() {return name;}
	public String getArtist() {return artist;}
	public String getLyrics(int index) {return lyrics.get(index);}
	public long getDuration() {return duration;}
	public File getPath() {return path;}
	public int getChosenLyrics() {return chosenLyrics;}
	public int getLyricsSize() {return lyrics.size();}

	public void deleteLyrics() {lyrics = new ArrayList<>();}

	@Override
	public boolean equals(Object song) {
		if(song instanceof Song)
			if(path.getAbsolutePath().equals(((Song) song).getPath().getAbsolutePath())) return true;
		return false;
	}
}

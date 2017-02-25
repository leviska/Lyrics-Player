package ru.myitschool.iskandarovlev.lyricsplayer;

import android.util.Log;

import java.util.ArrayList;

/*
 * Класс плейлиста
 * Получение всех песен на устройстве
 */
public class Playlist {
	private final String LOG_TAG = "Playlist";
	private ArrayList<Song> songsList = new ArrayList<>();
	private String playlistName;

	public Playlist(Playlist playlist) {
		Log.d(LOG_TAG, "constructor-copy: Running");
		this.playlistName = playlist.playlistName;
		this.songsList = new ArrayList<>(playlist.songsList);
	}

	public Playlist(String playlistName) {
		Log.d(LOG_TAG, "constructor: Running");
		this.playlistName = playlistName;
	}

	public ArrayList<Song> getSongsList() {
		Log.d(LOG_TAG, "getSongsList: Running");
		return new ArrayList<>(songsList);
	}

	public void setSongsList(ArrayList<Song> songsList) {
		Log.d(LOG_TAG, "setSongsList: Running");
		this.songsList = new ArrayList<>(songsList);
	}

	public Song getSong(int id) {
		Log.d(LOG_TAG, "getSong: running");
		if(id >= 0 && id < songsList.size() && !songsList.isEmpty()) return songsList.get(id);
		return null;
	}

	public String getPlaylistName() {
		Log.d(LOG_TAG, "getPlaylistName: Running");
		return playlistName;
	}

	public void setPlaylistName(String playlistName) {
		Log.d(LOG_TAG, "setPlayListName: Running");
		this.playlistName = playlistName;
	}
}
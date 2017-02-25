package ru.myitschool.iskandarovlev.lyricsplayer;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Created by Лев on 08.05.2016.
 */
public class FileManager {
	private final static String LOG_TAG = "FileManager";
	private final static String MEDIA_PATH = Environment.getExternalStorageDirectory().toString();

	public static Playlist loadPlaylist(String playlistName, boolean loadMetadata, Context context) {
		Log.d(LOG_TAG, "loadCurrentPlaylist: Running");
		FileInputStream inputStream;
		Playlist playlist;
		try {
			java.util.Scanner sc;
			if(isExternalStorageWritable()) inputStream = new FileInputStream(MEDIA_PATH + "/Playlists/" + playlistName + ".m3u8");
			else inputStream = context.openFileInput(playlistName + ".m3u8");
			sc = new java.util.Scanner(inputStream).useDelimiter("\n");
			playlist = new Playlist(playlistName);
			ArrayList<Song> tempSongs = new ArrayList<Song>();
			if(sc.hasNext() && sc.next().equals(context.getResources().getString(R.string.m3u_tag))) {
				while (sc.hasNext()) {
					String temp = sc.next();
					if(temp == null || temp.equals("") || temp.equals("\n")) continue;
					String artist = null;
					String name = null;
					if(temp.substring(0, 8).equals(context.getResources().getString(R.string.m3u_song))) {
						int i;
						for(i = 7; i < temp.length(); i++)
							if(temp.charAt(i) == '-') break;
						artist = temp.substring(8, i - 1);
						name = temp.substring(i + 2);
						temp = sc.next();
						if(temp == null || temp.equals("") || temp.equals("\n")) continue;
					}
					Song tempSong = new Song(temp);
					tempSong.setMetadata();
					if(name != null && !name.equals(context.getResources().getString(R.string.default_song_name))) tempSong.setName(name);
					if(artist != null && !artist.equals(context.getResources().getString(R.string.default_artist))) tempSong.setArtist(artist);
					tempSongs.add(tempSong);
				}
			}
			else if(sc.hasNext()){
				while (sc.hasNext()) {
					Song tempSong = new Song(sc.next().replaceAll("\n", ""));
					if(loadMetadata) tempSong.setMetadata();
					tempSongs.add(tempSong);
				}
			}
			else throw new FileNotFoundException();
			playlist.setSongsList(tempSongs);
			inputStream.close();
		}
		catch (Exception e) {
			Log.d(LOG_TAG, "loadCurrentPlaylist", e);
			playlist = new Playlist(context.getResources().getString(R.string.default_playlist));
			playlist = loadStandardPlaylist(loadMetadata);
		}
		return playlist;
	}

	public static void savePlaylist(Playlist playlist, Context context) {
		Log.d(LOG_TAG, "saveCurrentPlaylist: running");
		FileOutputStream outputStream;
		try {
			if(isExternalStorageWritable()) {
				File dir = new File(MEDIA_PATH + "/Playlists/");
				if(!dir.exists()) dir.mkdir();
				outputStream = new FileOutputStream(MEDIA_PATH + "/Playlists/" + playlist.getPlaylistName() + ".m3u8");
			}
			else outputStream = context.openFileOutput(playlist.getPlaylistName() + ".m3u8", Context.MODE_PRIVATE);
			outputStream.write(context.getResources().getString(R.string.m3u_tag).getBytes());
			outputStream.write("\n\n".getBytes());
			ArrayList<Song> tempSongs = playlist.getSongsList();
			for (int i = 0; i < tempSongs.size(); i++) {
				String temp;
				String artist;
				String name;
				Song song = tempSongs.get(i);
				artist = song.getArtist();
				name = song.getName();
				if(artist == null || artist.equals("")) artist = context.getResources().getString(R.string.default_artist);
				if(name == null || name.equals("")) name = context.getResources().getString(R.string.default_song_name);
				temp = context.getResources().getString(R.string.m3u_song) + artist.replaceAll("-", " ") + " - " + name.replaceAll("-", " ") + "\n" +
						song.getPath().getAbsolutePath() + "\n\n";
				outputStream.write(temp.getBytes());
			}
			outputStream.close();
		}
		catch (Exception e) {
			Log.e(LOG_TAG, "saveCurrentPlaylist", e);
		}
	}

	@Nullable
	public static ArrayList<String> loadLyrics(String file, Context context) {
		Log.d(LOG_TAG, "loadLyrics: Running");
		ArrayList<String> lyrics = new ArrayList<>();
		FileInputStream inputStream;
		for(int i = 0; true; i++) {
			try {
				String current_path = file;
				if(current_path.contains(".mp3")) current_path = current_path.substring(0, current_path.indexOf(".mp3"));
				if(i > 0) current_path += " (" + i + ")";
				current_path += ".lrc";
				if(isExternalStorageWritable()) inputStream = new FileInputStream(MEDIA_PATH + "/Lyrics/" + current_path);
				else inputStream = context.openFileInput(current_path);
				java.util.Scanner sc = new java.util.Scanner(inputStream).useDelimiter("\n");
				if (!sc.hasNext()) throw new Exception();
				String lyric = "";
				while (sc.hasNext()) {
					String temp = sc.next();
					if(temp == null) continue;
					if (!temp.isEmpty()
							&& temp.length() > 3 && temp.charAt(0) == '['
							&& temp.charAt(1) >= '0' && temp.charAt(1) <= '9'
							&& temp.charAt(2) >= '0' && temp.charAt(2) <= '9') {
						temp = temp.substring(0, temp.indexOf(']'));
						if (temp.startsWith("\\s")) temp = temp.substring(1);
						lyric += temp;
					}
					else if(!temp.startsWith("[")) {
						lyric += temp + "\n";
					}
					else if(temp.isEmpty()) {
						lyric += "\n";
					}
				}
				lyrics.add(lyric);
				inputStream.close();
			}
			catch (FileNotFoundException e) {
				if(i > 0) break;
				else return null;
			}
			catch (Exception e) {
				if(i == 0) {
					Log.e(LOG_TAG, "loadSongLyrics", e);
					return null;
				}
				else break;
			}
		}
		return lyrics;
	}

	public static void saveLyrics(ArrayList<String> lyrics, String name, String artist, String path, Context context) {
		Log.d(LOG_TAG, "saveLyrics: Running");
		final String LRC_ARTIST = "[ar:";
		final String LRC_NAME = "[ti:";
		final String LRC_EDITOR = "[re:";
		for(int i = 0; i < lyrics.size(); i++) {
			try {
				String current_path = path;
				if(current_path.contains(".mp3")) current_path = current_path.substring(0, current_path.indexOf(".mp3"));
				if(i > 0) current_path += " (" + i + ")";
				current_path += ".lrc";
				FileOutputStream outputStream;
				if(isExternalStorageWritable()) {
					File dir = new File(MEDIA_PATH + "/Lyrics/");
					if(!dir.exists()) dir.mkdir();
					outputStream = new FileOutputStream(MEDIA_PATH + "/Lyrics/" + current_path);
				}
				else outputStream = context.openFileOutput(current_path, Context.MODE_PRIVATE);
				String temp = LRC_ARTIST + artist + "]\n" +
						LRC_NAME + name + "]\n" +
						LRC_EDITOR + "Lyrics Player Android app]\n\n";
				outputStream.write(temp.getBytes());
				outputStream.write(lyrics.get(i).getBytes());
			} catch (Exception e) {
				Log.e(LOG_TAG, "saveLyrics", e);
			}
		}
	}

	private static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	public static Playlist loadStandardPlaylist(boolean loadMetadata) {
		Log.d(LOG_TAG, "loadStandardPlaylist: Running");
		ArrayList<Song> songs = new ArrayList<>();
		if (MEDIA_PATH != null) {
			Log.d(LOG_TAG, "loadStandardPlaylist: Running");
			File home = new File(MEDIA_PATH);
			songs = scanDirectory(home, loadMetadata, songs);
			File VK_AUDIO = new File(MEDIA_PATH + "/.vkontakte/cache/");
			if (VK_AUDIO.exists()) {
				File[] listFiles = VK_AUDIO.listFiles();
				if (listFiles != null && listFiles.length > 0) {
					for (File file : listFiles) {
						Log.d(LOG_TAG, "scanDirectory: Scanning: " + file);
						if (!file.isDirectory()) {
							if(file.getName().matches("[0-9_]")) {
								Song song = new Song(file.getPath());
								if(loadMetadata) song.setMetadata();
								songs.add(song);
							}
						}
					}
				}
			}
		}
		Playlist playlist = new Playlist("Стандартный плейлист");
		playlist.setSongsList(songs);
		return playlist;
	}

	private static ArrayList<Song> scanDirectory(File directory, boolean loadMetadata, ArrayList<Song> songs) {
		Log.d(LOG_TAG, "scanDirectory: Running");
		if (directory != null) {
			File[] listFiles = directory.listFiles();
			if (listFiles != null && listFiles.length > 0) {
				for (File file : listFiles) {
					Log.d(LOG_TAG, "scanDirectory: Scanning: " + file);
					if (file.isDirectory()) {
						songs = scanDirectory(file, loadMetadata, songs);
					} else {
						if(file.getName().equals(".nomedia")) break;
						if(file.getName().endsWith("mp3") ||
								file.getName().endsWith("m4a") ||
								file.getName().endsWith("flac") ||
								file.getName().endsWith("wav")) {
							Song song = new Song(file.getPath());
							if(loadMetadata) song.setMetadata();
							songs.add(song);
						}
					}
				}
			}
		}
		return songs;
	}
}

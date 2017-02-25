package ru.myitschool.iskandarovlev.lyricsplayer;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * Класс управления и загрузки текстов из интернета
 */
public class RunnableManager {
	private static final String LOG_TAG = "RunnableManager";
	private static LyricsRunnable runnable;
	private static boolean isEnded = false;
	private static boolean isRunning = false;
	private static boolean isUpdated = false;
	private static ExecutorService es = Executors.newFixedThreadPool(2);
	private static MainActivity context;

	//Заменяем runnable
	public static void createRunnable(String artist, String name, String path, MainActivity context) {
		Log.d(LOG_TAG, "createRunnable: Running");
		runnable = new LyricsRunnable(artist, name, path);
		RunnableManager.context = context;
		if(isRunning) isUpdated = true;
	}

	public static void run() {
		if(isRunning) return;
		Log.d(LOG_TAG, "createRunnable: Running");
		isRunning = true;
		es.execute(new Runnable() {
			@Override
			public void run() {
				runnable.execute();
				while(true) {
					while(!isEnded) {
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if(isUpdated) {
						isUpdated = false;
						isEnded = false;
						runnable.execute();
					}
					else {
						isEnded = false;
						isRunning = false;
						return;
					}
				}
			}
		});
	}

	private static class LyricsRunnable extends AsyncTask<Void, Void, ArrayList<String>> {
		private final String LOG_TAG = "LyricsThread";
		private String name;
		private String artist;
		private String path;
		private boolean isError = true;

		public LyricsRunnable(String artist, String name, String path) {
			this.artist = artist;
			this.name = name;
			this.path = path;
		}

		private String getTextFromURL(String url, String filters[]) throws Exception {
			Log.d(LOG_TAG, "getTextFromURL: Running");
			Document doc = Jsoup //Подключение к сайту
					.connect(url)
					.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					.timeout(10000)
					.get();
			Element text = null;
			if (filters.length == 0) throw new Exception("Для этого сайта нету фильтров");
			for (int i = 0; text == null && i < filters.length; text = doc.select(filters[i]).first(), i++);
			if (text == null) throw new Exception("Ошибка получения текста");
			text.select("br").append("\\n");
			return Jsoup.parse(text.text()).text().replaceAll("\\\\n", "\n").replaceAll("\n ", "\n"); //Парсинг текста
		}

		@Override
		protected ArrayList<String> doInBackground(Void... v) {
			Log.d(LOG_TAG, "doInBackground: Running");
			ArrayList<String> lyrics = new ArrayList<>();
			try {
				if(name != null && artist != null && !name.isEmpty() && !artist.isEmpty()) {
					if(!name.equals(context.getString(R.string.default_song_name)) && !artist.equals(context.getString(R.string.default_artist))) {
						name = name.replaceAll("[^A-Za-z0-9 ]", ""); //Убираем все символы, кроме алфавита и цифр
						artist = artist.replaceAll("[^A-Za-z0-9 ]", "");
						String search_url = "https://www.google.com/search?q=" + artist.replaceAll("\\s", "+") + "+%2F+" + name.replaceAll("\\s", "+"); //Создаем url для поиска
						Document doc = Jsoup //Подключаемся к поиску Google
								.connect(search_url.toLowerCase())
								.timeout(10000)
								.get();
						/*
						Element spell = doc.select("a.spell").first(); //Проверка правописания
						if(spell != null && spell.hasText()) {
							String text = spell.text();
							Log.d(LOG_TAG, "spell.text() " + text);
							name = text.substring(0, text.indexOf(" / "));
							artist = text.substring(text.indexOf(" / ") + 3, text.length());
							System.out.println(name);
							System.out.println(artist);
						}
						*/
						Elements search_results = doc.select("h3.r").select("a"); //Получение результатов поиска
						String search_urls[] = new String[search_results.size()];
						for(int i = 0; i < search_results.size(); i++) {
							String url = search_results.get(i).attr("href"); //Парсинг ссылки из мусора гугла
							url = url.substring(url.indexOf("://") + 3);
							if(url.startsWith("www")) url = url.substring(4);
							if(url.contains(".html")) url = url.substring(0, url.indexOf(".html") + 5);
							if(url.contains("&sa=U")) url = url.substring(0, url.indexOf("&sa=U"));
							if(url.contains("&ved=")) url = url.substring(0, url.indexOf("&ved="));
							Log.d(LOG_TAG, "search_url " + url);
							search_urls[i] = url;
						}
						String filters_res[] = context.getResources().getStringArray(R.array.filters);
						ArrayList<Pair<String, ArrayList<String>>> filters = new ArrayList<>();
						Pair<String, ArrayList<String>> temp_filter;
						String temp_first = "";
						ArrayList<String> temp_second = new ArrayList<>();
						for(int i = 0; i < filters_res.length; i++) {
							if(filters_res[i].startsWith("url: ")) {
								if(!temp_first.isEmpty()) {
									temp_filter = new Pair<>(temp_first, temp_second);
									filters.add(temp_filter);
									temp_second = new ArrayList<>();
									Log.d(LOG_TAG, "filter " + temp_filter.first + " : " + temp_filter.second.get(0));
								}
								temp_first = filters_res[i].substring(5);
							}
							else if(i == filters_res.length - 1) {
								temp_second.add(filters_res[i]);
								temp_filter = new Pair<>(temp_first, temp_second);
								filters.add(temp_filter);
								Log.d(LOG_TAG, "filter " + temp_filter.first + " : " + temp_filter.second);
							}
							else temp_second.add(filters_res[i]);
						}
						for(int i = 0; i < filters.size(); i++) {
							for (String search_url_item : search_urls) {
								if (search_url_item.toLowerCase().contains(filters.get(i).first.toLowerCase())) {
									lyrics.add(getTextFromURL("http://" + search_url_item, filters.get(i).second.toArray(new String[0])) + "\n\n" + context.getString(R.string.lyrics_taken) + ": " + filters.get(i).first);
									isError = false;
								}
							}
						}
						if(lyrics.size() == 0) {
							//Если не нашли ни одного фильтра для ссылок
							lyrics.add(context.getString(R.string.lyrics_not_found));
						}
					} else lyrics.add(context.getString(R.string.default_song_name));
				}
				else throw new Exception("Пустые данные");
			}
			catch (org.jsoup.HttpStatusException e) {
				/*
				if(e.getUrl().startsWith("https://ipv4")) {
					//Это означает, что гугл решил, что это бот и надо бы его проверить. Код ниже открывает ссылку гугла, но даже прохождение капчи не помогает =/
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(e.getUrl()));
					context.startActivity(browserIntent);
				}
				*/
				lyrics = new ArrayList<>();
				lyrics.add(context.getString(R.string.error_lyrics));
			}
			catch (Exception e) {
				Log.e(LOG_TAG, "LyricsRunnable", e);
				lyrics = new ArrayList<>();
				lyrics.add(context.getString(R.string.error_lyrics));
			}
			return lyrics;
		}

		@Override
		protected void onPostExecute(ArrayList<String> lyrics) {
			Log.d(LOG_TAG, "onPostExecute: Running");
			isEnded = true;
			context.foundLyrics(lyrics, name, artist, path, isError);
		}
	}
}

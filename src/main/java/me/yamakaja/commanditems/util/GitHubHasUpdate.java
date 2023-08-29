package me.yamakaja.commanditems.util;

import org.bukkit.plugin.Plugin;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.scheduler.BukkitRunnable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.yamakaja.commanditems.CommandItems;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.logging.Level;

public class GitHubHasUpdate {
    public static final String API_HOST = "https://api.github.com/repos";
    public static final String API_LATEST_RELEASE = "releases/latest";
    public static final String USER_AGENT = "Relaxing9 CommandItems Fork";
    public static final boolean DEBUG = true;

    private final String author;
    private final String repository;
    private final Plugin plugin;
	private URL url;
	private VersionComparator versionComparator;

	private boolean checking;
	private boolean error;
	private boolean hasUpdate;
	private String latestVersion;
	private final String currentVersion;
    
    public GitHubHasUpdate(Plugin plugin, String author, String repository) {
        this.plugin = plugin;
        this.author = author;
        this.repository = repository;
        this.currentVersion = plugin.getDescription().getVersion();
        this.versionComparator = (latest, current) ->
				!latest.equalsIgnoreCase(current);

		this.checking = false;
		this.error = false;
		this.hasUpdate = false;

    }

	public GitHubHasUpdate withVersionComparator(VersionComparator versionComparator) {
		this.versionComparator = versionComparator;
		return this;
	}

	public void checkUpdate() {
		checkUpdate(null);
	}


	public GitHubHasUpdate checkUpdate(UpdateCallback callback) {
		checking = true;
		final GitHubHasUpdate self = this;
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					try {
						String rawUrl = API_HOST + "/" + author + "/" + repository + "/" + API_LATEST_RELEASE;
						url = new URL(rawUrl);
					} catch(MalformedURLException e) {
                        CommandItems.logger.log(Level.SEVERE, "Invalid url: '" + url + "', are the author '" + author + "' and repository '" + repository + "' correct?");
						error = true;
						return;
					}

					try {
						URLConnection conn = url.openConnection();
						conn.setConnectTimeout(15000);
						conn.addRequestProperty("User-Agent", USER_AGENT);
						conn.addRequestProperty("Accept", "application/vnd.github.v3+json");
						conn.setDoOutput(true);
						try(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
							String response = reader.readLine();
							debug("Response:", response);

                            JsonNode latestRelease = new ObjectMapper().readTree(response);

							if(latestRelease.isEmpty()) {
								CommandItems.logger.log(Level.WARNING, "Failed to get api response from " + url);
								error = true;
								return;
							}
							debug("json: " + latestRelease.toString());

							latestVersion = latestRelease.get("tag_name").toString();
							debug("Tag name:", latestVersion);

							debug("Plugin version:", currentVersion);

							hasUpdate = versionComparator.isNewer(latestVersion, currentVersion);
						}
					} catch(IOException e) {
						CommandItems.logger.log(Level.SEVERE, "Failed to get latest release:" + ExceptionUtils.getStackTrace(e));
						error = true;
					} catch(ClassCastException e) {
						CommandItems.logger.log(Level.INFO, "Unexpected structure of the result, failed to parse it");
						error = true;
					}
				} finally {
					checking = false;
					debug("result:", self);
					if(callback != null) {
						new BukkitRunnable() {
							@Override
							public void run() {
								callback.run(self);
							}
						}.runTask(plugin);
					}
				}
			}
		}.runTaskAsynchronously(plugin);
		return this;
	}

	public boolean isChecking() {
		return checking;
	}

	public boolean hasFailed() {
		return error;
	}

	public boolean hasUpdate() {
		return hasUpdate;
	}

	public String getRepository() {
		return repository;
	}

	public String getAuthor() {
		return author;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public String getLatestVersion() {
		return latestVersion;
	}

	public interface VersionComparator {

        boolean isNewer(String latestVersion, String currentVersion);
	}

	public interface UpdateCallback {
		void run(GitHubHasUpdate result);
	}

	private void debug(Object... message) {
		if(DEBUG) {
			CommandItems.logger.log(Level.INFO, "[" + this.getClass().getSimpleName() + "] [DEBUG] " + StringUtils.join(message, " "));
		}
	}

	@Override
	public String toString() {
		return "GitHubNotifyUpdates(" + StringUtils.join(Arrays.asList(
				"author=" + author,
				"repository=" + repository,
				"plugin=" + plugin.getName(),
				"checking=" + checking,
				"hasUpdate=" + hasUpdate,
				"error=" + error,
				"currentVersion=" + currentVersion,
				"latestVersion=" + latestVersion
		), ", ") + ")";
	}
}
package io.github.palexdev.feedfx.utils.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class GitHubUpdateChecker implements UpdateChecker {
    private final String currentVersion;
    private final String url;

    public GitHubUpdateChecker(String currentVersion, String owner, String repo) {
        this.currentVersion = currentVersion;
        this.url = "https://api.github.com/repos/%s/%s/releases/latest"
            .formatted(owner, repo);
    }

    @Override
    public String getCurrentVersion() {
        return currentVersion;
    }

    @Override
    public String getLatestVersion() throws UpdateCheckException {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            connection.setRequestMethod("GET");

            String response = read(connection);
            if (response.isBlank()) return null;

            Pattern tagPattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^,]*)\",");
            Matcher tagMatcher = tagPattern.matcher(response);
            if (tagMatcher.find()) return  tagMatcher.group(1);
        } catch (Exception ex) {
            throw new  UpdateCheckException(ex);
        }
        return null;
    }

    @Override
    public int compare(String current, String latest) {
        if (latest == null) return -2;
        long currL = Long.parseLong(current.replace(".", ""));
        long latestL = Long.parseLong(latest.substring(1).replace(".", ""));
        return Long.compare(latestL, currL);
    }

    protected String read(HttpsURLConnection connection) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream())
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}

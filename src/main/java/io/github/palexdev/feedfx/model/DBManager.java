package io.github.palexdev.feedfx.model;

import io.github.palexdev.feedfx.FeedFX;
import io.github.palexdev.feedfx.events.AppEvent;
import io.github.palexdev.mfxcore.events.bus.IEventBus;
import io.inverno.core.annotation.Bean;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.tinylog.Logger;

@Bean
public class DBManager {
    //================================================================================
    // Properties
    //================================================================================
    private final IEventBus bus;
    private final Path dbPath = FeedFX.appBaseDir().resolve("feeds.db");

    //================================================================================
    // Constructors
    //================================================================================
    public DBManager(IEventBus bus) {
        this.bus = bus;
    }

    //================================================================================
    // Methods
    //================================================================================
    private PreparedStatement getFeedsQuery(
        Connection connection,
        FeedsSource source, Tag tag, boolean showRead, long recentReadTime
    ) throws SQLException {
        PreparedStatement stmt;
        if (tag != null) {
            stmt = connection.prepareStatement("""
                SELECT f.* FROM feeds f WHERE (? = -1 OR f.source_id = ?)
                AND (f.id IN (SELECT feed_id FROM feed_tags WHERE tag_id = ?))
                """);
            stmt.setInt(1, source.id());
            stmt.setInt(2, source.id());
            stmt.setInt(3, tag.id());
        } else {
            stmt = connection.prepareStatement("""
                SELECT f.* FROM feeds f WHERE (? = -1 OR f.source_id = ?)
                AND (f.isRead = ? OR f.readDate >= ?)
                """);
            stmt.setInt(1, source.id());
            stmt.setInt(2, source.id());
            stmt.setBoolean(3, showRead);
            stmt.setLong(4, recentReadTime);
        }
        return stmt;
    }

    public Collection<Feed> getFeeds(FeedsSource source, Tag tag, boolean showRead, long recentReadTime) {
        try (
            Connection connection = connect();
            PreparedStatement stmt = getFeedsQuery(connection, source, tag, showRead, recentReadTime);
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                List<Feed> feeds = new ArrayList<>();
                while (rs.next()) {
                    feeds.add(new Feed(
                        source.id(),
                        rs.getString("title"),
                        rs.getString("link"),
                        rs.getString("image"),
                        rs.getLong("date"),
                        rs.getBoolean("isRead"),
                        rs.getLong("readDate")
                    ));
                }
                return feeds;
            }
        } catch (SQLException ex) {
            Logger.error("An error occurred while fetching feeds from the database:\n{}", ex);
            return Collections.emptyList();
        }
    }

    public int addFeeds(Feed... feeds) {
        try (
            Connection connection = connect();
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO feeds (title, link, image, date, isRead, readDate, source_id) VALUES (?, ?, ?, ?, ?, ?, ?)"
            )
        ) {
            for (Feed feed : feeds) {
                stmt.setString(1, feed.title());
                stmt.setString(2, feed.link());
                stmt.setString(3, feed.img());
                stmt.setLong(4, feed.date());
                stmt.setBoolean(5, feed.read());
                stmt.setLong(6, feed.readDate());
                stmt.setInt(7, feed.sourceId());

                stmt.addBatch();
            }

            return Arrays.stream(stmt.executeBatch())
                .filter(r -> r > 0)
                .sum();
        } catch (SQLException ex) {
            Logger.error("An error occurred while adding the feeds:\n{}", ex);
            return 0;
        }
    }

    public int getFeedId(Feed feed) {
        try (
            Connection connection = connect();
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT id FROM feeds WHERE link = ?"
            )
        ) {
            stmt.setString(1, feed.link());
            try (ResultSet res = stmt.executeQuery()) {
                if (res.next()) {
                    return res.getInt("id");
                }
            }
        } catch (SQLException ex) {
            Logger.error("Failed to retrieve id for feed {} because:\n{}", feed.title(), ex);
        }
        return -1;
    }

    public void tagFeed(Feed feed, Tag... tags) {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);

            // Since we don't have the id associated to the feed
            // We must get it from its link, which is unique
            int feedId = getFeedId(feed);
            if (feedId == -1) {
                Logger.error("Cannot tag feed because id is unknown");
                return;
            }

            // Delete all tags first
            try (PreparedStatement dstmt = connection.prepareStatement(
                "DELETE FROM feed_tags WHERE feed_id = ?"
            )) {
                dstmt.setInt(1, feedId);
                dstmt.executeUpdate();
            }

            // Insert new tags if any
            if (tags != null && tags.length > 0) {
                try (PreparedStatement istmt = connection.prepareStatement(
                    "INSERT OR IGNORE INTO feed_tags (feed_id, tag_id) VALUES (?, ?)"
                )) {
                    for (Tag tag : tags) {
                        istmt.setInt(1, feedId);
                        istmt.setInt(2, tag.id());
                        istmt.addBatch();
                    }
                    istmt.executeBatch();
                }
            }

            connection.commit();
        } catch (SQLException ex) {
            Logger.error("Error updating tags for feed {} because:\n{}", feed.title(), ex);
        }
    }

    public List<Tag> getTagsForFeed(Feed feed) {
        try (Connection connection = connect()) {
            // Since we don't have the id associated to the feed
            // We must get it from its link, which is unique
            int feedId = getFeedId(feed);
            if (feedId == -1) {
                Logger.error("Cannot fetch tags for feed because id is unknown");
                return Collections.emptyList();
            }

            try (PreparedStatement stmt = connection.prepareStatement("""
                SELECT t.id, t.name, t.color FROM tags t
                INNER JOIN feed_tags ft ON t.id = ft.tag_id
                WHERE ft.feed_id = ?
                """
            )) {
                stmt.setInt(1, feedId);
                try (ResultSet res = stmt.executeQuery()) {
                    List<Tag> tags = new ArrayList<>();
                    while (res.next()) {
                        tags.add(new Tag(
                            res.getInt("id"),
                            res.getString("name"),
                            res.getString("color")
                        ));
                    }
                    return tags;
                }
            }
        } catch (SQLException ex) {
            Logger.error("Error fetching tags for feed {} because:\n{}", ex);
        }
        return Collections.emptyList();
    }

    public boolean markFeedAs(Feed feed, boolean read, long readDate) {
        try (
            Connection connection = connect();
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE feeds SET isRead = ?, readDate = ? WHERE link = ?"
            )
        ) {
            stmt.setBoolean(1, read);
            stmt.setLong(2, readDate);
            stmt.setString(3, feed.link());
            stmt.executeUpdate();
            return read;
        } catch (SQLException ex) {
            Logger.error("Failed to set feed {} as {} because:\n{}", feed.title(), read, ex);
            return feed.read();
        }
    }

    public Collection<FeedsSource> getSources() {
        try (
            Connection connection = connect();
            Statement stmt = connection.createStatement();
            ResultSet res = stmt.executeQuery(
                "SELECT * FROM sources"
            )
        ) {
            List<FeedsSource> sources = new ArrayList<>();
            while (res.next()) {
                sources.add(new FeedsSource(
                    res.getInt("id"),
                    res.getString("name"),
                    res.getString("link")
                ));
            }
            return sources;
        } catch (SQLException ex) {
            Logger.error("Failed to get sources from db because:\n{}", ex);
        }
        return Collections.emptyList();
    }

    public Optional<FeedsSource> addSource(String name, String link) {
        try (Connection connection = connect()) {
            // Check if source already exists
            // May cause bugs if not done!
            try (PreparedStatement cstmt = connection.prepareStatement(
                "SELECT id, name, link FROM sources WHERE name = ? AND link = ?"
            )) {
                cstmt.setString(1, name);
                cstmt.setString(2, link);
                try (ResultSet res = cstmt.executeQuery()) {
                    if (res.next()) {
                        return Optional.of(new FeedsSource(
                            res.getInt("id"),
                            res.getString("name"),
                            res.getString("link")
                        ));
                    }
                }
            }

            // Not found, insert and return new source
            try (PreparedStatement istmt = connection.prepareStatement(
                "INSERT INTO sources (name, link) VALUES (?, ?)"
            )) {
                istmt.setString(1, name);
                istmt.setString(2, link);

                int rowsAffected = istmt.executeUpdate();
                if (rowsAffected == 0) {
                    Logger.warn("RSS source {}: {} not added", name, link);
                    return Optional.empty();
                }

                try (ResultSet res = istmt.getGeneratedKeys()) {
                    if (res.next()) {
                        int id = res.getInt(1);
                        return Optional.of(new FeedsSource(id, name, link));
                    }
                }

                Logger.error("Something went wrong while adding RSS source {}", name);
                return Optional.empty();
            }
        } catch (SQLException ex) {
            Logger.error("Failed to add RSS source because:\n{}", ex);
            return Optional.empty();
        }
    }

    public boolean deleteSource(FeedsSource source) {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);

            try (PreparedStatement dfStmt = connection.prepareStatement(
                "DELETE FROM feeds WHERE source_id = ?"
            )) {
                dfStmt.setInt(1, source.id());
                dfStmt.executeUpdate();
            }

            try (PreparedStatement dsStmt = connection.prepareStatement(
                "DELETE FROM sources WHERE id = ?"
            )) {
                dsStmt.setInt(1, source.id());
                dsStmt.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            Logger.error("Failed to remove RSS source because:\n{}", ex);
            return false;
        }
    }

    public Collection<Tag> getTags() {
        try (
            Connection connection = connect();
            Statement stmt = connection.createStatement();
            ResultSet res = stmt.executeQuery(
                "SELECT * FROM tags"
            )
        ) {
            List<Tag> tags = new ArrayList<>();
            while (res.next()) {
                tags.add(new Tag(
                    res.getInt("id"),
                    res.getString("name"),
                    res.getString("color")
                ));
            }
            return tags;
        } catch (SQLException ex) {
            Logger.error("Failed to get tags from db because:\n{}", ex);
        }
        return Collections.emptyList();
    }

    public Optional<Tag> addTag(String name, String color) {
        try (
            Connection connection = connect();
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO tags (name, color) VALUES (?, ?)"
            )
        ) {
            stmt.setString(1, name);
            stmt.setString(2, color);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                Logger.warn("Tag {} not added", name);
                return Optional.empty();
            }

            try (ResultSet res = stmt.getGeneratedKeys()) {
                if (res.next()) {
                    int id = res.getInt(1);
                    return Optional.of(new Tag(id, name, color));
                }
            }

            Logger.error("Something went wrong while adding tag {}", name);
            return Optional.empty();
        } catch (SQLException ex) {
            Logger.error("Failed to add tag because:\n{}", ex);
            return Optional.empty();
        }
    }

    public boolean editTag(int id, String name, String color) {
        try (
            Connection connection = connect();
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE tags SET name = ?, color = ? WHERE id = ?"
            )
        ) {
            stmt.setString(1, name);
            stmt.setString(2, color);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            Logger.error("Failed to edit tag because:\n{}", ex);
            return false;
        }
    }

    public boolean deleteTag(Tag tag) {
        try (
            Connection connection = connect();
            PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM tags WHERE id = ?"
            )
        ) {
            stmt.setInt(1, tag.id());
            stmt.executeUpdate();
            return true;
        } catch (SQLException ex) {
            Logger.error("Failed to remove RSS source because:\n{}", ex);
            return false;
        }
    }

    private void initialize() throws IOException {
        Files.createFile(dbPath);

        try (Connection connection = connect();
             Statement stmt = connection.createStatement()
        ) {
            stmt.addBatch("""
                CREATE TABLE sources (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE,
                    link TEXT UNIQUE
                );
                """);

            stmt.addBatch("""
                CREATE TABLE feeds (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    link TEXT NOT NULL UNIQUE,
                    image TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    isRead BOOLEAN DEFAULT FALSE,
                    readDate INTEGER DEFAULT -1 NOT NULL,
                    source_id INTEGER,
                    FOREIGN KEY (source_id) REFERENCES sources(id)
                );
                """);

            stmt.addBatch("""
                CREATE TABLE tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    color TEXT
                );
                """);

            stmt.addBatch("""
                CREATE TABLE feed_tags (
                    feed_id INTEGER,
                    tag_id INTEGER,
                    FOREIGN KEY (feed_id) REFERENCES feeds(id) ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
                    PRIMARY KEY (feed_id, tag_id)
                );
                """);

            stmt.addBatch("INSERT INTO tags (name, color) VALUES ('High Priority', '#EE4444')");
            stmt.addBatch("INSERT INTO tags (name, color) VALUES ('Medium Priority', '#F57512')");
            stmt.addBatch("INSERT INTO tags (name, color) VALUES ('Low Priority', '#EAB40A')");

            stmt.executeBatch();
        } catch (NullPointerException | SQLException ex) {
            Logger.error("Failed to initialize database because:\n{}", ex);
            bus.publish(new AppEvent.AppCloseEvent());
            throw new RuntimeException(ex);
        }
    }

    private Connection connect() throws SQLException {
        if (!Files.exists(dbPath)) {
            try {
                initialize();
            } catch (IOException ex) {
                Logger.error("Could not create db file because:\n{}", ex);
                throw new RuntimeException(ex);
            }
        }
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }
}

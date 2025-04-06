package io.github.palexdev.feedfx.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public class Feed {
    //================================================================================
    // Static Properties
    //================================================================================
    public static final Comparator<Feed> DEFAULT_COMPARATOR =
        Comparator.comparingLong(Feed::date).reversed()
            .thenComparing(Feed::title);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM");
    private static final DateTimeFormatter FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy");


    //================================================================================
    // Properties
    //================================================================================
    private final int sourceId;
    private final String title;
    private final String link;
    private final String img;
    private final long date;
    private boolean read;
    private long readDate;

    //================================================================================
    // Constructors
    //================================================================================
    public Feed(
        int sourceId,
        String title,
        String link,
        String img,
        long date,
        boolean read,
        long readDate
    ) {
        this.sourceId = sourceId;
        this.title = title;
        this.link = link;
        this.img = img;
        this.date = date;
        this.read = read;
        this.readDate = readDate;
    }

    public Feed(int sourceId, String name, String link, String img, long date) {
        this(sourceId, name, link, img, date, false, -1L);
    }

    //================================================================================
    // Methods
    //================================================================================
    public String dateToString() {
        Instant instant = Instant.ofEpochMilli(date);
        ZonedDateTime atZone = instant.atZone(ZoneId.systemDefault());
        LocalDate feedDate = atZone.toLocalDate();

        LocalDate today = LocalDate.now();
        if (feedDate.isEqual(today)) { // Today
            return atZone.format(TIME_FORMATTER);
        } else if (feedDate.isEqual(today.minusDays(1))) {
            return "Yesterday " + atZone.format(TIME_FORMATTER);
        } else {
            boolean sameYear = feedDate.getYear() == today.getYear();
            return atZone.format(sameYear ? DATE_FORMATTER : FULL_DATE_FORMATTER);
        }
    }

    public String getImageExtension() {
        return Optional.ofNullable(img)
            .map(s -> {
                int dot = s.lastIndexOf('.');
                return dot != -1 ? s.substring(dot) : "Unknown";
            })
            .orElse("No Image Available");
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Feed) obj;
        return this.sourceId == that.sourceId &&
               Objects.equals(this.title, that.title) &&
               Objects.equals(this.link, that.link) &&
               Objects.equals(this.img, that.img) &&
               this.date == that.date;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId, title, link, img, date);
    }

    @Override
    public String toString() {
        return "Feed[" +
               "sourceId=" + sourceId + ", " +
               "title=" + title + ", " +
               "link=" + link + ", " +
               "img=" + img + ", " +
               "date=" + date + ", " +
               "read=" + read + ']';
    }

    //================================================================================
    // Getters/Setters
    //================================================================================
    public int sourceId() {
        return sourceId;
    }

    public String title() {
        return title;
    }

    public String link() {
        return link;
    }

    public String img() {
        return img;
    }

    public long date() {
        return date;
    }

    public boolean read() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public long readDate() {
        return readDate;
    }

    public void setReadDate(long readDate) {
        this.readDate = readDate;
    }
}
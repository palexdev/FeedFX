package io.github.palexdev.feedfx.model;

import com.rometools.modules.mediarss.MediaEntryModule;
import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import io.inverno.core.annotation.Bean;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.tinylog.Logger;

@Bean
public class FeedHandler {

    //================================================================================
    // Methods
    //================================================================================
    public List<Feed> fetch(FeedsSource source) {
        try {
            SyndFeed input = new SyndFeedInput().build(reader(source.link()));
            return input.getEntries().stream()
                .map(e -> processFeedEntry(source, e))
                .toList();
        } catch (Exception ex) {
            Logger.error("Failed to fetch feeds from source {} because:\n{}", source.name(), ex);
            return Collections.emptyList();
        }
    }

    private Feed processFeedEntry(FeedsSource source, SyndEntry entry) {
        String title = Optional.ofNullable(entry.getTitle()).orElse("Untitled");
        String link = Optional.ofNullable(entry.getLink()).orElse("");
        long date = Optional.ofNullable(entry.getPublishedDate())
            .map(d -> d.toInstant().toEpochMilli())
            .orElse(Instant.EPOCH.toEpochMilli());
        String image = extractImage(entry);
        return new Feed(source.id(), title, link, image, date);
    }

    private String extractImage(SyndEntry entry) {
        // 1. Check <enclosure> for an image
        if (entry.getEnclosures() != null) {
            for (SyndEnclosure enclosure : entry.getEnclosures()) {
                if (enclosure.getType().startsWith("image/")) {
                    return enclosure.getUrl();
                }
            }
        }

        // 2. Check <media:content> for an image
        MediaEntryModule mediaModule = (MediaEntryModule) entry.getModule(MediaEntryModule.URI);
        if (mediaModule != null) {
            for (MediaContent mediaContent : mediaModule.getMediaContents()) {
                if ((mediaContent.getType() != null && mediaContent.getType().startsWith("image/")) ||
                    (mediaContent.getMedium() != null && mediaContent.getMedium().equals("image"))) {
                    return mediaContent.getReference().toString();
                }
            }
        }

        // 3. Check for an <img> tag inside the <description>
        if (entry.getDescription() != null) {
            String description = entry.getDescription().getValue();
            String imgTag = extractFirstImageFromHtml(description);
            if (imgTag != null) {
                return imgTag;
            }
        }

        return "";
    }

    private String extractFirstImageFromHtml(String html) {
        if (html == null) return null;
        Pattern pattern = Pattern.compile("<img[^>]+src=['\"](.*?)['\"]");
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1) : null;
    }

    private XmlReader reader(String url) throws IOException {
        return new XmlReader(URI.create(url).toURL());
    }
}

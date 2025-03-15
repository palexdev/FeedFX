package unit;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.net.URI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FeedTests {
    static String TEST_FEED;

    @BeforeAll
    static void init() {
        TEST_FEED = System.getenv("TEST_FEED");
        if (TEST_FEED == null)
            throw new IllegalStateException("Cannot run tests because TEST_FEED variable is not set on the environment!");
    }

    @Test
    void testFeed() throws Exception {
        SyndFeed feed = new SyndFeedInput()
            .build(new XmlReader(
                URI.create(TEST_FEED).toURL()
            ));
        System.out.println(feed.getTitle());
        feed.getEntries().forEach(System.out::println);
    }
}

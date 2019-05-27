import io.manebot.database.search.Search;
import io.manebot.plugin.ts3.platform.chat.TeamspeakChatMessage;
import junit.framework.TestCase;

public class BBCodeStripTest extends TestCase {

    public static void main(String[] args) throws Exception {
        new BBCodeStripTest().testParser();
    }

    public void testParser() throws Exception {
        assertEquals(
                TeamspeakChatMessage.stripBBCode("[URL]https://youtu.be/bEfpZYYX9p8[/URL]"),
                "https://youtu.be/bEfpZYYX9p8"
        );
    }

}

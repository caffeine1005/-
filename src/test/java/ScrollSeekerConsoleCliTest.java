import org.example.digital_scroll_management.DigitalScroll;
import org.example.digital_scroll_management.DigitalScrollRepository;
import org.example.digital_scroll_management.DigitalScrollService;
import org.example.scroll_seeker.ScrollSeekerConsole;
import org.example.scroll_seeker.ScrollSeekerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ScrollSeekerConsoleCliTest {

    @TempDir
    Path tempDir;

    @Test
    void allowDownloadFlowCoversPreviewAndFilters() throws Exception {
        Path storage = tempDir.resolve("scrolls.db");
        Path uploads = tempDir.resolve("uploads");
        DigitalScrollRepository repository = new DigitalScrollRepository(storage);
        DigitalScrollService scrollService = new DigitalScrollService(repository, uploads);
        ScrollSeekerService seekerService = new ScrollSeekerService(scrollService);

        Path source = tempDir.resolve("spell.bin");
        Files.write(source, new byte[]{1, 2, 3});
        DigitalScroll scroll = scrollService.addScroll("mage", "Fireball", source.toString());
        String scrollId = scroll.getScrollId();
        String dateStr = scroll.getUploadTimestamp().toLocalDate().toString();

        Path downloadTarget = tempDir.resolve("download.bin");

        String input = String.join("\n",
                "1",
                "2",
                scrollId,
                downloadTarget.toString(),
                "3",
                "mage",
                scrollId,
                "Fire",
                dateStr,
                "4",
                scrollId,
                "5"
        ) + "\n";

        Scanner scanner = new Scanner(new StringReader(input));
        ScrollSeekerConsole console = new ScrollSeekerConsole(scanner, seekerService);
        console.viewAndDownloadMenu(true);

        assertTrue(Files.exists(downloadTarget));
        assertEquals(1, scrollService.getScroll(scrollId).getDownloadCount());
    }

    @Test
    void guestsCannotDownload() throws Exception {
        Path storage = tempDir.resolve("scrolls.db");
        Path uploads = tempDir.resolve("uploads");
        DigitalScrollRepository repository = new DigitalScrollRepository(storage);
        DigitalScrollService scrollService = new DigitalScrollService(repository, uploads);
        ScrollSeekerService seekerService = new ScrollSeekerService(scrollService);

        String input = String.join("\n",
                "1",
                "2",
                "5"
        ) + "\n";

        Scanner scanner = new Scanner(new StringReader(input));
        ScrollSeekerConsole console = new ScrollSeekerConsole(scanner, seekerService);
        console.viewAndDownloadMenu(false);

        assertEquals(0, seekerService.filterScrolls("", "", "", null).size());
    }
}
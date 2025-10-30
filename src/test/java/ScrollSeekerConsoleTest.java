import org.example.digital_scroll_management.DigitalScroll;
import org.example.digital_scroll_management.DigitalScrollRepository;
import org.example.digital_scroll_management.DigitalScrollService;
import org.example.scroll_seeker.ScrollSeekerConsole;
import org.example.scroll_seeker.ScrollSeekerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ScrollSeekerConsoleTest {

    @TempDir
    Path tempDir;

    private ScrollSeekerService createService() throws Exception {
        Path storage = tempDir.resolve("scrolls.db");
        Path uploads = tempDir.resolve("uploads");
        DigitalScrollRepository repository = new DigitalScrollRepository(storage);
        DigitalScrollService scrollService = new DigitalScrollService(repository, uploads);
        return new ScrollSeekerService(scrollService);
    }

    private record ScrollContext(ScrollSeekerService service, DigitalScroll scroll) {
    }

    private ScrollContext createServiceWithScroll() throws Exception {
        Path storage = tempDir.resolve("ctx-scrolls.db");
        Path uploads = tempDir.resolve("ctx-uploads");
        DigitalScrollRepository repository = new DigitalScrollRepository(storage);
        DigitalScrollService scrollService = new DigitalScrollService(repository, uploads);
        Path source = tempDir.resolve("source.bin");
        Files.write(source, new byte[]{1, 2, 3});
        DigitalScroll scroll = scrollService.addScroll("mage", "ContextScroll", source.toString());
        ScrollSeekerService service = new ScrollSeekerService(scrollService);
        return new ScrollContext(service, scroll);
    }

    @Test
    void previewScrollHandlesBlankAndMissing() throws Exception {
        ScrollSeekerConsole console = new ScrollSeekerConsole(new Scanner(new StringReader("")), createService());
        var preview = ScrollSeekerConsole.class.getDeclaredMethod("previewScroll", String.class);
        preview.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            preview.invoke(console, " ");
            preview.invoke(console, "unknown");
        } finally {
            System.setOut(original);
        }

        String output = out.toString();
        assertTrue(output.contains("Scroll ID is required."));
        assertTrue(output.contains("Scroll not found."));
    }

    @Test
    void chooseSaveLocationHandlesCancelAndSuccess() throws Exception {
        ScrollSeekerService service = createService();
        DigitalScroll scroll = new DigitalScroll("SC2001", "Fire", "mage", "file", LocalDateTime.now(), 0, 0);
        var choose = ScrollSeekerConsole.class.getDeclaredMethod("chooseSaveLocation", DigitalScroll.class);
        choose.setAccessible(true);

        // Cancellation branch
        ScrollSeekerConsole cancelConsole = new ScrollSeekerConsole(new Scanner(new StringReader("\n")), service);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        Object cancelled;
        try {
            cancelled = choose.invoke(cancelConsole, scroll);
        } finally {
            System.setOut(original);
        }
        assertNull(cancelled);
        assertTrue(out.toString().contains("Download cancelled."));

        // Success branch
        String desiredPath = tempDir.resolve("download.bin").toString();
        ScrollSeekerConsole successConsole = new ScrollSeekerConsole(
                new Scanner(new StringReader(desiredPath + System.lineSeparator())),
                service);
        String selected = (String) choose.invoke(successConsole, scroll);
        assertEquals(desiredPath, selected);
    }

    @Test
    void downloadScrollValidatesInputAndCancelsWithoutPath() throws Exception {
        ScrollContext context = createServiceWithScroll();
        String scrollId = context.scroll().getScrollId();
        ScrollSeekerConsole console = new ScrollSeekerConsole(
                new Scanner(new StringReader(System.lineSeparator())),
                context.service());
        var download = ScrollSeekerConsole.class.getDeclaredMethod("downloadScroll", String.class);
        download.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            download.invoke(console, new Object[]{null});
            download.invoke(console, "unknown");
            download.invoke(console, scrollId);
        } finally {
            System.setOut(original);
        }

        String output = out.toString();
        assertTrue(output.contains("Scroll ID is required."));
        assertTrue(output.contains("Scroll not found."));
        assertTrue(output.contains("Download cancelled."));
    }

    @Test
    void viewAndDownloadMenuForGuestsBlocksDownloads() throws Exception {
        String input = String.join(System.lineSeparator(),
                "2",
                "3",
                "",
                "",
                "",
                "not-a-date",
                "5") + System.lineSeparator();
        ScrollSeekerConsole console = new ScrollSeekerConsole(new Scanner(new StringReader(input)), createService());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            console.viewAndDownloadMenu(false);
        } finally {
            System.setOut(original);
        }

        String output = out.toString();
        assertTrue(output.contains("Guests cannot download scrolls."));
        assertTrue(output.contains("Invalid date, clearing filter."));
    }
}

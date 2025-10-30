package org.example.scroll_seeker;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.example.digital_scroll_management.DigitalScroll;

public class ScrollSeekerConsole {
    private final Scanner scanner;
    private final ScrollSeekerService service;

    public ScrollSeekerConsole(Scanner scanner, ScrollSeekerService service) {
        this.scanner = scanner;
        this.service = service;
    }

    public void viewAndDownloadMenu(boolean allowDownload) {
        String uploaderFilter = "";
        String scrollIdFilter = "";
        String nameFilter = "";
        LocalDate dateFilter = null;
        boolean stay = true;
        while (stay) {
            System.out.println("--------------------------------------");
            System.out.println("Scroll seeker");
            System.out.println("1. View scrolls");
            if (allowDownload) {
                System.out.println("2. Download scroll");
            } else {
                System.out.println("2. Download scroll (not available for guests)");
            }
            System.out.println("3. Search filters");
            System.out.println("4. Preview scroll");
            System.out.println("5. Return");
            String choice = prompt("Select an option: ");
            switch (choice) {
                case "1" -> listScrolls(uploaderFilter, scrollIdFilter, nameFilter, dateFilter);
                case "2" -> {
                    if (!allowDownload) {
                        System.out.println("Guests cannot download scrolls.");
                        break;
                    }
                    String id = prompt("Scroll ID to download: ");
                    downloadScroll(id);
                }
                case "3" -> {
                    uploaderFilter = prompt("Filter uploader (leave blank for ANY): ");
                    scrollIdFilter = prompt("Filter scroll ID (leave blank for ANY): ");
                    nameFilter = prompt("Filter name (leave blank for ANY): ");
                    String dateInput = prompt("Filter upload date (yyyy-mm-dd, blank for ANY): ");
                    if (dateInput.isEmpty()) {
                        dateFilter = null;
                    } else {
                        try {
                            dateFilter = LocalDate.parse(dateInput);
                        } catch (Exception ex) {
                            System.out.println("Invalid date, clearing filter.");
                            dateFilter = null;
                        }
                    }
                    listScrolls(uploaderFilter, scrollIdFilter, nameFilter, dateFilter);
                }
                case "4" -> {
                    String id = prompt("Scroll ID to preview: ");
                    previewScroll(id);
                }
                case "5" -> stay = false;
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private void listScrolls(String uploader,
                             String scrollId,
                             String name,
                             LocalDate date) {
        List<DigitalScroll> rows = service.filterScrolls(uploader, scrollId, name, date);
        if (rows.isEmpty()) {
            System.out.println("(no scrolls match filters)");
            return;
        }
        for (DigitalScroll scroll : rows) {
            System.out.println(formatScroll(scroll));
        }
    }

    private void previewScroll(String scrollId) {
        if (scrollId == null || scrollId.trim().isEmpty()) {
            System.out.println("Scroll ID is required.");
            return;
        }
        DigitalScroll scroll = service.findScroll(scrollId.trim());
        if (scroll == null) {
            System.out.println("Scroll not found.");
            return;
        }
        ScrollPreview preview = service.buildPreview(scroll);
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println(preview.getHexSample());
            return;
        }
        final CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Preview " + scroll.getScrollId());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JTextArea area = new JTextArea(preview.getHexSample());
            area.setEditable(false);
            frame.getContentPane().add(new JScrollPane(area), BorderLayout.CENTER);
            JButton close = new JButton("Close");
            close.addActionListener(e -> {
                frame.dispose();
                latch.countDown();
            });
            frame.getContentPane().add(close, BorderLayout.SOUTH);
            frame.setSize(600, 400);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void downloadScroll(String scrollId) {
        if (scrollId == null || scrollId.trim().isEmpty()) {
            System.out.println("Scroll ID is required.");
            return;
        }
        DigitalScroll scroll = service.findScroll(scrollId.trim());
        if (scroll == null) {
            System.out.println("Scroll not found.");
            return;
        }
        String path = chooseSaveLocation(scroll);
        if (path == null) {
            return;
        }
        try {
            service.downloadScroll(scroll, Path.of(path));
            System.out.println("Download completed: " + path);
        } catch (IllegalStateException ex) {
            System.out.println("Download failed: " + ex.getMessage());
        }
    }

    private String chooseSaveLocation(DigitalScroll scroll) {
        if (GraphicsEnvironment.isHeadless()) {
            String path = prompt("Save as (full path, leave blank to cancel): ");
            if (path.isEmpty()) {
                System.out.println("Download cancelled.");
                return null;
            }
            return path;
        }
        final String[] target = new String[1];
        final CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File(scroll.getScrollId() + "_" + scroll.getName()));
            int result = chooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                target[0] = chooser.getSelectedFile().getAbsolutePath();
            } else {
                target[0] = null;
            }
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        if (target[0] == null || target[0].isEmpty()) {
            System.out.println("Download cancelled.");
            return null;
        }
        return target[0];
    }

    private String formatScroll(DigitalScroll scroll) {
        return String.format("ID=%s | Name=%s | Owner=%s | Uploaded=%s",
                scroll.getScrollId(),
                scroll.getName(),
                scroll.getOwnerUsername(),
                scroll.getUploadTimestamp());
    }

    private String prompt(String message) {
        System.out.print(message);
        String line = scanner.nextLine();
        if (line == null) {
            return "";
        }
        return line.trim();
    }
}

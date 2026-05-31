package com.routebuilder.ui.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * Full terminal emulator pane using RichTextFX InlineCssTextArea.
 *
 * Supports:
 *  - Non-editable text (read-only) but selectable/copiable.
 *  - Full SGR ANSI (ESC[...m): reset, bold, dim, italic, underline, colors 30–37, 39,
 *    bright/intense 90–97, background 40–47, 49, bright background 100–107,
 *    and 256-color / truecolor.
 *  - Carriage-return (\r) rewriting the current visual line.
 *  - ESC[K erase sequence.
 *  - Auto-scroll to bottom.
 */
public class ConsolePane extends VBox {

    private static final String FONT_FAMILY = "'JetBrains Mono', 'Consolas', 'DejaVu Sans Mono', monospace";
    private static final String FONT_SIZE   = "12.5px";
    private static final String DEFAULT_FG  = "-sui-text-main";

    private final InlineCssTextArea textArea;
    private int currentLineStart = 0;

    public ConsolePane() {
        getStyleClass().add("console-pane");

        Button btnCopy = UIFactory.createIconButton("fas-copy", "Copy All Logs");
        btnCopy.setOnAction(e -> copyAllToClipboard());

        Button btnClear = UIFactory.createIconButton("fas-trash-alt", "Clear Console");
        btnClear.setOnAction(e -> clear());

        HBox header = UIFactory.createPanelHeader("TERMINAL / CONSOLE", btnCopy, btnClear);

        textArea = new InlineCssTextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.getStyleClass().add("console-text-area");

        VirtualizedScrollPane<InlineCssTextArea> vsPane = new VirtualizedScrollPane<>(textArea);
        vsPane.getStyleClass().add("console-scroll-pane");

        // Right-click context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem("Copy Selection");
        FontIcon copyMenuIcon = new FontIcon("fas-copy");
        copyItem.setGraphic(copyMenuIcon);
        copyItem.setOnAction(e -> textArea.copy());
        
        MenuItem copyAllItem = new MenuItem("Copy All Logs");
        FontIcon copyAllMenuIcon = new FontIcon("fas-copy");
        copyAllItem.setGraphic(copyAllMenuIcon);
        copyAllItem.setOnAction(e -> copyAllToClipboard());

        MenuItem clearItem = new MenuItem("Clear Console");
        FontIcon clearMenuIcon = new FontIcon("fas-trash-alt");
        clearItem.setGraphic(clearMenuIcon);
        clearItem.setOnAction(e -> clear());
        contextMenu.getItems().addAll(copyItem, copyAllItem, clearItem);

        textArea.setContextMenu(contextMenu);

        VBox.setVgrow(vsPane, Priority.ALWAYS);
        getChildren().addAll(header, vsPane);

        ThemeManager.addListener(this::onThemeChanged);

        appendRaw("\033[2m[Sovereign Builder Studio]\033[0m Initialized — System active…\n");
    }

    private void onThemeChanged(String themeName) {
        // Refresh styling for all existing text when theme changes
        // Since we use lookups like -sui-text-main, we don't necessarily need to re-apply everything,
        // but RichTextFX might need a nudge if colors were hardcoded or if we want to ensure
        // tokens are re-resolved.
        // Actually, DEFAULT_FG is "-sui-text-main", so it's a lookup.
        // The main thing is that buildStyle() will now use the new token values if called again.
    }

    public void copyAllToClipboard() {
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(textArea.getText());
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
    }

    public void log(String message) {
        Platform.runLater(() -> appendRaw(message));
    }

    public void clear() {
        Platform.runLater(() -> {
            textArea.clear();
            currentLineStart = 0;
        });
    }

    private String fgColor  = DEFAULT_FG;
    private String bgColor  = null;
    private boolean bold    = false;
    private boolean dim     = false;
    private boolean italic  = false;
    private boolean underline = false;

    private void appendRaw(String raw) {
        if (raw == null) return;
        raw = raw.replace("\r\n", "\n");
        int len = raw.length();
        int i = 0;
        StringBuilder plain = new StringBuilder();

        while (i < len) {
            char c = raw.charAt(i);

            if (c == '\r') {
                flushPlain(plain);
                plain.setLength(0);
                eraseCurrentLine();
                i++;
                if (i < len && raw.charAt(i) == '\n') {
                    commitNewline();
                    i++;
                }

            } else if (c == '\n') {
                flushPlain(plain);
                plain.setLength(0);
                commitNewline();
                i++;

            } else if (c == '\033' && i + 1 < len) {
                flushPlain(plain);
                plain.setLength(0);
                char next = raw.charAt(i + 1);
                if (next == '[') {
                    i += 2;
                    int seqStart = i;
                    while (i < len && !isFinalByte(raw.charAt(i))) {
                        i++;
                    }
                    if (i < len) {
                        char finalByte = raw.charAt(i);
                        String params = raw.substring(seqStart, i);
                        i++;
                        if (finalByte == 'm') {
                            applySgr(params);
                        } else if (finalByte == 'K') {
                            eraseCurrentLine();
                        }
                    }
                } else {
                    i += 2;
                }

            } else {
                plain.append(c);
                i++;
            }
        }
        flushPlain(plain);
        textArea.moveTo(textArea.getLength());
        textArea.requestFollowCaret();
    }
    
    private boolean isFinalByte(char c) {
        return c == 'm' || c == 'K' || c == 'J' || c == 'A' || c == 'B' || c == 'C' || c == 'D' || c == 'H' || c == 'f' || c == 's' || c == 'u';
    }

    private void flushPlain(StringBuilder sb) {
        if (sb.length() == 0) return;
        int start = textArea.getLength();
        textArea.appendText(sb.toString());
        int end = textArea.getLength();
        textArea.setStyle(start, end, buildStyle());
    }

    private void eraseCurrentLine() {
        int end = textArea.getLength();
        if (end > currentLineStart) {
            textArea.deleteText(currentLineStart, end);
        }
    }

    private void commitNewline() {
        int start = textArea.getLength();
        textArea.appendText("\n");
        int end = textArea.getLength();
        textArea.setStyle(start, end, buildStyle());
        currentLineStart = textArea.getLength();
    }

    private void applySgr(String params) {
        if (params.isEmpty()) {
            resetAttrs();
            return;
        }
        String[] codes = params.split(";");
        int idx = 0;
        while (idx < codes.length) {
            int code;
            try { code = Integer.parseInt(codes[idx].trim()); }
            catch (NumberFormatException e) { idx++; continue; }

            switch (code) {
                case 0: resetAttrs(); break;
                case 1: bold = true; break;
                case 2: dim = true; break;
                case 3: italic = true; break;
                case 4: underline = true; break;
                case 22: bold = false; dim = false; break;
                case 23: italic = false; break;
                case 24: underline = false; break;
                case 30: fgColor = "#555555"; break;
                case 31: fgColor = "#ff5555"; break;
                case 32: fgColor = "#50fa7b"; break;
                case 33: fgColor = "#f1fa8c"; break;
                case 34: fgColor = "#6272a4"; break;
                case 35: fgColor = "#ff79c6"; break;
                case 36: fgColor = "#8be9fd"; break;
                case 37: fgColor = "#bbbbbb"; break;
                case 39: fgColor = DEFAULT_FG; break;
                case 40: bgColor = "#000000"; break;
                case 41: bgColor = "#8b0000"; break;
                case 42: bgColor = "#006400"; break;
                case 43: bgColor = "#8b8000"; break;
                case 44: bgColor = "#00008b"; break;
                case 45: bgColor = "#8b0082"; break;
                case 46: bgColor = "#008b8b"; break;
                case 47: bgColor = "#c0c0c0"; break;
                case 49: bgColor = null; break;
                case 90: fgColor = "#808080"; break;
                case 91: fgColor = "#ff6e6e"; break;
                case 92: fgColor = "#69ff94"; break;
                case 93: fgColor = "#ffffa5"; break;
                case 94: fgColor = "#d6acff"; break;
                case 95: fgColor = "#ff92df"; break;
                case 96: fgColor = "#a4ffff"; break;
                case 97: fgColor = "#ffffff"; break;
                case 100: bgColor = "#555555"; break;
                case 101: bgColor = "#ff5555"; break;
                case 102: bgColor = "#50fa7b"; break;
                case 103: bgColor = "#f1fa8c"; break;
                case 104: bgColor = "#6272a4"; break;
                case 105: bgColor = "#ff79c6"; break;
                case 106: bgColor = "#8be9fd"; break;
                case 107: bgColor = "#f8f8f2"; break;
                case 38: {
                    if (idx + 1 < codes.length) {
                        int type;
                        try { type = Integer.parseInt(codes[idx + 1].trim()); }
                        catch (NumberFormatException e) { break; }
                        if (type == 5 && idx + 2 < codes.length) {
                            try {
                                int n = Integer.parseInt(codes[idx + 2].trim());
                                fgColor = ansi256toHex(n);
                            } catch (NumberFormatException e) { }
                            idx += 2;
                        } else if (type == 2 && idx + 4 < codes.length) {
                            try {
                                int r = Integer.parseInt(codes[idx + 2].trim());
                                int g = Integer.parseInt(codes[idx + 3].trim());
                                int b = Integer.parseInt(codes[idx + 4].trim());
                                fgColor = String.format("#%02x%02x%02x", r, g, b);
                            } catch (NumberFormatException e) { }
                            idx += 4;
                        }
                    }
                    break;
                }
                case 48: {
                    if (idx + 1 < codes.length) {
                        int type;
                        try { type = Integer.parseInt(codes[idx + 1].trim()); }
                        catch (NumberFormatException e) { break; }
                        if (type == 5 && idx + 2 < codes.length) {
                            try {
                                int n = Integer.parseInt(codes[idx + 2].trim());
                                bgColor = ansi256toHex(n);
                            } catch (NumberFormatException e) { }
                            idx += 2;
                        } else if (type == 2 && idx + 4 < codes.length) {
                            try {
                                int r = Integer.parseInt(codes[idx + 2].trim());
                                int g = Integer.parseInt(codes[idx + 3].trim());
                                int b = Integer.parseInt(codes[idx + 4].trim());
                                bgColor = String.format("#%02x%02x%02x", r, g, b);
                            } catch (NumberFormatException e) { }
                            idx += 4;
                        }
                    }
                    break;
                }
                default: break;
            }
            idx++;
        }
    }

    private void resetAttrs() {
        fgColor   = DEFAULT_FG;
        bgColor   = null;
        bold      = false;
        dim       = false;
        italic    = false;
        underline = false;
    }

    private String buildStyle() {
        StringBuilder sb = new StringBuilder();
        sb.append("-fx-font-family: ").append(FONT_FAMILY).append(";");
        sb.append("-fx-font-size: ").append(FONT_SIZE).append(";");
        String fg = fgColor;
        if (dim) {
            if ("-sui-text-main".equals(fg)) {
                fg = "-sui-text-dim";
            } else {
                fg = dimColor(fg);
            }
        }
        sb.append("-fx-fill: ").append(fg).append(";");
        if (bgColor != null)
            sb.append("-fx-background-color: ").append(bgColor).append(";");
        if (bold)
            sb.append("-fx-font-weight: bold;");
        if (italic)
            sb.append("-fx-font-style: italic;");
        if (underline)
            sb.append("-fx-underline: true;");
        return sb.toString();
    }

    private String dimColor(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return String.format("#%02x%02x%02x", r / 2, g / 2, b / 2);
        } catch (Exception e) {
            return hex;
        }
    }

    private static final String[] ANSI_256;
    static {
        ANSI_256 = new String[256];
        int[] base = {
            0x000000,0xcc0000,0x4e9a06,0xc4a000,0x3465a4,0x75507b,0x06989a,0xd3d7cf,
            0x555753,0xef2929,0x8ae234,0xfce94f,0x729fcf,0xad7fa8,0x34e2e2,0xeeeeec
        };
        for (int i = 0; i < 16; i++) {
            ANSI_256[i] = String.format("#%06x", base[i]);
        }
        for (int i = 16; i < 232; i++) {
            int idx = i - 16;
            int b = idx % 6;
            int g = (idx / 6) % 6;
            int r = idx / 36;
            int rv = r > 0 ? 55 + r * 40 : 0;
            int gv = g > 0 ? 55 + g * 40 : 0;
            int bv = b > 0 ? 55 + b * 40 : 0;
            ANSI_256[i] = String.format("#%02x%02x%02x", rv, gv, bv);
        }
        for (int i = 232; i < 256; i++) {
            int v = 8 + (i - 232) * 10;
            ANSI_256[i] = String.format("#%02x%02x%02x", v, v, v);
        }
    }

    private static String ansi256toHex(int n) {
        if (n < 0 || n >= 256) return DEFAULT_FG;
        return ANSI_256[n];
    }
}

package com.routebuilder.ui.components;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

/**
 * Centralized Design Token & Theme Engine for Sovereign UI.
 * Optimized for High-Contrast and reliable cross-component synchronization.
 */
public class ThemeManager {

    private static final Map<String, String> THEMES;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("VSCode Dark", "theme-vscode-dark");
        m.put("IntelliJ Light", "theme-intellij-light");
        m.put("Material Light", "theme-material-light");
        m.put("Solarized Light", "theme-solarized-light");
        m.put("Tokyo Night Light", "theme-tokyo-light");
        m.put("Nordic Frost", "theme-nordic");
        m.put("Dracula", "theme-dracula");
        m.put("Cyberpunk 2077", "theme-cyberpunk");
        m.put("Neon Dreams", "theme-neon-dreams");
        m.put("Cyber Glow", "theme-cyber-glow");
        m.put("Electric Violet", "theme-electric-violet");
        THEMES = Collections.unmodifiableMap(m);
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String PREF_THEME = "app_theme";
    
    // We use a WeakHashMap to avoid memory leaks while keeping track of all roots
    private static final Set<Parent> registeredRoots = Collections.newSetFromMap(new WeakHashMap<>());
    
    private static String currentThemeName = PREFS.get(PREF_THEME, "VSCode Dark");
    private static String currentThemeClass = THEMES.getOrDefault(currentThemeName, "theme-vscode-dark");

    public static Map<String, String> getAvailableThemes() { return THEMES; }
    public static String getCurrentThemeName() { return currentThemeName; }
    public static String getCurrentThemeClass() { return currentThemeClass; }

    public static void registerRoot(Parent root) {
        if (root != null) {
            registeredRoots.add(root);
            applyThemeToRoot(root);
        }
    }

    public static void applyTheme(String themeName) {
        if (themeName == null || !THEMES.containsKey(themeName)) return;
        currentThemeName = themeName;
        currentThemeClass = THEMES.get(themeName);
        PREFS.put(PREF_THEME, themeName);
        
        for (Parent root : registeredRoots) {
            if (root != null) applyThemeToRoot(root);
        }
        notifyThemeChangeListeners(themeName);
    }

    private static void applyThemeToRoot(Parent root) {
        root.getStyleClass().removeAll(THEMES.values());
        root.getStyleClass().add(currentThemeClass);
        
        // Define tokens
        String bgPrimary = "#1e1e1e";
        String bgSecondary = "#252526";
        String accentPrimary = "#007acc";
        String textMain = "#d4d4d4"; 
        String textDim = "#909090";
        String borderColor = "#404040";
        String selectionColor = "#264f78";
        String iconColor = "#d4d4d4";

        if (currentThemeClass.contains("light")) {
            bgPrimary = "#ffffff"; bgSecondary = "#f3f3f3"; accentPrimary = "#005fb8";
            textMain = "#1a1a1a"; textDim = "#5a5a5a"; borderColor = "#c8c8c8"; selectionColor = "#add6ff"; iconColor = "#1a1a1a";
        } else if (currentThemeClass.equals("theme-nordic")) {
            bgPrimary = "#2e3440"; bgSecondary = "#3b4252"; accentPrimary = "#88c0d0";
            textMain = "#eceff4"; textDim = "#d8dee9"; borderColor = "#4c566a"; selectionColor = "#434c5e"; iconColor = "#8fbcbb";
        } else if (currentThemeClass.equals("theme-dracula")) {
            bgPrimary = "#282a36"; bgSecondary = "#1e1f29"; accentPrimary = "#bd93f9";
            textMain = "#f8f8f2"; textDim = "#6272a4"; borderColor = "#44475a"; selectionColor = "#44475a"; iconColor = "#f8f8f2";
        } else if (currentThemeClass.equals("theme-monokai")) {
            bgPrimary = "#2d2a2e"; bgSecondary = "#221f22"; accentPrimary = "#ffd866";
            textMain = "#fcfcfa"; textDim = "#939293"; borderColor = "#403e41"; selectionColor = "#5b595c"; iconColor = "#ffd866";
        } else if (currentThemeClass.equals("theme-cyberpunk")) {
            bgPrimary = "#000000"; bgSecondary = "#0d0208"; accentPrimary = "#f3f315";
            textMain = "#f3f315"; textDim = "#00ff41"; borderColor = "#003b00"; selectionColor = "#333300"; iconColor = "#f3f315";
        } else if (currentThemeClass.equals("theme-midnight")) {
            bgPrimary = "#05070a"; bgSecondary = "#0f131a"; accentPrimary = "#ff007c";
            textMain = "#ffffff"; textDim = "#aab1ff"; borderColor = "#1c212b"; selectionColor = "#3d102e"; iconColor = "#ff007c";
        }

        // Apply tokens as inline styles to ensure they are available for CSS lookups in sub-components
        StringBuilder sb = new StringBuilder();
        sb.append("-sui-bg-primary: ").append(bgPrimary).append(";");
        sb.append("-sui-bg-secondary: ").append(bgSecondary).append(";");
        sb.append("-sui-accent-primary: ").append(accentPrimary).append(";");
        sb.append("-sui-text-main: ").append(textMain).append(";");
        sb.append("-sui-text-dim: ").append(textDim).append(";");
        sb.append("-sui-border-color: ").append(borderColor).append(";");
        sb.append("-sui-selection: ").append(selectionColor).append(";");
        sb.append("-sui-icon-color: ").append(iconColor).append(";");
        
        // Legacy support
        sb.append("-console-fg: ").append(textMain).append(";");
        sb.append("-console-dim-fg: ").append(textDim).append(";");

        root.setStyle(sb.toString());
    }

    public static String getCurrentDynamicCssUri() {
        // No longer needed as we use setStyle, but kept for compatibility
        return null;
    }

    public interface ThemeChangeListener { void onThemeChanged(String name); }
    private static final java.util.List<ThemeChangeListener> listeners = new java.util.ArrayList<>();
    public static void addListener(ThemeChangeListener l) { if (l != null) listeners.add(l); }
    public static void removeListener(ThemeChangeListener l) { listeners.remove(l); }
    
    private static void notifyThemeChangeListeners(String newTheme) {
        Platform.runLater(() -> {
            for (ThemeChangeListener l : new java.util.ArrayList<>(listeners)) {
                if (l != null) l.onThemeChanged(newTheme);
            }
        });
    }
}

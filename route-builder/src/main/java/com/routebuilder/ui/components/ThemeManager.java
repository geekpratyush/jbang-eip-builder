package com.routebuilder.ui.components;

import javafx.scene.Parent;
import javafx.scene.Scene;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

public class ThemeManager {

    private static final Map<String, String> THEMES;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("VSCode Dark", "theme-vscode-dark");
        m.put("IntelliJ Light", "theme-intellij-light");
        m.put("GitHub Dark", "theme-github-dark");
        m.put("Dracula", "theme-dracula");
        m.put("Monokai Pro", "theme-monokai");
        m.put("Solarized Dark", "theme-solarized");
        m.put("Cyberpunk / Hacker", "theme-cyberpunk");
        m.put("Enterprise Blue", "theme-enterprise");
        THEMES = Collections.unmodifiableMap(m);
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String PREF_THEME = "app_theme";

    // Use WeakHashMap to avoid memory leaks when stages are closed
    private static final Set<Parent> registeredRoots = Collections.newSetFromMap(new WeakHashMap<>());
    
    private static String currentThemeName = PREFS.get(PREF_THEME, "VSCode Dark");
    private static String currentThemeClass = THEMES.getOrDefault(currentThemeName, "theme-vscode-dark");
    private static String currentDynamicCssUri = null;

    public static Map<String, String> getAvailableThemes() {
        return THEMES;
    }

    public static String getCurrentThemeName() {
        return currentThemeName;
    }

    public static String getCurrentThemeClass() {
        return currentThemeClass;
    }

    public static String getCurrentDynamicCssUri() {
        return currentDynamicCssUri;
    }

    /**
     * Registers a root node so it receives theme updates dynamically.
     */
    public static void registerRoot(Parent root) {
        if (root != null) {
            registeredRoots.add(root);
            applyThemeClassAndCss(root, currentThemeClass, currentDynamicCssUri);
        }
    }

    /**
     * Applies a new theme to all registered roots and saves the preference.
     */
    public static void applyTheme(String themeName) {
        if (themeName == null || !THEMES.containsKey(themeName)) return;

        currentThemeName = themeName;
        currentThemeClass = THEMES.get(themeName);
        PREFS.put(PREF_THEME, themeName);

        currentDynamicCssUri = generateDynamicCss(currentThemeClass);

        for (Parent root : registeredRoots) {
            applyThemeClassAndCss(root, currentThemeClass, currentDynamicCssUri);
        }
        
        notifyThemeChangeListeners(themeName);
    }

    private static String generateDynamicCss(String cssClass) {
        String baseColor = "#1e1e1e";
        String textColor = "#cccccc";
        if (cssClass.equals("theme-intellij-light") || cssClass.equals("theme-github-light")) { baseColor = "#ffffff"; textColor = "#333333"; }
        else if (cssClass.equals("theme-dracula")) { baseColor = "#282a36"; textColor = "#f8f8f2"; }
        else if (cssClass.equals("theme-monokai")) { baseColor = "#272822"; textColor = "#f8f8f2"; }
        else if (cssClass.equals("theme-cyberpunk")) { baseColor = "#050505"; textColor = "#00ff00"; }
        else if (cssClass.equals("theme-hacker")) { baseColor = "#050505"; textColor = "#00ff00"; }

        String keyCol = cssClass.equals("theme-intellij-light") ? "#0000ff" : "#9cdcfe";
        String strCol = cssClass.equals("theme-intellij-light") ? "#a31515" : "#ce9178";
        String numCol = cssClass.equals("theme-intellij-light") ? "#098658" : "#b5cea8";
        String tagCol = cssClass.equals("theme-intellij-light") ? "#800000" : "#569cd6";
        String editorBg = cssClass.equals("theme-intellij-light") ? "#ffffff" : "#1e1e1e";
        String editorFg = cssClass.equals("theme-intellij-light") ? "#333333" : "#d4d4d4";
        String borderCol = cssClass.equals("theme-intellij-light") ? "#cccccc" : "#444444";

        String menuBgCol = cssClass.equals("theme-intellij-light") ? "#f3f3f3" : "#252526";
        String menuBorderCol = cssClass.equals("theme-intellij-light") ? "#cccccc" : "#454545";
        String menuHoverCol = cssClass.equals("theme-intellij-light") ? "#e5e5e5" : "#094771";
        String menuTextCol = cssClass.equals("theme-intellij-light") ? "#333333" : "#cccccc";
        String menuTextHoverCol = cssClass.equals("theme-intellij-light") ? "#000000" : "#ffffff";

        String dynamicCss = 
            ".root { -fx-base: " + baseColor + "; -fx-control-inner-background: " + baseColor + "; -fx-background-color: " + baseColor + "; -fx-text-base-color: " + textColor + "; -fx-text-background-color: " + textColor + "; -fx-text-fill: " + textColor + "; }\n" +
            ".context-menu { -fx-background-color: " + menuBgCol + "; -fx-border-color: " + menuBorderCol + "; }\n" +
            ".context-menu .menu-item:focused { -fx-background-color: " + menuHoverCol + "; }\n" +
            ".context-menu .menu-item .label { -fx-text-fill: " + menuTextCol + "; }\n" +
            ".context-menu .menu-item:focused .label { -fx-text-fill: " + menuTextHoverCol + "; }\n" +
            ".context-menu .ikonli-font-icon { -fx-icon-color: " + menuTextCol + "; }\n" +
            ".context-menu .menu-item:focused .ikonli-font-icon { -fx-icon-color: " + menuTextHoverCol + "; }\n" +
            ".syntax-editor .text.syntax-key { -fx-fill: " + keyCol + "; }\n" +
            ".syntax-editor .text.syntax-string { -fx-fill: " + strCol + "; }\n" +
            ".syntax-editor .text.syntax-number { -fx-fill: " + numCol + "; }\n" +
            ".syntax-editor .text.syntax-keyword { -fx-fill: " + keyCol + "; -fx-font-weight: bold; }\n" +
            ".syntax-editor .text.syntax-tag { -fx-fill: " + tagCol + "; }\n" +
            ".syntax-editor .text.syntax-attr { -fx-fill: " + keyCol + "; }\n" +
            ".syntax-editor { -fx-font-family: 'Monospaced'; -fx-font-size: 13px; -fx-background-color: " + editorBg + "; -fx-border-color: " + borderCol + "; -fx-border-radius: 3px; }\n" +
            ".syntax-editor .text { -fx-fill: " + editorFg + "; }";
            
        try {
            return "data:text/css;charset=utf-8," + java.net.URLEncoder.encode(dynamicCss, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return null;
        }
    }

    private static void applyThemeClassAndCss(Parent root, String themeClass, String dynamicCssUri) {
        root.getStyleClass().removeAll(THEMES.values());
        if (!root.getStyleClass().contains(themeClass)) {
            root.getStyleClass().add(themeClass);
        }
        Scene scene = root.getScene();
        if (scene != null) {
            scene.getStylesheets().removeIf(s -> s.startsWith("data:text/css"));
            if (dynamicCssUri != null) {
                scene.getStylesheets().add(dynamicCssUri);
            }
        }
    }
    
    // --- Listener Support ---
    public interface ThemeChangeListener {
        void onThemeChanged(String newThemeName);
    }
    
    private static final Set<ThemeChangeListener> listeners = Collections.newSetFromMap(new WeakHashMap<>());
    
    public static void addListener(ThemeChangeListener listener) {
        listeners.add(listener);
    }
    
    private static void notifyThemeChangeListeners(String newTheme) {
        for (ThemeChangeListener listener : listeners) {
            if (listener != null) listener.onThemeChanged(newTheme);
        }
    }
}

package com.tessera.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Standardized Component Factory for the Sovereign UI Framework (SUI).
 * Ensures consistency across all studio windows.
 */
public class SuiKit {

    /**
     * Creates a standardized Studio Header with icon and title.
     */
    public static HBox createStudioHeader(String title, String iconCode) {
        HBox header = new HBox(8);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("sui-studio-header");
        
        // Logical styling instead of hardcoded hex
        header.setStyle("-fx-background-color: -sui-bg-secondary; -fx-border-color: -sui-border-color; -fx-border-width: 0 0 1 0;");

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(18);
        icon.setStyle("-fx-icon-color: -sui-accent-primary;");

        Label lblTitle = new Label(title.toUpperCase());
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -sui-text-main; -fx-font-size: 14px;");

        header.getChildren().addAll(icon, lblTitle);
        return header;
    }

    /**
     * Creates a standardized primary action button.
     */
    public static Button createActionButton(String label, String iconCode, String styleClass, Runnable action) {
        Button btn = new Button(label, new FontIcon(iconCode));
        btn.getStyleClass().addAll("sui-button", styleClass);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    /**
     * Creates a flexible toolbar spacer.
     */
    public static Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /**
     * Standardized Pane Title for internal dividers.
     */
    public static Label createPaneTitle(String text) {
        Label lbl = new Label(text.toUpperCase());
        lbl.getStyleClass().add("sui-pane-title");
        lbl.setPadding(new Insets(5, 10, 5, 10));
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -sui-text-dim; -fx-background-color: -sui-bg-secondary;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }
}

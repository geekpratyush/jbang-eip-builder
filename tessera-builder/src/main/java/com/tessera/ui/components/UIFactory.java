package com.tessera.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

public class UIFactory {

    /**
     * Creates a standardized small icon button for toolbars/headers.
     */
    public static Button createIconButton(String iconLiteral, String tooltipText) {
        Button btn = new Button();
        FontIcon icon = new FontIcon(iconLiteral);
        btn.setGraphic(icon);
        if (tooltipText != null && !tooltipText.isEmpty()) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        btn.getStyleClass().add("small-action-btn");
        return btn;
    }

    /**
     * Creates a standardized editor panel header (Label + Spacer + Actions).
     * Used extensively above WebViews.
     */
    public static HBox createPanelHeader(String title, Node... actions) {
        HBox header = new HBox(5);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(5, 10, 5, 10));
        header.getStyleClass().add("editor-header");

        Label lbl = new Label(title);
        lbl.getStyleClass().add("editor-header-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(lbl, spacer);
        if (actions != null) {
            header.getChildren().addAll(actions);
        }

        return header;
    }

    /**
     * Creates a standard App/Window Header (Title + Icons)
     */
    public static HBox createWindowHeader(String title, Node... actions) {
        HBox header = new HBox(20);
        header.setPadding(new Insets(15, 30, 15, 30));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("window-header-bar");
        
        Label lblTitle = new Label(title.toUpperCase());
        lblTitle.getStyleClass().add("window-title-label");
        
        Region spacer = new Region(); 
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(lblTitle, spacer);
        if (actions != null) {
            header.getChildren().addAll(actions);
        }
        return header;
    }
}

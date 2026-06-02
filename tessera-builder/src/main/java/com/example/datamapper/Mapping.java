package com.example.datamapper;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Simple model representing a source‑target mapping expression.
 */
public class Mapping {
    private final StringProperty source = new SimpleStringProperty();
    private final StringProperty target = new SimpleStringProperty();

    public Mapping() {
        this("", "");
    }

    public Mapping(String source, String target) {
        this.source.set(source);
        this.target.set(target);
    }

    public String getSource() {
        return source.get();
    }

    public void setSource(String source) {
        this.source.set(source);
    }

    public StringProperty sourceProperty() {
        return source;
    }

    public String getTarget() {
        return target.get();
    }

    public void setTarget(String target) {
        this.target.set(target);
    }

    public StringProperty targetProperty() {
        return target;
    }
}

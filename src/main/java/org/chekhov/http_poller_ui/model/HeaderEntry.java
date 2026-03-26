package org.chekhov.http_poller_ui.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class HeaderEntry {
    private final StringProperty key = new SimpleStringProperty();
    private final StringProperty value = new SimpleStringProperty();

    public HeaderEntry(String key, String value) {
        this.key.set(key);
        this.value.set(value);
    }

    public StringProperty keyProperty()   { return key; }
    public StringProperty valueProperty() { return value; }
    public String getKey()   { return key.get(); }
    public String getValue() { return value.get(); }
}

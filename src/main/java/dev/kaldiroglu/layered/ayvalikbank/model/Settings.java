package dev.kaldiroglu.layered.ayvalikbank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "settings")
public class Settings {

    @Id
    private String key;

    @Column(nullable = false)
    private String value;

    public Settings() {}

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}

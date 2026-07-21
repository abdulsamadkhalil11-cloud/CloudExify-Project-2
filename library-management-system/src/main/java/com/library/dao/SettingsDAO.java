package com.library.dao;

import java.util.Map;

public interface SettingsDAO {

    String getValue(String key, String defaultValue);

    void setValue(String key, String value);

    Map<String, String> getAll();
}

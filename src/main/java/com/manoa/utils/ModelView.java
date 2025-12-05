package com.manoa.utils;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    String view;
    HashMap<String, Object> data = new HashMap<>();

    public void setView(String view) {
        this.view = view;
    }

    public String getView() {
        return view;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void addData(String key, Object data) {
        this.data.put(key, data);
    }
}

package com.mazouri.fork.daogenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangdong on 16-1-19.
 */
public class PropertyOrderList {

    private List<Property> properties;
    private List<String> propertiesOrder;

    public PropertyOrderList() {
        properties = new ArrayList<Property>();
        propertiesOrder = new ArrayList<String>();
    }

    public void addProperty(Property property) {
        properties.add(property);
        propertiesOrder.add(null);
    }

    public void addPropertyAsc(Property property) {
        properties.add(property);
        propertiesOrder.add("ASC");
    }

    public void addPropertyDesc(Property property) {
        properties.add(property);
        propertiesOrder.add("DESC");
    }

    public void addOrderRaw(String order) {
        properties.add(null);
        propertiesOrder.add(order);
    }

    public List<Property> getProperties() {
        return properties;
    }

    List<String> getPropertiesOrder() {
        return propertiesOrder;
    }

    public String getCommaSeparatedString(String tablePrefixOrNull) {
        StringBuilder builder = new StringBuilder();
        int size = properties.size();
        for (int i = 0; i < size; i++) {
            Property property = properties.get(i);
            String order = propertiesOrder.get(i);
            if (property != null) {
                if(tablePrefixOrNull != null) {
                    builder.append(tablePrefixOrNull).append('.');
                }
                builder.append('\'').append(property.getColumnName()).append('\'').append(' ');
            }
            if (order != null) {
                builder.append(order);
            }
            if (i < size - 1) {
                builder.append(',');
            }
        }
        return builder.toString();
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }
}

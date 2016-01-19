package com.mazouri.fork.daogenerator;

/**
 * Created by wangdong on 16-1-19.
 */
public class Index extends PropertyOrderList {

    private String name;
    private boolean unique;

    public String getName() {
        return name;
    }

    public Index setName(String name) {
        this.name = name;
        return this;
    }

    public Index makeUnique() {
        unique = true;
        return this;
    }

    public boolean isUnique() {
        return unique;
    }
}

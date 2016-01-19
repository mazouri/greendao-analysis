package com.mazouri.fork.daogenerator;

/**
 * Created by wangdong on 16-1-19.
 */

import java.util.ArrayList;
import java.util.List;

/** NOT IMPLEMENTED YET. Check back later. */
public class Query {
    @SuppressWarnings("unused")
    private String name;
    private List<QueryParam> parameters;
    @SuppressWarnings("unused")
    private boolean distinct;

    public Query(String name) {
        this.name = name;
        parameters= new ArrayList<QueryParam>();
    }

    public QueryParam addEqualsParam(Property column) {
        return addParam(column, "=");
    }

    public QueryParam addParam(Property column, String operator) {
        QueryParam queryParam = new QueryParam(column, operator);
        parameters.add(queryParam);
        return queryParam;
    }

    public void distinct() {
        distinct = true;
    }
}

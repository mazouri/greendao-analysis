package com.mazouri.fork.greendao.query;

/**
 * 内部使用的类，来收集Where条件
 *
 * Created by wangdong on 16-1-19.
 */

import com.mazouri.fork.greendao.AbstractDao;
import com.mazouri.fork.greendao.DaoException;
import com.mazouri.fork.greendao.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/** Internal class to collect WHERE conditions. */
class WhereCollector<T> {
    private final AbstractDao<T, ?> dao;
    private final List<WhereCondition> whereConditions;
    private final String tablePrefix;

    WhereCollector(AbstractDao<T, ?> dao, String tablePrefix) {
        this.dao = dao;
        this.tablePrefix = tablePrefix;
        whereConditions = new ArrayList<WhereCondition>();
    }

    //将指定的whereCondition的对象以及集合依次添加到集合中
    void add(WhereCondition cond, WhereCondition... condMore) {
        checkCondition(cond);
        whereConditions.add(cond);
        for (WhereCondition whereCondition : condMore) {
            checkCondition(whereCondition);
            whereConditions.add(whereCondition);
        }
    }

    WhereCondition combineWhereConditions(String combineOp, WhereCondition cond1, WhereCondition cond2,
                                          WhereCondition... condMore) {
        StringBuilder builder = new StringBuilder("(");
        List<Object> combinedValues = new ArrayList<Object>();

        addCondition(builder, combinedValues, cond1);
        builder.append(combineOp);
        addCondition(builder, combinedValues, cond2);

        for (WhereCondition cond : condMore) {
            builder.append(combineOp);
            addCondition(builder, combinedValues, cond);
        }
        builder.append(')');
        return new WhereCondition.StringCondition(builder.toString(), combinedValues.toArray());
    }

    void addCondition(StringBuilder builder, List<Object> values, WhereCondition condition) {
        checkCondition(condition);
        condition.appendTo(builder, tablePrefix);
        condition.appendValuesTo(values);
    }

    void checkCondition(WhereCondition whereCondition) {
        if (whereCondition instanceof WhereCondition.PropertyCondition) {
            checkProperty(((WhereCondition.PropertyCondition) whereCondition).property);
        }
    }

    //检查指定的property是否是Dao中的属性的一部分，通过反射判断
    void checkProperty(Property property) {
        if (dao != null) {
            Property[] properties = dao.getProperties();
            boolean found = false;
            for (Property property2 : properties) {
                if (property == property2) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new DaoException("Property '" + property.name + "' is not part of " + dao);
            }
        }
    }

    void appendWhereClause(StringBuilder builder, String tablePrefixOrNull, List<Object> values) {
        ListIterator<WhereCondition> iter = whereConditions.listIterator();
        while (iter.hasNext()) {
            if (iter.hasPrevious()) {
                builder.append(" AND ");
            }
            WhereCondition condition = iter.next();
            condition.appendTo(builder, tablePrefixOrNull);
            condition.appendValuesTo(values);
        }
    }

    boolean isEmpty() {
        return whereConditions.isEmpty();
    }
}

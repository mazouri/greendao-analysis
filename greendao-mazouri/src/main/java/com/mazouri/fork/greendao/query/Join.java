package com.mazouri.fork.greendao.query;

import com.mazouri.fork.greendao.AbstractDao;
import com.mazouri.fork.greendao.Property;

/**
 * A Join lets you relate to other entity types for queries, and allows using WHERE statements on the joined entity
 * type.
 *
 * 允许与其他的相关的类型的实体类做查询，在做Join的实体对象上允许使用where
 *
 * Created by wangdong on 16-1-18.
 */
public class Join<SRC, DST> {

    final String sourceTablePrefix;
    final AbstractDao<DST, ?> daoDestination;

    final Property joinPropertySource;
    final Property joinPropertyDestination;
    final String tablePrefix;
    final WhereCollector<DST> whereCollector;

    public Join(String sourceTablePrefix, Property sourceJoinProperty,
                AbstractDao<DST, ?> daoDestination, Property destinationJoinProperty,
                String joinTablePrefix) {
        this.sourceTablePrefix = sourceTablePrefix;
        this.joinPropertySource = sourceJoinProperty;
        this.daoDestination = daoDestination;
        this.joinPropertyDestination = destinationJoinProperty;
        tablePrefix = joinTablePrefix;
        whereCollector = new WhereCollector<DST>(daoDestination, joinTablePrefix);
    }


    /**
     * Adds the given conditions to the where clause using an logical AND. To create new conditions, use the properties
     * given in the generated dao classes.
     */
    public Join<SRC, DST> where(WhereCondition cond, WhereCondition... condMore) {
        whereCollector.add(cond, condMore);
        return this;
    }

    /**
     * Adds the given conditions to the where clause using an logical OR. To create new conditions, use the properties
     * given in the generated dao classes.
     */
    public Join<SRC, DST> whereOr(WhereCondition cond1, WhereCondition cond2, WhereCondition... condMore) {
        whereCollector.add(or(cond1, cond2, condMore));
        return this;
    }

    /**
     * Creates a WhereCondition by combining the given conditions using OR. The returned WhereCondition must be used
     * inside {@link #where(WhereCondition, WhereCondition...)} or
     * {@link #whereOr(WhereCondition, WhereCondition, WhereCondition...)}.
     */
    public WhereCondition or(WhereCondition cond1, WhereCondition cond2, WhereCondition... condMore) {
        return whereCollector.combineWhereConditions(" OR ", cond1, cond2, condMore);
    }

    /**
     * Creates a WhereCondition by combining the given conditions using AND. The returned WhereCondition must be used
     * inside {@link #where(WhereCondition, WhereCondition...)} or
     * {@link #whereOr(WhereCondition, WhereCondition, WhereCondition...)}.
     */
    public WhereCondition and(WhereCondition cond1, WhereCondition cond2, WhereCondition... condMore) {
        return whereCollector.combineWhereConditions(" AND ", cond1, cond2, condMore);
    }

    /**
     * Usually you don't need this value; just in case you are mixing custom
     * {@link WhereCondition.StringCondition} into the query, this value allows to reference
     * the joined (target) table.
     */
    public String getTablePrefix() {
        return tablePrefix;
    }
}

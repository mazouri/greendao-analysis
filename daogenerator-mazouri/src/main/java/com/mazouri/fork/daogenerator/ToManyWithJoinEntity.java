package com.mazouri.fork.daogenerator;

import java.util.List;

/**
 * To-many relationship to many target entities using a join entity (aka JOIN table).
 *
 * Created by wangdong on 16-1-19.
 */
public class ToManyWithJoinEntity extends ToManyBase {
    private final Entity joinEntity;
    private final Property sourceProperty;
    private final Property targetProperty;

    public ToManyWithJoinEntity(Schema schema, Entity sourceEntity, Entity targetEntity, Entity joinEntity,
                                Property sourceProperty, Property targetProperty) {
        super(schema, sourceEntity, targetEntity);
        this.joinEntity = joinEntity;
        this.sourceProperty = sourceProperty;
        this.targetProperty = targetProperty;
    }

    public Entity getJoinEntity() {
        return joinEntity;
    }

    public Property getSourceProperty() {
        return sourceProperty;
    }

    public Property getTargetProperty() {
        return targetProperty;
    }

    void init3rdPass() {
        super.init3rdPass();
        List<Property> pks = sourceEntity.getPropertiesPk();
        if (pks.isEmpty()) {
            throw new RuntimeException("Source entity has no primary key, but we need it for " + this);
        }
        List<Property> pks2 = targetEntity.getPropertiesPk();
        if (pks2.isEmpty()) {
            throw new RuntimeException("Target entity has no primary key, but we need it for " + this);
        }
    }
}

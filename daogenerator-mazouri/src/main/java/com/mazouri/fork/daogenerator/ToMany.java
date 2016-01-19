package com.mazouri.fork.daogenerator;

import java.util.List;

/**
 * To-many relationship from a source entity to many target entities.
 *
 * Created by wangdong on 16-1-19.
 */
public class ToMany extends ToManyBase {
    @SuppressWarnings("unused")
    private Property[] sourceProperties;
    private final Property[] targetProperties;

    public ToMany(Schema schema, Entity sourceEntity, Property[] sourceProperties, Entity targetEntity,
                  Property[] targetProperties) {
        super(schema, sourceEntity, targetEntity);
        this.sourceProperties = sourceProperties;
        this.targetProperties = targetProperties;
    }

    public Property[] getSourceProperties() {
        return sourceProperties;
    }

    public void setSourceProperties(Property[] sourceProperties) {
        this.sourceProperties = sourceProperties;
    }

    public Property[] getTargetProperties() {
        return targetProperties;
    }

    void init2ndPass() {
        super.init2ndPass();
        if (sourceProperties == null) {
            List<Property> pks = sourceEntity.getPropertiesPk();
            if (pks.isEmpty()) {
                throw new RuntimeException("Source entity has no primary key, but we need it for " + this);
            }
            sourceProperties = new Property[pks.size()];
            sourceProperties = pks.toArray(sourceProperties);
        }
        int count = sourceProperties.length;
        if (count != targetProperties.length) {
            throw new RuntimeException("Source properties do not match target properties: " + this);
        }

        for (int i = 0; i < count; i++) {
            Property sourceProperty = sourceProperties[i];
            Property targetProperty = targetProperties[i];

            PropertyType sourceType = sourceProperty.getPropertyType();
            PropertyType targetType = targetProperty.getPropertyType();
            if (sourceType == null || targetType == null) {
                throw new RuntimeException("Property type uninitialized");
            }
            if (sourceType != targetType) {
                System.err.println("Warning to-one property type does not match target key type: " + this);
            }
        }
    }

    void init3rdPass() {
        super.init3rdPass();
    }
}

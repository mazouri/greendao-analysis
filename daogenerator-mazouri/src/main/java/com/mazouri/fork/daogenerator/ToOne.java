package com.mazouri.fork.daogenerator;

/**
 * To-one relationship from a source entity to one (or zero) target entity.
 *
 * Created by wangdong on 16-1-19.
 */
public class ToOne {
    private final Schema schema;
    private final Entity sourceEntity;
    private final Entity targetEntity;
    private final Property[] fkProperties;
    private final String[] resolvedKeyJavaType;
    private final boolean[] resolvedKeyUseEquals;
    private String name;
    private final boolean useFkProperty;

    public ToOne(Schema schema, Entity sourceEntity, Entity targetEntity, Property[] fkProperties, boolean useFkProperty) {
        this.schema = schema;
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
        this.fkProperties = fkProperties;
        this.useFkProperty = useFkProperty;
        resolvedKeyJavaType = new String[fkProperties.length];
        resolvedKeyUseEquals = new boolean[fkProperties.length];
    }

    public Entity getSourceEntity() {
        return sourceEntity;
    }

    public Entity getTargetEntity() {
        return targetEntity;
    }

    public Property[] getFkProperties() {
        return fkProperties;
    }

    public String[] getResolvedKeyJavaType() {
        return resolvedKeyJavaType;
    }

    public boolean[] getResolvedKeyUseEquals() {
        return resolvedKeyUseEquals;
    }

    public String getName() {
        return name;
    }

    /**
     * Sets the name of the relation, which is used as the property name in the entity (the source entity owning the
     * to-many relationship).
     */
    public void setName(String name) {
        this.name = name;
    }

    public boolean isUseFkProperty() {
        return useFkProperty;
    }

    void init2ndPass() {
        if (name == null) {
            char[] nameCharArray = targetEntity.getClassName().toCharArray();
            nameCharArray[0] = Character.toLowerCase(nameCharArray[0]);
            name = new String(nameCharArray);
        }

    }

    /** Constructs fkColumns. Depends on 2nd pass of target key properties. */
    void init3ndPass() {

        Property targetPkProperty = targetEntity.getPkProperty();
        if (fkProperties.length != 1 || targetPkProperty == null) {
            throw new RuntimeException("Currently only single FK columns are supported: " + this);
        }

        Property property = fkProperties[0];
        PropertyType propertyType = property.getPropertyType();
        if (propertyType == null) {
            propertyType = targetPkProperty.getPropertyType();
            property.setPropertyType(propertyType);
            // Property is not a regular property with primitive getters/setters, so let it catch up
            property.init2ndPass();
            property.init3ndPass();
        } else if (propertyType != targetPkProperty.getPropertyType()) {
            System.err.println("Warning to-one property type does not match target key type: " + this);
        }
        resolvedKeyJavaType[0] = schema.mapToJavaTypeNullable(propertyType);
        resolvedKeyUseEquals[0] = checkUseEquals(propertyType);
    }

    protected boolean checkUseEquals(PropertyType propertyType) {
        boolean useEquals;
        switch (propertyType) {
            case Byte:
            case Short:
            case Int:
            case Long:
            case Boolean:
            case Float:
                useEquals = true;
                break;
            default:
                useEquals = false;
                break;
        }
        return useEquals;
    }

    @Override
    public String toString() {
        String sourceName = sourceEntity != null ? sourceEntity.getClassName() : null;
        String targetName = targetEntity != null ? targetEntity.getClassName() : null;
        return "ToOne '" + name + "' from " + sourceName + " to " + targetName;
    }
}

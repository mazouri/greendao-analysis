package com.mazouri.fork.greendao.query;

import com.mazouri.fork.greendao.DaoException;
import com.mazouri.fork.greendao.Property;
import com.mazouri.fork.greendao.internal.SqlUtils;

import java.util.Date;
import java.util.List;

/**
 * Internal interface to model WHERE conditions used in queries. Use the {@link Property} objects in the DAO classes to
 * create new conditions.
 *
 * 在查询的时候，将where条件进行对象处理化。使用Property对象在DaoClass中创建新的where条件
 *
 * Created by wangdong on 16-1-19.
 */
public interface WhereCondition {
    void appendTo(StringBuilder builder, String tableAlias);

    void appendValuesTo(List<Object> values);

    public abstract static class AbstractCondition implements WhereCondition {

        protected final boolean hasSingleValue;
        protected final Object value;
        protected final Object[] values;

        public AbstractCondition() {
            hasSingleValue = false;
            value = null;
            values = null;
        }

        public AbstractCondition(Object value) {
            this.value = value;
            hasSingleValue = true;
            values = null;
        }

        public AbstractCondition(Object[] values) {
            this.value = null;
            hasSingleValue = false;
            this.values = values;
        }

        @Override
        public void appendValuesTo(List<Object> valuesTarget) {
            if (hasSingleValue) {
                valuesTarget.add(value);
            } else if (values != null) {
                for (Object value : values) {
                    valuesTarget.add(value);
                }
            }
        }
    }

    public static class PropertyCondition extends AbstractCondition {

        private static Object checkValueForType(Property property, Object value) {
            if (value != null && value.getClass().isArray()) {
                throw new DaoException("Illegal value: found array, but simple object required");
            }
            Class<?> type = property.type;
            if (type == Date.class) {
                if (value instanceof Date) {
                    return ((Date) value).getTime();
                } else if (value instanceof Long) {
                    return value;
                } else {
                    throw new DaoException("Illegal date value: expected java.util.Date or Long for value " + value);
                }
            } else if (property.type == boolean.class || property.type == Boolean.class) {
                if (value instanceof Boolean) {
                    return ((Boolean) value) ? 1 : 0;
                } else if (value instanceof Number) {
                    int intValue = ((Number) value).intValue();
                    if (intValue != 0 && intValue != 1) {
                        throw new DaoException("Illegal boolean value: numbers must be 0 or 1, but was " + value);
                    }
                } else if (value instanceof String) {
                    String stringValue = ((String) value);
                    if ("TRUE".equalsIgnoreCase(stringValue)) {
                        return 1;
                    } else if ("FALSE".equalsIgnoreCase(stringValue)) {
                        return 0;
                    } else {
                        throw new DaoException(
                                "Illegal boolean value: Strings must be \"TRUE\" or \"FALSE\" (case insensitive), but was "
                                        + value);
                    }
                }
            }
            return value;
        }

        private static Object[] checkValuesForType(Property property, Object[] values) {
            for (int i = 0; i < values.length; i++) {
                values[i] = checkValueForType(property, values[i]);
            }
            return values;
        }

        public final Property property;
        public final String op;

        public PropertyCondition(Property property, String op) {
            this.property = property;
            this.op = op;
        }

        public PropertyCondition(Property property, String op, Object value) {
            super(checkValueForType(property, value));
            this.property = property;
            this.op = op;
        }

        public PropertyCondition(Property property, String op, Object[] values) {
            super(checkValuesForType(property, values));
            this.property = property;
            this.op = op;
        }

        @Override
        public void appendTo(StringBuilder builder, String tableAlias) {
            SqlUtils.appendProperty(builder, tableAlias, property).append(op);
        }
    }

    public static class StringCondition extends AbstractCondition {

        protected final String string;

        public StringCondition(String string) {
            this.string = string;
        }

        public StringCondition(String string, Object value) {
            super(value);
            this.string = string;
        }

        public StringCondition(String string, Object... values) {
            super(values);
            this.string = string;
        }

        @Override
        public void appendTo(StringBuilder builder, String tableAlias) {
            builder.append(string);
        }

    }
}

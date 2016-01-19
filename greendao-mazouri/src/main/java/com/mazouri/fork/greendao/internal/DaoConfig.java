package com.mazouri.fork.greendao.internal;

import android.database.sqlite.SQLiteDatabase;

import com.mazouri.fork.greendao.AbstractDao;
import com.mazouri.fork.greendao.DaoException;
import com.mazouri.fork.greendao.Property;
import com.mazouri.fork.greendao.identityscope.IdentityScope;
import com.mazouri.fork.greendao.identityscope.IdentityScopeLong;
import com.mazouri.fork.greendao.identityscope.IdentityScopeObject;
import com.mazouri.fork.greendao.identityscope.IdentityScopeType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal class used by greenDAO. DaoConfig stores essential data for DAOs, and is hold by AbstractDaoMaster. This
 * class will retrieve the required information from the DAO classes.
 *
 * DaoConfig存储了GreenDao所需要的必须的数据。同时，这个对象自身是由AbstractDaoMaster持有。这个类将会从Dao的类中获取所需要的信息
 *
 * Created by wangdong on 16-1-18.
 */
public class DaoConfig implements Cloneable {

    public final SQLiteDatabase db;
    public final String tablename;  //数据库的表的名称
    public final Property[] properties; //数据库的字段属性

    public final String[] allColumns;   //指定的表的所有的字段的名称
    public final String[] pkColumns;    //指定的表的主键的字段的名称的数组
    public final String[] nonPkColumns; //指定的表的非主键的字段的名称的数组

    /** Single property PK or null if there's no PK or a multi property PK. */
    public final Property pkProperty;
    public final boolean keyIsNumeric;
    public final TableStatements statements;

    private IdentityScope<?, ?> identityScope;  //实现了IdentityScope接口的对象，也就是实体对象的存储类

    /**
     * 构造函数，参数是SQliteDataBase与继承了AbstractDao的类的Class的对象。
     *  职责：1、初始化表名
     *      2、初始化表字段的数组（通过对参数中的Class用反射）
     *      3、初始化主键字段与非主键字段。（Property字段中维护了一个是否是主键的字段）
     * @param db
     * @param daoClass
     */
    public DaoConfig(SQLiteDatabase db, Class<? extends AbstractDao<?, ?>> daoClass) {
        this.db = db;
        try {
            this.tablename = (String) daoClass.getField("TABLENAME").get(null);
            Property[] properties = reflectProperties(daoClass);
            this.properties = properties;

            allColumns = new String[properties.length];

            List<String> pkColumnList = new ArrayList<String>();
            List<String> nonPkColumnList = new ArrayList<String>();
            Property lastPkProperty = null;
            for (int i = 0; i < properties.length; i++) {
                Property property = properties[i];
                String name = property.columnName;
                allColumns[i] = name;
                if (property.primaryKey) {
                    pkColumnList.add(name);
                    lastPkProperty = property;
                } else {
                    nonPkColumnList.add(name);
                }
            }
            String[] nonPkColumnsArray = new String[nonPkColumnList.size()];
            nonPkColumns = nonPkColumnList.toArray(nonPkColumnsArray);
            String[] pkColumnsArray = new String[pkColumnList.size()];
            pkColumns = pkColumnList.toArray(pkColumnsArray);

            pkProperty = pkColumns.length == 1 ? lastPkProperty : null;
            statements = new TableStatements(db, tablename, allColumns, pkColumns);

            if (pkProperty != null) {
                Class<?> type = pkProperty.type;
                keyIsNumeric = type.equals(long.class) || type.equals(Long.class) || type.equals(int.class)
                        || type.equals(Integer.class) || type.equals(short.class) || type.equals(Short.class)
                        || type.equals(byte.class) || type.equals(Byte.class);
            } else {
                keyIsNumeric = false;
            }

        } catch (Exception e) {
            throw new DaoException("Could not init DAOConfig", e);
        }
    }

    /**
     * 对指定的Class应用反射，内部有一个内部类Properties，本质上是对这个内部类应用反射
     *
     * @param daoClass
     * @return
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private static Property[] reflectProperties(Class<? extends AbstractDao<?, ?>> daoClass)
            throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException {
        Class<?> propertiesClass = Class.forName(daoClass.getName() + "$Properties");
        Field[] fields = propertiesClass.getDeclaredFields();

        ArrayList<Property> propertyList = new ArrayList<Property>();
        final int modifierMask = Modifier.STATIC | Modifier.PUBLIC;
        for (Field field : fields) {
            // There might be other fields introduced by some tools, just ignore them (see issue #28)
            if ((field.getModifiers() & modifierMask) == modifierMask) {
                Object fieldValue = field.get(null);
                if (fieldValue instanceof Property) {
                    propertyList.add((Property) fieldValue);
                }
            }
        }

        Property[] properties = new Property[propertyList.size()];
        for (Property property : propertyList) {
            if (properties[property.ordinal] != null) {
                throw new DaoException("Duplicate property ordinals");
            }
            properties[property.ordinal] = property;
        }
        return properties;
    }

    /**
     * Does not copy identity scope.
     *
     * 另外一个构造函数，参数是DaoConfig，做对象的拷贝，但是已经在指定的作用域的缓存对象不做拷贝
     */
    public DaoConfig(DaoConfig source) {
        db = source.db;
        tablename = source.tablename;
        properties = source.properties;
        allColumns = source.allColumns;
        pkColumns = source.pkColumns;
        nonPkColumns = source.nonPkColumns;
        pkProperty = source.pkProperty;
        statements = source.statements;
        keyIsNumeric = source.keyIsNumeric;
    }

    /** Does not copy identity scope. */
    @Override
    public DaoConfig clone() {
        return new DaoConfig(this);
    }

    public IdentityScope<?, ?> getIdentityScope() {
        return identityScope;
    }

    public void setIdentityScope(IdentityScope<?, ?> identityScope) {
        this.identityScope = identityScope;
    }

    /**
     * 初始化对象的存储的作用域，根据当前的表的主键是整数还是非整数，
     * 分别选择用IdentityScopeLong与IdentityScopeObject来进行对象在指定的会话的存储
     *
     * @param type
     */
    @SuppressWarnings("rawtypes")
    public void initIdentityScope(IdentityScopeType type) {
        if (type == IdentityScopeType.None) {
            identityScope = null;
        } else if (type == IdentityScopeType.Session) {
            if (keyIsNumeric) {
                identityScope = new IdentityScopeLong();
            } else {
                identityScope = new IdentityScopeObject();
            }
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
}

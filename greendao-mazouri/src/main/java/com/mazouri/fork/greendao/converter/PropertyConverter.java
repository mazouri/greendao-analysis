package com.mazouri.fork.greendao.converter;

/**
 * To use custom types in your entity, implement this to convert db values to entity values and back.
 * <p/>
 * Notes for implementations:
 * <ul>
 * <li>Converters are created by the default constructor</li>
 * <li>Converters must be implemented thread-safe</li>
 * </ul>
 *
 * 数据库的属性字段与Java对象相互映射的抽象的接口
 *
 * Created by wangdong on 16-1-18.
 */
public interface PropertyConverter<P, D> {
    //成员方法，参数是泛型，隶属于sql的属性字段，字面意思看是将数据库的字段映射为Java对象中的属性
    P convertToEntityProperty(D databaseValue);

    //成员方法，参数是泛型，隶属于Java对象的属性字段，字面意思是将Java对象的属性字段映射为数据库的属性字段
    D convertToDatabaseValue(P entityProperty);
}

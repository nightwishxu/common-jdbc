//package core.common.test;
//
//import core.common.jdbc.JdbcClassUtils;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import javax.persistence.Column;
//import javax.persistence.Id;
//
//class PropertyMapping {
//    final boolean insertable;
//    final boolean updatable;
//    final String columnName;
//    final boolean id;
//    final Method getter;
//    final Method setter;
//    final Class enumClass;
//    final String fieldName;
//
//    public PropertyMapping(Method getter, Method setter, Field field) {
//        this.getter = getter;
//        this.setter = setter;
//        this.enumClass = getter.getReturnType().isEnum() ? getter.getReturnType() : null;
//        Column column = (Column)field.getAnnotation(Column.class);
//        this.insertable = column == null || column.insertable();
//        this.updatable = column == null || column.updatable();
//        this.columnName = column == null ? JdbcClassUtils.getGetterName(getter) : ("".equals(column.name()) ? JdbcClassUtils.getGetterName(getter) : column.name());
//        this.id = field.isAnnotationPresent(Id.class);
//        this.fieldName = field.getName();
//    }
//
//    Object get(Object target) throws Exception {
//        Object r = this.getter.invoke(target);
//        return this.enumClass == null ? r : Enum.valueOf(this.enumClass, (String)r);
//    }
//
//    void set(Object target, Object value) throws Exception {
//        if (this.enumClass != null && value != null) {
//            value = Enum.valueOf(this.enumClass, (String)value);
//        }
//
//        try {
//            if (value != null) {
//                this.setter.invoke(target, this.setter.getParameterTypes()[0].cast(value));
//            }
//        } catch (Exception var4) {
//            var4.printStackTrace();
//            System.err.println(this.fieldName + "--" + value);
//        }
//
//    }
//}

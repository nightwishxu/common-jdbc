package core.common.jdbc;


import core.common.util.Page;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface IBaseDao<T extends Serializable, PK extends Serializable> {
    T get(PK var1);

    List<T> getAll();

    boolean saveOne(T var1);

    PK saveAndReturnId(T var1);

    int update(T var1);

    int saveAll(List<T> var1);

    int delete(Object var1);

    int deleteAll(List<T> var1);

    void deleteByPK(PK var1);

    T findUnique(String var1, Object var2);

    boolean exists(PK var1);

    long getCount(QueryRule var1);

    T getMax(String var1);

    List<T> find(QueryRule var1);

    Page<T> find(QueryRule var1, int var2, int var3);

    T findUnique(Map<String, Object> var1);

    T findUnique(QueryRule var1);

    Page<T> pagination(List<T> var1, int var2, int var3);

    void mergeList(List<Object> var1, List<Object> var2, String var3);

    void mergeList(List<Object> var1, List<Object> var2, String var3, boolean var4);
}

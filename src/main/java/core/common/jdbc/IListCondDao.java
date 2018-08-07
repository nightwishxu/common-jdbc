package core.common.jdbc;

import core.common.util.Page;

import java.util.List;

public interface IListCondDao<LongKV> extends IListDao {
    List<Long> pre(long var1, long var3, long var5, int var7);

    List<Long> getFirst(long var1, long var3, int var5);

    List<Long> next(long var1, long var3, long var5, int var7);

    Page getByPage(long var1, long var3, int var5, int var6);

    long getCount(long var1, long var3);

    long insert(long var1, long var3, long var5);

    boolean delete(long var1, long var3, long var5);

    boolean exist(long var1, long var3, long var5);

    void clearCache(long var1, long var3);

    long reload(long var1, long var3);

    int getNewNum(long var1, long var3, long var5);
}

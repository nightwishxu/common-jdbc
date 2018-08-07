package core.common.jdbc;

import java.util.Date;
import java.util.List;

import core.common.util.Page;
import org.springframework.jdbc.core.RowMapper;

public interface IListDao<LongKV> extends RowMapper<LongKV> {
    List<Long> pre(long var1, long var3, int var5);

    List<Long> getFirst(long var1, int var3);

    List<Long> next(long var1, long var3, int var5);

    Page getByPage(long var1, int var3, int var4);

    long getCount(long var1);

    void clearCache(long var1);

    long reload(long var1);

    long insert(long var1, long var3);

    long insert(List<LongKV> var1);

    long insert(long var1, long var3, Date var5);

    boolean delete(long var1, long var3);

    boolean exist(long var1, long var3);

    String getTable(long var1);

    int getCacheSize();

    int getNewNum(long var1, long var3);
}
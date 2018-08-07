package core.common.jdbc;

import core.common.util.HashGen;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class Shard<T> {
    private TreeMap<Long, T> nodes;
    private List<T> shards;
    private final int NODE_NUM = 100;

    public Shard(List<T> shards) {
        this.shards = shards;
        this.init();
    }

    private void init() {
        this.nodes = new TreeMap();

        for(int i = 0; i != this.shards.size(); ++i) {
            T shardInfo = this.shards.get(i);

            for(int n = 0; n < 100; ++n) {
                this.nodes.put(this.hash("SHARD-" + i + "-NODE-" + n), shardInfo);
            }
        }

    }

    public T getShardInfo(String key) {
        SortedMap<Long, T> tail = this.nodes.tailMap(this.hash(key));
        return tail.size() == 0 ? this.nodes.get(this.nodes.firstKey()) : tail.get(tail.firstKey());
    }

    private Long hash(String key) {
        return HashGen.hash(key);
    }

    public static void main() {
    }
}

package com.example.producers.partitioner;

import com.example.producers.PizzaProducer;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.internals.StickyPartitionCache;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class CustomPartitioner implements Partitioner {

    public static final Logger logger = LoggerFactory.getLogger(CustomPartitioner.class.getName());
    private String specialKeyName;
    // 무작위로 하나의 파티션을 "sticky하게" 선택
    private final StickyPartitionCache stickyPartitionCache = new StickyPartitionCache();

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        // keyBytes 나 valueBytes는 serialize된 값
        final List<PartitionInfo> partitionInfos = cluster.partitionsForTopic(topic); // 파티션 정보들을 반환
        int numPartitions = partitionInfos.size();
        // partition 5개 중에 2개를 특정 파티션에 보낼것이다.
        int numSpecialPartitions = numPartitions / 2;
        int partitionIdx = 0;

        if (key == null) stickyPartitionCache.partition(topic, cluster);
        if (key instanceof String keyStr) {
            if (keyStr.equals(specialKeyName)) partitionIdx = Utils.toPositive(Utils.murmur2(valueBytes)) % numSpecialPartitions; // 0,1
            else partitionIdx = Utils.toPositive(Utils.murmur2(keyBytes)) % (numPartitions-numSpecialPartitions) + numSpecialPartitions; // 2,3,4
        }
        logger.info("key: {}, is sent to partition: {}", key, partitionIdx);
        return partitionIdx;
    }

    @Override
    public void close() {

    }

    /**
     * Map에 들어오는 것은 producer의 config 정보
     */
    @Override
    public void configure(Map<String, ?> configs) {
        specialKeyName = (String) configs.get("custom.specialKey");
    }
}

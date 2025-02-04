package com.grafka.publishers

import com.grafka.entities.KafkaCluster
import com.grafka.entities.KafkaMessage
import com.grafka.resolvers.KafkaClusterQueryResolver
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.io.StringReader
import java.time.Instant
import java.util.*

@Component
class KafkaMessagePublisher(private val kafkaClusterQueryResolver: KafkaClusterQueryResolver) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun getPublisher(clusterId: String, topic: String) = getPublisher(
            kafkaClusterQueryResolver.clusters(clusterId).first(),
            topic
    )

    fun getPublisher(cluster: KafkaCluster, topic: String): Flux<KafkaMessage> {
        log.info("Getting publisher for ${cluster.clusterId}:${topic}")

        val config = getConfigurationFromCluster(cluster)

        val options = ReceiverOptions.create<String, GenericRecord>(config)
                .addAssignListener { partitions -> partitions.forEach { p -> p.seekToBeginning() } }
                .subscription(Collections.singleton(topic))

        return KafkaReceiver
                .create(options)
                .receive()
                .log()
                .map {
                    KafkaMessage(
                            it.topic(),
                            it.key(),
                            it.value().toString(),
                            it.partition(),
                            it.offset(),
                            it.timestampType().toString(),
                            it.timestamp(),
                            it.leaderEpoch().orElse(null)
                    )
                }
    }

    private fun getConfigurationFromCluster(cluster: KafkaCluster): Properties {
        val properties = Properties().apply {
            load(StringReader(cluster.config))
        }
        val appender = Instant.now().toEpochMilli()
        // TODO better way!
        properties["client.id"] = "${properties["client.id"]}-${appender}"
        properties["group.id"] = "${properties["group.id"]}-${appender}"
        return properties
    }
}
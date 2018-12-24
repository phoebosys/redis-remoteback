/*
 * Copyright Phoebosys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ys.phoebos.redis.replay.receiver

import com.moandjiezana.toml.Toml
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisCluster
import ys.phoebos.redis.*
import ys.phoebos.redis.protocol.Talk
import ys.phoebos.redis.replay.*
import ys.phoebos.redis.replay.client.Connector
import java.time.Duration
import java.util.regex.Pattern

class KafkaReceiver : Receiver {


    private lateinit var type: MessageType
    private lateinit var topic: Any
    private var pollTimeout: Long = POLL_TIMEOUT
    private lateinit var consumer: KafkaConsumer<*, *>
    private var stop = false

    override fun setConfig(type: MessageType, config: Toml) {
        this.type = type
        topic = config.toMap()[KAFKA_TOPIC_NAME]?:listOf(KAFKA_TOPIC)
        pollTimeout = config.getLong(POLL_TIMEOUT_NAME, POLL_TIMEOUT)

        val kafkaConfig = config.toMap()
            .filterKeys { it !in listOf(RECEIVER_CLASS, KAFKA_TOPIC_NAME, POLL_TIMEOUT_NAME) }
            .mapKeys { it.key.replace('-', '.') }

        when (type) {
            MessageType.PROTOCOL -> this.consumer = KafkaConsumer(kafkaConfig, ByteArrayDeserializer(), ByteArrayDeserializer())
            MessageType.STRING -> this.consumer = KafkaConsumer(kafkaConfig, StringDeserializer(), StringDeserializer())
            MessageType.ROW -> {
                this.consumer = KafkaConsumer(kafkaConfig, StringDeserializer(), StringDeserializer())
            }
        }
    }

    override fun open() {
        when (topic) {
            is String -> consumer.subscribe(Pattern.compile(topic as String))
            is List<*> -> consumer.subscribe(topic as List<String>)
        }
    }

    override fun close() {
        consumer.close()
    }

    override fun receive(): List<Any> {
        return consumer.poll(Duration.ofMillis(pollTimeout)).map { it.value() }
    }
}

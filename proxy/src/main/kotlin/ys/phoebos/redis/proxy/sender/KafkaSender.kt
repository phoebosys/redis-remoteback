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

package ys.phoebos.redis.proxy.sender

import ys.phoebos.redis.proxy.*
import com.moandjiezana.toml.Toml
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import ys.phoebos.redis.KAFKA_TOPIC
import ys.phoebos.redis.KAFKA_TOPIC_NAME
import ys.phoebos.redis.MessageType
import ys.phoebos.redis.protocol.Command
import ys.phoebos.redis.protocol.Talk

class KafkaSender : Sender {

    private lateinit var type: MessageType
    private lateinit var topic: String
    private lateinit var filter: List<String>
    private lateinit var producer: KafkaProducer<*, *>

    override fun setConfig(type: MessageType, config: Toml) {
        this.type = type
        this.filter = config.getList<String>(FILTER_NAME, FILTER)
        this.topic = config.getString(KAFKA_TOPIC_NAME, KAFKA_TOPIC)
        val kafkaConfig = config.toMap()
            .filterKeys { it !in listOf(FILTER_NAME, SENDER_CLASS, KAFKA_TOPIC_NAME) }
            .mapKeys { it.key.replace('-', '.') }

        when (type) {
            MessageType.PROTOCOL -> this.producer = KafkaProducer(kafkaConfig, ByteArraySerializer(), ByteArraySerializer())
            MessageType.STRING -> this.producer = KafkaProducer(kafkaConfig, StringSerializer(), StringSerializer())
            MessageType.ROW -> {
                this.producer = KafkaProducer(kafkaConfig, StringSerializer(), StringSerializer())
                this.filter = emptyList()
            }
        }
    }

    override fun send(talk: Talk) {
        when (type) {
            MessageType.PROTOCOL -> {
                if ((talk.command as Command).isNotEmpty() && (filter.isEmpty() || filter.contains((talk.command as Command).cmd))) {
                    val res = (producer as KafkaProducer<ByteArray, ByteArray>).send(ProducerRecord(topic, (talk.command as Command).toProtocol())).get()
                    LOG.debug("SEND -> ${String((talk.command as Command).toProtocol())} ${if(res.hasOffset()) "offset=${res.offset()}" else ""} size=${res.serializedValueSize()} pattern=${res.partition()}")
                } else
                    LOG.debug("SKIP -> ${String((talk.command as Command).toProtocol())}")
            }
            MessageType.STRING -> {
                if ((talk.command as Command).isNotEmpty() && (filter.isEmpty() || filter.contains((talk.command as Command).cmd))) {
                    val res = (producer as KafkaProducer<String, String>).send(ProducerRecord(topic, talk.command.toString())).get()
                    LOG.debug("SEND -> ${talk.command} ${if (res.hasOffset()) "offset=${res.offset()}" else ""} size=${res.serializedValueSize()} pattern=${res.partition()}")
                } else
                    LOG.debug("SKIP -> ${talk.command}")
            }
            MessageType.ROW -> {
                if ((talk.command as String).isNotEmpty()) {
                    if (filter.isEmpty() || filter.contains((talk.command as String).split(' ')[0])) {
                        val res =
                            (producer as KafkaProducer<String, String>).send(ProducerRecord(topic, talk.command as String)).get()
                        LOG.debug("SEND -> ${talk.command} ${if (res.hasOffset()) "offset=${res.offset()}" else ""} size=${res.serializedValueSize()} pattern=${res.partition()}")
                    } else
                        LOG.debug("SKIP -> ${talk.command}")
                } // else skip row reply
            }
        }
    }
}

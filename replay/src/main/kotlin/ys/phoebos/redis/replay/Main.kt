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

package ys.phoebos.redis.replay

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.getInt
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import ys.phoebos.redis.*
import ys.phoebos.redis.replay.worker.CoroutineWorker
import ys.phoebos.redis.replay.worker.RxWorker
import ys.phoebos.redis.replay.worker.ThreadWorker
import ys.phoebos.redis.replay.client.Connector
import ys.phoebos.redis.replay.receiver.Receiver
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress


@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
fun main(args: Array<String>) {

    val configFile = args.getOrElse(0){CONFIG}

    LoggerFactory::class.java.classLoader.getResource(configFile)?.file?.let {
        val file = File(it)
        if (!file.canRead())
            throw IOException(file.absolutePath + " can not read.")

        val config = Toml().read(file)
        val type = MessageType.valueOf(config.getString(MESSAGE_NAME, MESSAGE_TYPE))

        val receiverConfig: Toml = config.getTable(RECEIVER_NAME)
        val receiver = Class.forName(receiverConfig.getString(RECEIVER_CLASS)!!).newInstance() as Receiver
        receiver.setConfig(type, receiverConfig)
        receiver.open()

        val redisConfig = config.getTable(REDIS_NAME)
        val connector = Connector(InetSocketAddress(redisConfig.getString(CONN_IP_NAME, CONN_IP),
            redisConfig.getInt(CONN_PORT_NAME, CONN_PORT)))

        val logger = LoggerFactory.getLogger(config.getString(LOG_NAME_NAME, LOG_NAME))
        val level = config.getString(LOG_LEVEL_NAME, LOG_LEVEL)
        val log: (String) -> Unit = when (level.toLowerCase()) {
            "trace" -> logger::trace
            "debug" -> logger::debug
            "info" -> logger::info
            "warn" -> logger::warn
            else -> logger::error
        }

        val rsptimeout = config.getLong(RSP_TIMEOUT_NAME, RSP_TIMEOUT)
        when(config.getString(WORKER_NAME, WORKER_TYPE)) {
            "coroutine" -> CoroutineWorker(type, receiver, connector, log, rsptimeout).start()
            "rx" -> RxWorker(type, receiver, connector, log).start()
            else -> ThreadWorker(type, receiver, connector, log).start()
        }

        while(true) {
            try {
                Thread.sleep(1000)
            } catch (e: Exception) {
            }
        }
    } ?: throw IOException("$configFile not found.")
}

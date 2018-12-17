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

package ys.phoebos.redis.proxy

import ys.phoebos.redis.proxy.sender.RxSender
import ys.phoebos.redis.proxy.worker.CoroutineWorker
import ys.phoebos.redis.proxy.worker.RxWorker
import ys.phoebos.redis.proxy.worker.ThreadWorker
import com.moandjiezana.toml.Toml
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException


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

        val senderList: List<Toml> = config.getTables(SENDER_NAME)
        val senders: List<RxSender> = senderList.map { cfg -> Class.forName(cfg.getString(SENDER_CLASS)!!).newInstance() as RxSender }
        senders.zip(senderList){sender, cfg -> sender.setConfig(type, cfg)}

        val (reqSource, rspSource) = Proxy(config).start()

        val rsptimeout = config.getLong(RSP_TIMEOUT_NAME, RSP_TIMEOUT)
        when(config.getString(WORKER_NAME, WORKER_TYPE)) {
            "coroutine" -> CoroutineWorker(type, senders, reqSource, rspSource, rsptimeout).start()
            "rx" -> RxWorker(type, senders, reqSource, rspSource).start()
            else -> ThreadWorker(type, senders, reqSource, rspSource).start()
        }

    } ?: throw IOException("$configFile not found.")
}

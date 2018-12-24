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

package ys.phoebos.redis.proxy.worker

import ys.phoebos.redis.CHARSET
import ys.phoebos.redis.MessageType
import ys.phoebos.redis.protocol.*
import ys.phoebos.redis.proxy.LOG
import ys.phoebos.redis.proxy.sender.Sender
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class ThreadWorker(
    private val type: MessageType,
    private val senders: List<Sender>,
    reqIn: InputStream,
    rspIn: InputStream
) {
    private var reqFlow: Readiable
    private var rspFlow: Readiable
    init {
        when(type) {
            MessageType.PROTOCOL, MessageType.STRING -> {
                reqFlow = CommandInputStream(reqIn)
                rspFlow = ReplyInputStream(rspIn)
            }
            MessageType.ROW -> {
                reqFlow = ReadiableReader(InputStreamReader(reqIn, CHARSET))
                rspFlow = ReadiableReader(InputStreamReader(rspIn, CHARSET))
            }
        }
    }

    fun start() {
        val ns = ""
        Thread {
            while (true) {
                try {
                    when (type) {
                        MessageType.PROTOCOL, MessageType.STRING -> (reqFlow as CommandInputStream).readCommand().let {
                            senders.forEach { sender -> sender.send(Talk(it, emptyReply)) }
                        }
                        MessageType.ROW -> (reqFlow as ReadiableReader).readLine().let {
                            senders.forEach { sender -> sender.send(Talk(it, ns)) }
                        }
                    }
                } catch (e: IOException) {
                    LOG.error("ThreadWorker.req", e)
                }
            }
        }.start()
        Thread {
            while (true) {
                try {
                    when (type) {
                        MessageType.PROTOCOL, MessageType.STRING -> (rspFlow as ReplyInputStream).readReply().let {
                            senders.forEach { sender -> sender.send(Talk(emptyCommand, it)) }
                        }
                        MessageType.ROW -> (rspFlow as ReadiableReader).readLine().let {
                            senders.forEach { sender -> sender.send(Talk(ns, it)) }
                        }
                    }
                } catch (e: IOException) {
                    LOG.error("ThreadWorker.rsp", e)
                }
            }
        }.start()
    }
}

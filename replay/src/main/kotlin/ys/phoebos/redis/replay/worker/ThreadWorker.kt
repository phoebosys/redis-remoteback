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

package ys.phoebos.redis.replay.worker

import ys.phoebos.redis.MessageType
import ys.phoebos.redis.replay.LOG
import ys.phoebos.redis.replay.client.Connector
import ys.phoebos.redis.replay.receiver.Receiver
import java.io.*

class ThreadWorker(
    private val type: MessageType,
    private val receiver: Receiver,
    private val connector: Connector,
    private val log: (String) -> Unit
) {
    fun start() {
        Thread {
            when (type) {
                MessageType.PROTOCOL -> {
                    while (true) {
                        try {
                            receiver.receive().forEach {
                                log("==> ${String((it as ByteArray))}")
                                connector.send(it)
                            }
                        } catch (e: IOException) {
                            LOG.error("ThreadWorker.replaySend", e)
                        }
                    }
                }
                MessageType.STRING, MessageType.ROW -> {
                    while (true) {
                        try {
                            receiver.receive().forEach {
                                log("==> $it")
                                connector.sendRow(it as String)
                            }
                        } catch (e: IOException) {
                            LOG.error("ThreadWorker.replaySend", e)
                        }
                    }
                }
            }
        }.start()

        Thread {
            when (type) {
                MessageType.PROTOCOL -> {
                    while (true) {
                        try {
                            val reply = connector.receive()
                                log("<== ${String(reply.toProtocol())}")
                        } catch (e: IOException) {
                            LOG.error("ThreadWorker.replayReceive", e)
                        }
                    }
                }
                MessageType.STRING -> {
                    while (true) {
                        try {
                            val reply = connector.receive()
                            log("<== $reply")
                        } catch (e: IOException) {
                            LOG.error("ThreadWorker.replayReceive", e)
                        }
                    }
                }
                MessageType.ROW -> {
                    while (true) {
                        try {
                            val rec = connector.receiveRow()
                            log("<== $rec")
                        } catch (e: IOException) {
                            LOG.error("ThreadWorker.replayReceive", e)
                        }
                    }
                }
            }
        }.start()
    }
}

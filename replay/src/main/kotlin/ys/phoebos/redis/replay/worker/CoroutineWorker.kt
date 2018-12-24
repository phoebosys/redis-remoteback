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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.selects.select
import ys.phoebos.redis.MessageType
import ys.phoebos.redis.replay.LOG
import ys.phoebos.redis.replay.client.Connector
import ys.phoebos.redis.replay.receiver.Receiver
import java.lang.Exception

@ExperimentalCoroutinesApi
class CoroutineWorker(
    private val type: MessageType,
    private val receiver: Receiver,
    private val connector: Connector,
    private val log: (String) -> Unit,
    private val rsptimeout: Long
) {
    private fun GlobalScope.reqProtocol(receiver: Receiver, connector: Connector) = produce<String> {
        while(true) {
            receiver.receive().forEach {
                try {
                    when (type) {
                        MessageType.PROTOCOL -> {
                            connector.send(it as ByteArray)
                            send(String(it))
                        }
                        else -> {   // only MessageType.STRING
                            connector.send(it as String)
                            send(it)
                        }
                    }
                } catch (e: Exception) {
                    LOG.error("CoroutineWorker.replayReqProtocol", e)
                }
            }
        }
    }

    private fun GlobalScope.rspProtocol(connector: Connector) = produce {
        while(true) {
            try {
                send(when (type) {
                        MessageType.PROTOCOL -> String(connector.receive().toProtocol())
                        else -> connector.receive().toString()
                })
            } catch (e: Exception) {
                LOG.error("CoroutineWorker.replayRspProtocol", e)
            }
        }
    }

    private fun GlobalScope.reqRow(receiver: Receiver, connector: Connector) = produce<String> {
        while(true) {
            receiver.receive().forEach {
                try {
                    connector.sendRow(it as String)
                    send(it)
                } catch (e: Exception) {
                    LOG.error("CoroutineWorker.replayReqRow", e)
                }
            }
        }
    }

    private fun GlobalScope.rspRow(connector: Connector) = produce {
        while(true) {
            try {
                send(connector.receiveRow())
            } catch (e: Exception) {
                LOG.error("CoroutineWorker.replayRspRow", e)
            }
        }
    }

    private suspend fun selectRow(reqRow: ReceiveChannel<String>, rspRow: ReceiveChannel<String>) {
        select<Unit> {
            reqRow.onReceive { value -> log("==> $value") }
            rspRow.onReceive { value -> log("<== $value") }
        }
    }

    fun start()= runBlocking {
        when (type) {
            MessageType.PROTOCOL, MessageType.STRING -> {
                val job = GlobalScope.launch {
                    val req = GlobalScope.reqProtocol(receiver, connector)
                    val rsp = GlobalScope.rspProtocol(connector)
                    while (true) {
                        val cmd = req.receive()
                        val reply = withTimeoutOrNull(rsptimeout) {
                            rsp.receive()
                        }
                        log("==> $cmd\n<==$reply")
                    }
                }
                job.join()
            }
            MessageType.ROW -> {
                val req = GlobalScope.reqRow(receiver, connector)
                val rsp = GlobalScope.rspRow(connector)
                while (true) {
                    selectRow(req, rsp)
                }
            }
        }
    }
}

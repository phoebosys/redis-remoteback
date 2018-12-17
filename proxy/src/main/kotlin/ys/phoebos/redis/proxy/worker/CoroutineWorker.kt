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

import ys.phoebos.redis.proxy.*
import ys.phoebos.redis.proxy.protocol.*
import ys.phoebos.redis.proxy.sender.RxSender
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.selects.select
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception

@ExperimentalCoroutinesApi
class CoroutineWorker(
    private val type: MessageType,
    private val senders: List<RxSender>,
    reqIn: InputStream,
    rspIn: InputStream,
    private val rsptimeout: Long
) {
    private var reqFlow: Readiable
    private var rspFlow: Readiable
    private val ns = ""

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

    private fun GlobalScope.reqProtocol(reqFlow: CommandInputStream) = produce {
        while(true) {
            try {
                send(reqFlow.readCommand())
            } catch (e: Exception) {
                LOG.error("CoroutineWorker.reqProtocol", e)
            }
        }
    }

    private fun GlobalScope.rspProtocol(rspFlow: ReplyInputStream) = produce {
        while(true) {
            try {
                send(rspFlow.readReply())
            } catch (e: Exception) {
                LOG.error("CoroutineWorker.rspProtocol", e)
            }
        }
    }

    private fun GlobalScope.reqRow(reader: ReadiableReader) = produce<String> {
        for (line in reader.lines()) {
            try {
                send(line)
            } catch (e: Exception) {
                LOG.error("CoroutineWorker.reqRow", e)
            }
        }
    }

    private fun GlobalScope.rspRow(reader: ReadiableReader) = produce<String> {
        for (line in reader.lines()) {
            try {
                send(line)
            } catch (e: Exception) {
                LOG.error("CoroutineWorker.rspRow", e)
            }
        }
    }

    private suspend fun selectRow(reqRow: ReceiveChannel<String>, rspRow: ReceiveChannel<String>) {
        select<Unit> {
            reqRow.onReceive { value ->
                senders.forEach { sender -> sender.send(Talk(value, ns)) }
            }
            rspRow.onReceive { value ->
                senders.forEach { sender -> sender.send(Talk(ns, value)) }
            }
        }
    }

    fun start()= runBlocking {
        when (type) {
            MessageType.PROTOCOL, MessageType.STRING -> {
                GlobalScope.launch {
                    val req = GlobalScope.reqProtocol(reqFlow as CommandInputStream)
                    val rsp = GlobalScope.rspProtocol(rspFlow as ReplyInputStream)
                    while (true) {
                        val cmd = req.receive()
                        val reply = withTimeoutOrNull(rsptimeout) {
                            rsp.receive()
                        }
                        senders.forEach { sender -> sender.send(Talk(cmd, reply ?: emptyReply)) }
                    }
                }
            }
            MessageType.ROW -> {
                val req = GlobalScope.reqRow(reqFlow as ReadiableReader)
                val rsp = GlobalScope.rspRow(rspFlow as ReadiableReader)
                while (true) {
                    selectRow(req, rsp)
                }
            }
        }
    }
}

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

import ys.phoebos.redis.proxy.LOG
import ys.phoebos.redis.proxy.sender.RxSender
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import ys.phoebos.redis.CHARSET
import ys.phoebos.redis.MessageType
import ys.phoebos.redis.protocol.*
import java.io.InputStream
import java.io.InputStreamReader

class RxWorker(
    private val type: MessageType,
    private val senders: List<RxSender>,
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
        val commandFlow = Flowable.create(FlowableOnSubscribe<Talk> { emitter ->
            while (!emitter.isCancelled) {
                try {
                    val command = when (type) {
                        MessageType.PROTOCOL, MessageType.STRING ->
                            Talk((reqFlow as CommandInputStream).readCommand(), emptyReply)
                        MessageType.ROW -> Talk((reqFlow as ReadiableReader).readLine(), ns)
                    }
                    while (emitter.requested() == 0L) {
                        if (emitter.isCancelled) {
                            break
                        }
                    }
                    emitter.onNext(command)
                } catch (e: Exception) {
                    LOG.error("RxWorker.commandFlow", e)
                    emitter.onError(e)
                }
            }
            reqFlow.close()
            emitter.onComplete()
        }, BackpressureStrategy.ERROR)
            .subscribeOn(Schedulers.io())

        val replyFlow = Flowable.create(FlowableOnSubscribe<Talk> {emitter ->
            while (!emitter.isCancelled) {
                try {
                    val reply = when (type) {
                        MessageType.PROTOCOL, MessageType.STRING -> Talk(emptyCommand, (rspFlow as ReplyInputStream).readReply())
                        MessageType.ROW -> Talk(ns, (rspFlow as ReadiableReader).readLine())
                    }
                    while (emitter.requested() == 0L) {
                        if (emitter.isCancelled) {
                            break
                        }
                    }
                    emitter.onNext(reply)
                }catch (e: Exception) {
                    LOG.error("RxWorker.commandFlow", e)
                    emitter.onError(e)
                }
            }
            rspFlow.close()
            emitter.onComplete()
        }, BackpressureStrategy.ERROR)
            .subscribeOn(Schedulers.io())//.observeOn(Schedulers.newThread())

        var join = when(type) {
            MessageType.PROTOCOL, MessageType.STRING -> {
                Flowable.zip(commandFlow, replyFlow,
                    BiFunction<Talk, Talk, Talk> { first, second -> Talk(first.command, second.reply) })
//                    .subscribeOn(Schedulers.newThread())
//                    .observeOn(Schedulers.newThread())
            }
            MessageType.ROW -> {
                Flowable.mergeDelayError(commandFlow, replyFlow)
//                    .subscribeOn(Schedulers.newThread())
//                    .observeOn(Schedulers.newThread())
            }
        }
        senders.forEach { join.subscribe(it) }
    }
}

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

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ys.phoebos.redis.MessageType
import ys.phoebos.redis.protocol.*
import ys.phoebos.redis.replay.LOG
import ys.phoebos.redis.replay.client.Connector
import ys.phoebos.redis.replay.receiver.Receiver

class RxWorker(
    private val type: MessageType,
    private val receiver: Receiver,
    private val connector: Connector,
    private val log: (String) -> Unit
) {
    fun start() {
        val commandFlow = Flowable.create(FlowableOnSubscribe<String> { emitter ->
            while (!emitter.isCancelled) {
                try {
                    val command = receiver.receive()
                    while (emitter.requested() == 0L) {
                        if (emitter.isCancelled) {
                            break
                        }
                    }
                    command.forEach {
                        when (type) {
                            MessageType.PROTOCOL -> {
                                connector.send(it as ByteArray)
                                emitter.onNext("$==> ${String(it)}")
                            }
                            MessageType.STRING -> {
                                connector.send(it as String)
                                emitter.onNext("$==> $it")
                            }
                            MessageType.ROW -> {
                                connector.sendRow(it as String)
                                emitter.onNext("$==> $it")
                            }
                        }
                    }
                } catch (e: Exception) {
                    LOG.error("RxWorker.replayCommand", e)
                    emitter.onError(e)
                }
            }
            receiver.close()
            emitter.onComplete()
        }, BackpressureStrategy.ERROR)
            .subscribeOn(Schedulers.io())

        val replyFlow = Flowable.create(FlowableOnSubscribe<String> {emitter ->
            while (!emitter.isCancelled) {
                try {
                    val reply = when (type) {
                        MessageType.PROTOCOL -> String(connector.receive().toProtocol())
                        MessageType.STRING -> connector.receive().toString()
                        MessageType.ROW -> connector.receiveRow()
                    }
                    while (emitter.requested() == 0L) {
                        if (emitter.isCancelled) {
                            break
                        }
                    }
                    emitter.onNext("<== $reply")
                }catch (e: Exception) {
                    LOG.error("RxWorker.replayReply", e)
                    emitter.onError(e)
                }
            }
            receiver.close()
            emitter.onComplete()
        }, BackpressureStrategy.ERROR)
            .subscribeOn(Schedulers.io())//.observeOn(Schedulers.newThread())

        var join = when(type) {
            MessageType.PROTOCOL, MessageType.STRING -> {
                Flowable.zip(commandFlow, replyFlow,
                    BiFunction<String, String, String> { command, reply -> "$command\n$reply" })
//                    .subscribeOn(Schedulers.newThread())
//                    .observeOn(Schedulers.newThread())
            }
            MessageType.ROW -> {
                Flowable.mergeDelayError(commandFlow, replyFlow)
//                    .subscribeOn(Schedulers.newThread())
//                    .observeOn(Schedulers.newThread())
            }
        }

        join.subscribe(RxLogger(type, log))
    }
}

class RxLogger(val type: MessageType, val log: (String) -> Unit) : Subscriber<String> {
    private var subscription: Subscription? = null

    override fun onSubscribe(s: Subscription) {
        subscription = s
        s.request(1)
    }

    override fun onNext(str: String) {
        log(str)
        subscription!!.request(1)
    }

    override fun onError(t: Throwable) {
        LOG.debug("RxLogger onError", t)
    }

    override fun onComplete() {
        LOG.debug("RxLogger onComplete")
    }
}

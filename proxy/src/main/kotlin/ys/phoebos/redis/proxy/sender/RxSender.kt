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

import ys.phoebos.redis.proxy.LOG
import ys.phoebos.redis.proxy.MessageType
import ys.phoebos.redis.proxy.protocol.Talk
import com.moandjiezana.toml.Toml
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

abstract class RxSender : Subscriber<Talk> {
    private var subscription: Subscription? = null

    abstract fun setConfig(type: MessageType, config: Toml)

    abstract fun send(talk: Talk)

    override fun onSubscribe(s: Subscription) {
        subscription = s
        s.request(100)
    }

    override fun onNext(talk: Talk) {
        send(talk)
        subscription!!.request(100)

    }

    override fun onError(t: Throwable) {
        LOG.debug("RxSender onError", t)
    }

    override fun onComplete() {
    }
}

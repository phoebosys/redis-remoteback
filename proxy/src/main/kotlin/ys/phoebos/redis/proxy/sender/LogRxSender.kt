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

import com.moandjiezana.toml.Toml
import org.slf4j.LoggerFactory
import ys.phoebos.redis.*
import ys.phoebos.redis.protocol.Command
import ys.phoebos.redis.protocol.Reply
import ys.phoebos.redis.protocol.Talk


class LogRxSender : RxSender() {

    private lateinit var type: MessageType
    private lateinit var log: (String) -> Unit

    override fun setConfig(type: MessageType, config: Toml) {
        this.type = type
        val logger = LoggerFactory.getLogger(config.getString(LOG_NAME_NAME, LOG_NAME))
        val level = config.getString(LOG_LEVEL_NAME, LOG_LEVEL)
        log = when (level.toLowerCase()) {
            "trace" -> logger::trace
            "debug" -> logger::debug
            "info" -> logger::info
            "warn" -> logger::warn
            else -> logger::error
        }
    }

    override fun send(talk: Talk) {
        when (type) {
            MessageType.PROTOCOL -> {
                if ((talk.command as Command).isNotEmpty())
                    log("--> ${String((talk.command as Command).toProtocol())}")
                if ((talk.reply as Reply).isNotEmpty())
                    log("<-- ${String((talk.reply as Reply).toProtocol())}")
            }
            MessageType.STRING -> {
                if ((talk.command as Command).isNotEmpty())
                    log("--> ${talk.command}")
                if ((talk.reply as Reply).isNotEmpty())
                    log("<-- ${talk.reply}")
            }
            MessageType.ROW -> {
                if ((talk.command as String).isNotEmpty())
                    log("--> ${talk.command}")
                if ((talk.reply as String).isNotEmpty())
                    log("<-- ${talk.reply}")
            }
        }
    }
}

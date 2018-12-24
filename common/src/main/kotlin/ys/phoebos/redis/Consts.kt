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

package ys.phoebos.redis

const val CONFIG = "config.toml"

const val CONN_IP_NAME = "connip"
const val CONN_IP = "127.0.0.1"

const val CONN_PORT_NAME = "connport"
const val CONN_PORT = 6379

const val MESSAGE_NAME = "message"
const val MESSAGE_TYPE = "PROTOCOL"

const val KAFKA_TOPIC_NAME = "topic"
const val KAFKA_TOPIC = "redis"

const val WORKER_NAME = "worker"
const val WORKER_TYPE = "coroutine"

const val REDIS_NAME = "redis"

const val RSP_TIMEOUT_NAME = "rsptimeout"
const val RSP_TIMEOUT = 100L

const val CRLF = "\r\n"

const val LOG_NAME_NAME = "logger"
const val LOG_NAME = "redis"
const val LOG_LEVEL_NAME = "level"
const val LOG_LEVEL = "info"


val CHARSET = Charsets.UTF_8
val nil = object{}


enum class MessageType {
    PROTOCOL,
    STRING,
    ROW
}

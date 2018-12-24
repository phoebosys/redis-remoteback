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

import org.slf4j.LoggerFactory

const val BIND_IP_NAME = "bindip"
const val BIND_IP = "127.0.0.1"

const val BIND_PORT_NAME = "bindport"
const val BIND_PORT = 6666

const val SENDER_NAME = "sender"

const val SENDER_CLASS = "class"

const val FILTER_NAME = "filter"
val FILTER = emptyList<String>()

val LOG = LoggerFactory.getLogger("ys.phoebosys.redis.proxy")!!

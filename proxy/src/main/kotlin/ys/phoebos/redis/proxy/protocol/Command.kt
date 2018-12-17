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

package ys.phoebos.redis.proxy.protocol

import ys.phoebos.redis.proxy.CHARSET
import ys.phoebos.redis.proxy.CRLF
import org.apache.commons.text.StringEscapeUtils.ESCAPE_JAVA
import java.lang.StringBuilder

val emptyCommand: Command = Command("", emptyList())

class Command(val cmd: String, private val args: List<String>): Protocolable {
    override fun isNotEmpty(): Boolean {
        return cmd.isNotEmpty()
    }

    override fun toProtocol(): ByteArray {
        val sb = StringBuilder("*").append(args.size + 1).append(CRLF)
            .append('$').append(cmd.length).append(CRLF).append(cmd).append(CRLF)
        args.forEach { sb.append('$').append(it.length).append(CRLF).append(it).append(CRLF) }
        return sb.toString().toByteArray(CHARSET)
    }

    override fun toString(): String {
        return if (args.isEmpty()) this.cmd
            else args.joinTo(StringBuilder(cmd).append(' '), separator = "\" \"", prefix = "\"", postfix = "\""
            ) { ESCAPE_JAVA.translate(it) }.toString()
    }

//    companion object: RedisStream<Command> {
//        override fun write(outs: OutputStream, t: Command) {
//            outs.write(t.toProtocol())
//        }
//
//        override fun read(ins: InputStream): Command? {
//            return ProtocolInputStream(ins).readCommand()
//        }
//
//    }
}

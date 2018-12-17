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
import ys.phoebos.redis.proxy.nil
import org.apache.commons.text.StringEscapeUtils.ESCAPE_JAVA
import java.io.IOException
import java.lang.StringBuilder

val emptyReply: Reply = Reply(nil)


class Reply(private val res: Any): Protocolable {
    override fun isNotEmpty(): Boolean {
        return res != nil
    }

    override fun toProtocol(): ByteArray {
        return toProtocol(res).toByteArray(CHARSET)
    }

    private fun toProtocol(res: Any): String {
        return when (res) {
            is Array<*> -> {arrayToProtocol(res as Array<Any>)}
            is ByteArray ->"\$${res.size}$CRLF${String(res)}$CRLF"
            is Long -> ":$res$CRLF"
            is Status -> "+$res$CRLF"
            is Fault -> "-$res$CRLF"
            nil -> "$-1$CRLF"
            else -> throw IOException("unknown reply type $res: ${res.javaClass.name}")
        }
    }

    private fun arrayToProtocol(res: Array<Any>): String {
        val sb = StringBuilder("*${res.size}$CRLF")
        res.joinTo(buffer = sb, separator = "") {
            toProtocol(it)
        }
        return sb.toString()
    }

    override fun toString(): String {
        return toString(res)
    }

    private fun toString(res: Any): String {
        return when (res) {
            is Array<*> -> arrayToString(res as Array<Any>)
            is ByteArray -> "\"${ESCAPE_JAVA.translate(String(res))}\""
            is Long -> res.toString()
            is Status -> res.toString()
            is Fault -> res.toString()
            nil -> "(nil)"
            else -> throw IOException("unknown reply type $res: ${res.javaClass.name}")
        }
    }

    private fun arrayToString(res: Array<Any>): String {
        val multiLine = res.filter { it is Array<*> }.map {
            if ((it as Array<*>).any { i -> i is Array<*> }) 1 else 0
        }.sum() > 0
        val sb = StringBuilder("*").append(res.size).append(if (multiLine) CRLF else ' ')
        res.joinTo(buffer = sb, separator = if (multiLine) CRLF else " ") {
            toString(it)
        }
        return sb.toString()
    }

//    companion object : RedisStream<Reply> {
//        override fun write(outs: OutputStream, t: Reply) {
//            outs.write(t.toProtocol())
//        }
//
//        override fun read(ins: InputStream): Reply? {
//            return ProtocolInputStream(ins).readReply()
//        }
//    }
}

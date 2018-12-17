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

import ys.phoebos.redis.proxy.nil
import com.carrotsearch.hppc.ByteArrayList
import java.io.Closeable
import java.io.EOFException
import java.io.InputStream
import javax.activation.UnsupportedDataTypeException

class CommandInputStream(input: InputStream) : ProtocolInputStream(input) {
    fun readCommand(): Command {
        val msg = readMessage()
        return when (msg) {
            is Array<*> -> {
                if (msg.size < 1)
                    throw UnsupportedDataTypeException()
                if (msg.all { it is ByteArray }) {
                    Command(String(msg[0] as ByteArray), msg.sliceArray(1 until msg.size).map { String(it as ByteArray) })
                } else {
                    throw UnsupportedDataTypeException()
                }
            }
            is ByteArray -> {
                Command(String(msg), emptyList())
            }
            else -> {
                throw UnsupportedDataTypeException()
            }
        }
    }
}

class ReplyInputStream(input: InputStream) : ProtocolInputStream(input) {
    fun readReply(): Reply {
        val msg = readMessage()
        return when (msg) {
            is Array<*> -> {
                Reply(msg as Array<Any>) //TODO warning
            }
            else -> {
                Reply(msg)
            }
        }
    }
}

sealed class ProtocolInputStream(input: InputStream): Closeable, Readiable {

    private val ins = ViewableInputStream(input)
    private val debug = false

    override fun ready(): Boolean {
        return ins.available() > 0
    }

    protected fun readMessage(): Any {
        val ch = readChar()
        when (ch) {
            '*' -> {
                val num = readUIntEOL()
                val array = arrayOfNulls<Any>(num)
                for (i in 0 until num) {
                    array[i] = readMessage()
                }
                return array
            }
            '$' -> {
                val num = readIntEOL()
                if (num < 0)
                    return nil
                return readByteArrayEOL(num)
            }
            ':' -> {
                return readLongEOL()
            }
            '+' -> {
                return Status(readByteArrayEOL())
            }
            '-' -> {
                return Fault(readByteArrayEOL())
            }
            else -> {
                throw UnsupportedDataTypeException("[${ins.ofs}]=${ch.toInt()} : ${String(ins.viewClear())}")
            }
        }
    }

    private fun readByte(): Byte {
        when (val ch = ins.read()) {
            -1 -> throw EOFException()
            else -> return ch.toByte()
        }
    }

    private fun readChar(): Char {
        return readByte().toChar()
    }


    private fun readDigits(): String {
        val sb = StringBuilder()
        while (true) {
            when (val ch = readChar()) {
                in '0'..'9', '-' -> {
                    sb.append(ch)
                }
                else -> {
                    ins.unread(ch.toInt() and 0xFF)
                    if (sb.isEmpty())
                        throw UnsupportedDataTypeException("[${ins.ofs}]=${ch.toInt()} : ${String(ins.viewClear())}")
                    return sb.toString()
                }
            }
        }
    }

    private fun readInt(): Int {
        var digit = readDigits().toInt()
        return digit
    }

    private fun readUInt(): Int {
        var digit = readDigits().toInt()
        if (digit < 0)
            throw NumberFormatException()
        return digit
    }

    private fun readIntEOL(): Int {
        val digit = readInt()
        readEOL()
        if(debug) print("DEBUG: ${String(ins.viewClear())}")
        return digit
    }

    private fun readUIntEOL(): Int {
        val digit = readUInt()
        readEOL()
        if(debug) print("DEBUG: ${String(ins.viewClear())}")
        return digit
    }

    private fun readLong(): Long {
        var digits = readDigits()
        return digits.toLong()
    }

    private fun readLongEOL(): Long {
        val digit = readLong()
        readEOL()
        if(debug) print("DEBUG: ${String(ins.viewClear())}")
        return digit
    }

    private fun readByteArray(num: Int): ByteArray {
        val ba = ByteArray(num)
        for (i in 0 until num) {
            ba[i] = readByte()
        }
        return ba
    }

    fun readByteArrayEOL(num: Int): ByteArray {
        val ba = readByteArray(num)
        readEOL()
        if(debug) print("DEBUG: ${String(ins.viewClear())}")
        return ba
    }

    private fun readByteArrayEOL(): ByteArray {
        val ba = ByteArrayList()
        while (true) {
            val b = readByte()
            if (b.toChar() != '\r')
                ba.add(b)
            else {
                val lf = readByte()
                if (lf.toChar() != '\n')
                    throw UnsupportedDataTypeException()
                if(debug) print("DEBUG: ${String(ins.viewClear())}")
                return ba.toArray()
            }
        }
    }

    private fun readEOL() {
        if (readChar() != '\r' || readChar() != '\n')
            throw UnsupportedDataTypeException("need \\r\\n : [${ins.ofs}] ${String(ins.viewClear())}")
    }

    override fun close() {
        ins.close()
    }
}
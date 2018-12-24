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

package ys.phoebos.redis.protocol

import com.carrotsearch.hppc.ByteArrayList
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream

class ViewableInputStream(ins: InputStream, size: Int) : PushbackInputStream(ins, size) {
    val maxSize = 256
    var buffer = ByteArrayList()
    var ofs = 0

    constructor(ins: InputStream) : this(ins, 1)

    fun position() = ofs

    fun clear() {
        buffer.clear()
    }

    fun view(): ByteArray {
        return buffer.toArray()
    }

    fun viewClear(): ByteArray {
        val ba = buffer.toArray()
        buffer.clear()
        return ba
    }

    @Throws(IOException::class)
    override fun read(): Int {
        val i = super.read()
        if (i != -1) {
            if (buffer.size() + 1 > maxSize)
                buffer.clear()
            buffer.add(i.toByte())
            ofs++
        }
        return i
    }

    @Throws(IOException::class)//read(byte[] b, int off, int len)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val l = super.read(b, off, len)
        if (l != -1) {
            if (buffer.size() + l > maxSize)
                buffer.clear()
            for (n in off until off + l) {
                buffer.add(b[n])
            }
            ofs += l
        }
        return l
    }

    @Throws(IOException::class)
    override fun unread(b: Int) {
        super.unread(b)
        buffer.resize(kotlin.math.max(0, buffer.size()-1))
        //buffer.dropLast(kotlin.math.min(buffer.size, 1))
        ofs--
    }

    @Throws(IOException::class)
    override fun unread(b: ByteArray, off: Int, len: Int) {
        super.unread(b, off, len)
        buffer.resize(kotlin.math.max(0, buffer.size()-len))
        //buffer.dropLast(kotlin.math.min(buffer.size, len))
        ofs -= len
    }

    @Throws(IOException::class)
    override fun unread(b: ByteArray) {
        super.unread(b)
        buffer.resize(kotlin.math.max(0,buffer.size()-b.size))
        //buffer.dropLast(kotlin.math.min(buffer.size, b.size))
        ofs -= b.size
    }

    @Throws(IOException::class)
    override fun close() {
        buffer.clear()
    }
}

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

import org.netcrusher.core.filter.TransformFilter
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class DumpFilter(private val clientAddress: InetSocketAddress, private val output: OutputStream) : TransformFilter {

    private val logger = LoggerFactory.getLogger(DumpFilter::class.java)

    override fun transform(bb: ByteBuffer) {
        val size = bb.remaining()
        if (size > 0) {
            try {
                if (bb.hasArray()) {
                    val bytes = bb.array()
                    output.write(bytes, bb.arrayOffset() + bb.position(), bb.limit() - bb.position())
                } else {    // the true is this
                    for (i in bb.position() until bb.limit()) {
                        output.write(bb.get(i).toInt())
                    }
                }
            } catch (e: IOException) {
                log(clientAddress, size, e.message?: "")
            }
        }
    }

    private fun log(clientAddress: InetSocketAddress, size: Int, data: CharSequence) {
        val params = arrayOf(clientAddress, size, data)
        logger.error("<{}> ({}): {}", *params)
    }
}

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

import kotlinx.coroutines.channels.Channel
import org.netcrusher.core.filter.TransformFilter
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlinx.coroutines.*

class ChannelDumpFilter(private val clientAddress: InetSocketAddress, private val channel: Channel<ByteArray>) : TransformFilter {

    private val logger = LoggerFactory.getLogger(ChannelDumpFilter::class.java)

    override fun transform(bb: ByteBuffer) {
        val size = bb.remaining()
        if (size > 0) {
            runBlocking {
                launch {
                    if (bb.hasArray()) {
                        channel.send(
                            bb.array().copyOfRange(
                                bb.arrayOffset() + bb.position(),
                                bb.arrayOffset() + bb.limit()
                            )
                        )
                    } else {    // the true is this
                        val bytes = ByteArray(size)
                        for (i in bb.position() until bb.limit())
                            bytes[i - bb.position()] = bb.get(i)
                        channel.send(bytes)
                    }
                }
            }
        }
    }

    private fun log(clientAddress: InetSocketAddress, size: Int, data: CharSequence) {
        val params = arrayOf(clientAddress, size, data)
        logger.error("<{}> ({}): {}", *params)
    }

}

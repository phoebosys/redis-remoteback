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

import com.moandjiezana.toml.Toml
import kotlinx.coroutines.channels.Channel
import org.netcrusher.core.reactor.NioReactor
import org.netcrusher.tcp.TcpCrusher
import org.netcrusher.tcp.TcpCrusherBuilder
import ys.phoebos.redis.CONN_IP
import ys.phoebos.redis.CONN_IP_NAME
import ys.phoebos.redis.CONN_PORT
import ys.phoebos.redis.CONN_PORT_NAME

class ChannelProxy (private val config: Toml) {
    private val reqChannel = Channel<ByteArray>()
    private val rspChannel = Channel<ByteArray>()

    private lateinit var reactor: NioReactor
    private lateinit var crusher: TcpCrusher


    fun start(): Pair<Channel<ByteArray>, Channel<ByteArray>> {

        try {
            reactor = NioReactor()//tickMs
        } catch (e: Exception) {
            ys.phoebos.redis.proxy.LOG.error("Fail to create reactor", e)
            throw e
        }
        try {
            crusher = TcpCrusherBuilder.builder()
                .withReactor(reactor)
                .withBindAddress(
                    config.getString(BIND_IP_NAME, BIND_IP),
                    config.getLong(BIND_PORT_NAME)?.toInt() ?: BIND_PORT
                )
                .withConnectAddress(
                    config.getString(CONN_IP_NAME, CONN_IP),
                    config.getLong(CONN_PORT_NAME)?.toInt() ?: CONN_PORT
                )
                .withOutgoingTransformFilterFactory { addr -> ChannelDumpFilter(addr, reqChannel) }
                .withIncomingTransformFilterFactory { addr -> ChannelDumpFilter(addr, rspChannel) }
                .buildAndOpen()
        } catch (e: Exception) {
            ys.phoebos.redis.proxy.LOG.error("Fail to create crusher", e)
            reactor.close()
            throw e
        }

        val closer = {
            try {
                if (crusher.isOpen) {
                    crusher.close()
                }
            } catch (e: Exception) {
                ys.phoebos.redis.proxy.LOG.error("Fail to close the crusher", e)
            }

            try {
                if (reactor.isOpen) {
                    reactor.close()
                }
            } catch (e: Exception) {
                ys.phoebos.redis.proxy.LOG.error("Fail to close the reactor", e)
            }
        }

        Runtime.getRuntime().addShutdownHook(Thread(closer))



        return reqChannel to rspChannel
    }

    /**
     * 可用命令：crusher.open(), close(), reopen, isopen, getClientAddresses(), getClientByteMeters, closeClient, freeze, unfreeze, isFrozen
     * 调度 reactor.getScheduler().scheduleFreeze(crusher, 3000, TimeUnit.MILLISECONDS);
     * 监听： .withDeferredListeners（true）
     *      .withCreationListener（（addr） - >
     *          LOG.info（“Client is created <{}>”，addr））
     *      .withDeletionListener（（addr，byteMeters） - >
     *          LOG.info（“客户端被删除<{}>”，addr））
     *      .buildAndOpen（）;
     *
     * 延迟： .withIncomingThrottlerFactory((addr) ->
     *           new DelayThrottler(200, 20, TimeUnit.MILLISECONDS))
     *       .withOutgoingThrottlerFactory((addr) ->
     *           new DelayThrottler(200, 20, TimeUnit.MILLISECONDS))
     *
     * 流控：
     *       .withIncomingThrottlerFactory((addr) ->
     *           new ByteRateThrottler(INCOMING_BYTES_PER_SEC, 1, TimeUnit.SECONDS))
     *       .withOutgoingThrottlerFactory((addr) ->
     *           new ByteRateThrottler(OUTGOING_BYTES_PER_SEC, 1, TimeUnit.SECONDS))
     *
     *  拦截：public class LengthPassFilter implements PassFilter {
     *          @Override
     *           public boolean check(ByteBuffer bb) {
     *               return bb.remaining() < 100;
     *           }
     *      }
     */



    fun stop() {
        crusher?.close()
        reactor?.close()
        reqChannel?.close()
        rspChannel?.close()
    }

}
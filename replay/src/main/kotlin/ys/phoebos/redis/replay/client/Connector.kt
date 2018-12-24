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

package ys.phoebos.redis.replay.client

import redis.clients.jedis.util.IOUtils
import ys.phoebos.redis.protocol.Reply
import ys.phoebos.redis.protocol.ReplyInputStream
import ys.phoebos.redis.replay.DEFAULT_TIMEOUT
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class Connector(private val addr: InetSocketAddress,
                private val ssl: Boolean = false,
                private var sslSocketFactory: SSLSocketFactory? = null,
                private var sslParameters: SSLParameters? = null,
                private var hostnameVerifier: HostnameVerifier? = null
                ) : Closeable {

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var replyInputStream: ReplyInputStream? = null
    //private var rowReader: BufferedReader? = null
    private val connectionTimeout = DEFAULT_TIMEOUT
    private val soTimeout = DEFAULT_TIMEOUT
    private var isBroken = false
    private val CRLF = "\r\n".toByteArray()

    fun send(cmd: ByteArray) {
        try {
            connect()
            outputStream!!.write(cmd)
            outputStream!!.write(CRLF)
            outputStream!!.flush()
        } catch (ex: IOException) {
            isBroken = true
            throw ex
        }
    }

    fun send(cmd: String) {
        send(cmd.toByteArray())
    }

    fun sendRow(cmd: String) {
        send(cmd)
    }

    fun receive(): Reply {
        try {
            connect()
            return replyInputStream!!.readReply()
        } catch (ex: IOException) {
            isBroken = true
            throw ex
        }

    }

    fun receiveRow(): String {
        return String(receive().toProtocol())
    }

    fun connect() {
        if (!isConnected()) {
            try {
                socket = Socket()

                socket!!.reuseAddress = true
                socket!!.keepAlive = true
                socket!!.tcpNoDelay = true
                socket!!.setSoLinger(true, 0)
                socket!!.connect(addr, connectionTimeout)
                // socket!!.soTimeout = soTimeout

                if (ssl) {
                    if (null == sslSocketFactory) {
                        sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                    }
                    socket = sslSocketFactory!!.createSocket(socket, addr.hostName, addr.port, true)
                    if (null != sslParameters) {
                        (socket as SSLSocket).sslParameters = sslParameters
                    }
                    if (null != hostnameVerifier && !hostnameVerifier!!.verify(addr.hostName, (socket as SSLSocket).session)) {
                        throw IOException("The connection to ${addr.hostName} failed ssl/tls hostname verification.")
                    }
                }
                outputStream = socket!!.getOutputStream()
                replyInputStream = ReplyInputStream(socket!!.getInputStream())
//                rowReader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            } catch (ex: IOException) {
                isBroken = true
                throw IOException("Failed connecting to host ${addr.hostName}:${addr.port}", ex)
            }
        }
    }

    fun isConnected(): Boolean {
        return socket != null && socket!!.isBound && !socket!!.isClosed && socket!!.isConnected
                && !socket!!.isInputShutdown && !socket!!.isOutputShutdown
    }

    fun disconnect() {
        if (isConnected()) {
            try {
                outputStream!!.flush()
                socket!!.close()
            } catch (ex: IOException) {
                isBroken = true
                throw IOException(ex)
            } finally {
                IOUtils.closeQuietly(socket)
            }
        }
    }

    protected fun flush() {
        try {
            outputStream!!.flush()
        } catch (ex: IOException) {
            isBroken = true
            throw ex
        }
    }

    override fun close() {
        disconnect()
    }
}

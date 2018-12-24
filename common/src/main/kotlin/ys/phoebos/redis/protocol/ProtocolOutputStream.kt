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

import java.io.FilterOutputStream
import java.io.OutputStream

class ProtocolOutputStream(output: OutputStream): FilterOutputStream(output) {

    override fun write(cmd: ByteArray) {
        super.write(cmd)
        super.flush()
    }

    fun write(cmd: String) {
        super.write(cmd.toByteArray())
        super.flush()
    }
}

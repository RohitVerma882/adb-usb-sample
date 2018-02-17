/* Copyright (C) 2018 Charles Muchene
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.charlesmuchene.adb.models

import com.charlesmuchene.adb.utilities.MAX_BUFFER_PAYLOAD
import com.charlesmuchene.adb.utilities.MESSAGE_DATA_PAYLOAD
import com.charlesmuchene.adb.utilities.MESSAGE_HEADER_PAYLOAD
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Adb message
 */
class AdbMessage {

    val headerBuffer = ByteBuffer.allocate(MESSAGE_HEADER_PAYLOAD)
            .order(ByteOrder.LITTLE_ENDIAN)

    val dataBuffer = ByteBuffer.allocate(MESSAGE_DATA_PAYLOAD)
            .order(ByteOrder.LITTLE_ENDIAN)

    val command: Int
        get() = headerBuffer.getInt(0)

    val argumentZero: Int
        get() = headerBuffer.getInt(4)

    val argumentOne: Int
        get() = headerBuffer.getInt(8)

    val dataLength: Int
        get() = headerBuffer.getInt(12)

    /**
     * Check if message has data payload
     *
     * @return `true` if this message has a data payload, `false` otherwise
     */
    fun hasDataPayload() = dataLength > 0

    /**
     * Determine if this message is a small payload. A small payload
     * is efficient to send in adb since the header can be sent together
     * with the data payload in one go.
     *
     * @return `true` if the total message payload <= [MAX_BUFFER_PAYLOAD], `false`
     * otherwise
     */
    fun isSmallPayload() = (MESSAGE_HEADER_PAYLOAD + dataLength) <= MAX_BUFFER_PAYLOAD

    /**
     * Get the total message payload
     *
     * @return Total message payload (header + data)
     */
    fun getTotalPayload() = headerBuffer.array() + dataBuffer.array()

    /**
     * Set up the message with a byte array payload
     *
     * @param command Adb command constant
     * @param argumentZero Argument zero
     * @param argumentOne Argument one
     * @param data Data payload as a [ByteArray]
     */
    operator fun set(command: Int, argumentZero: Int, argumentOne: Int, data: ByteArray?) {
        with(headerBuffer) {
            putInt(0, command)
            putInt(4, argumentZero)
            putInt(8, argumentOne)
            putInt(12, data?.size ?: 0)
            putInt(16, if (data == null) 0 else checksum(data))
            putInt(20, command.inv())
        }

        if (data != null) dataBuffer.put(data, 0, data.size)
    }

    /**
     * Set up the message with a byte buffer payload
     *
     * @param command Adb command constant
     * @param argumentZero Argument zero
     * @param argumentOne Argument one
     * @param data Data payload as a [ByteBuffer]
     */
    operator fun set(command: Int, argumentZero: Int, argumentOne: Int, data: ByteBuffer) {
        set(command, argumentZero, argumentOne, data.array())
    }

    /**
     * Set up the message with a string payload
     *
     * @param command Adb command constant
     * @param argumentZero Argument zero
     * @param argumentOne Argument one
     * @param data Data payload as string
     */
    operator fun set(command: Int, argumentZero: Int, argumentOne: Int, data: String) {
        val dataPayload = data + "\u0000"
        set(command, argumentZero, argumentOne, dataPayload.toByteArray())
    }

    /**
     * Set up the message with no data payload
     *
     * @param command Adb command constant
     * @param argumentZero Argument zero
     * @param argumentOne Argument one
     */
    operator fun set(command: Int, argumentZero: Int, argumentOne: Int) {
        set(command, argumentZero, argumentOne, null as ByteArray?)
    }

    /**
     * Checksum for the provided data
     *
     * @param data Data to perform checksum for
     */
    private fun checksum(data: ByteArray): Int {
        var result = 0
        for (index in data.indices) {
            var element = data[index].toInt()
            if (element < 0) element += 256
            result += element
        }
        return result
    }

}
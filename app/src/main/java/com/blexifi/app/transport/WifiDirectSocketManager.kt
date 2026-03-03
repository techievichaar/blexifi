package com.blexifi.app.transport

import com.blexifi.mesh.transport.SocketFrameCodec
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer

class WifiDirectSocketManager {
    fun startServer(port: Int): ServerSocket = ServerSocket(port)

    fun accept(serverSocket: ServerSocket): Socket = serverSocket.accept()

    fun connect(host: String, port: Int): Socket = Socket(host, port)

    fun send(socket: Socket, payload: ByteArray) {
        val out: OutputStream = socket.getOutputStream()
        out.write(SocketFrameCodec.encode(payload))
        out.flush()
    }

    fun receive(socket: Socket): ByteArray {
        val input: InputStream = socket.getInputStream()
        val lenBuffer = readExactly(input, 4)
        val length = ByteBuffer.wrap(lenBuffer).int
        val payload = readExactly(input, length)
        return SocketFrameCodec.decode(ByteBuffer.allocate(4 + payload.size).putInt(length).put(payload).array())
    }

    private fun readExactly(input: InputStream, length: Int): ByteArray {
        val result = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(result, offset, length - offset)
            if (read <= 0) throw IllegalStateException("Socket closed before reading frame")
            offset += read
        }
        return result
    }
}

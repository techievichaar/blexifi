package com.blexifi.mesh.transport;

import java.nio.ByteBuffer;

public final class SocketFrameCodec {
    private SocketFrameCodec() {
    }

    public static byte[] encode(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + payload.length);
        buffer.putInt(payload.length);
        buffer.put(payload);
        return buffer.array();
    }

    public static byte[] decode(byte[] framedPayload) {
        ByteBuffer buffer = ByteBuffer.wrap(framedPayload);
        int len = buffer.getInt();
        if (len < 0 || len > buffer.remaining()) {
            throw new IllegalStateException("Corrupt frame length");
        }
        byte[] payload = new byte[len];
        buffer.get(payload);
        return payload;
    }
}

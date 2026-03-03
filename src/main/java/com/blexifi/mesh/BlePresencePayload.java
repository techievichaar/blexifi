package com.blexifi.mesh;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public record BlePresencePayload(String deviceId, int protocolVersion, List<String> capabilities) {
    public byte[] encode() {
        byte[] idBytes = deviceId.getBytes(StandardCharsets.UTF_8);
        List<byte[]> capabilityBytes = new ArrayList<>();
        int size = 1 + 1 + idBytes.length + 1;
        for (String capability : capabilities) {
            byte[] bytes = capability.getBytes(StandardCharsets.UTF_8);
            capabilityBytes.add(bytes);
            size += 1 + bytes.length;
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put((byte) protocolVersion);
        buffer.put((byte) idBytes.length);
        buffer.put(idBytes);
        buffer.put((byte) capabilityBytes.size());
        for (byte[] bytes : capabilityBytes) {
            buffer.put((byte) bytes.length);
            buffer.put(bytes);
        }
        return buffer.array();
    }

    public static BlePresencePayload decode(byte[] raw) {
        ByteBuffer buffer = ByteBuffer.wrap(raw);
        int protocolVersion = Byte.toUnsignedInt(buffer.get());

        int idSize = Byte.toUnsignedInt(buffer.get());
        byte[] idBytes = new byte[idSize];
        buffer.get(idBytes);

        int capabilityCount = Byte.toUnsignedInt(buffer.get());
        List<String> capabilities = new ArrayList<>();
        for (int i = 0; i < capabilityCount; i++) {
            int len = Byte.toUnsignedInt(buffer.get());
            byte[] capabilityBytes = new byte[len];
            buffer.get(capabilityBytes);
            capabilities.add(new String(capabilityBytes, StandardCharsets.UTF_8));
        }

        return new BlePresencePayload(new String(idBytes, StandardCharsets.UTF_8), protocolVersion, capabilities);
    }
}

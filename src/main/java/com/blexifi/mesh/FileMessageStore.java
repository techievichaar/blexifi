package com.blexifi.mesh;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class FileMessageStore implements MessageStore {
    private final Path filePath;

    public FileMessageStore(Path filePath) {
        this.filePath = filePath;
        ensureFileExists();
    }

    @Override
    public synchronized void upsert(StoredMessage message) {
        Map<UUID, StoredMessage> all = loadAll();
        all.put(message.envelopeId(), message);
        persistAll(all);
    }

    @Override
    public synchronized Optional<StoredMessage> findByEnvelopeId(UUID envelopeId) {
        return Optional.ofNullable(loadAll().get(envelopeId));
    }

    @Override
    public synchronized List<StoredMessage> listPending() {
        List<StoredMessage> pending = new ArrayList<>();
        for (StoredMessage message : loadAll().values()) {
            if (message.state() == DeliveryState.PENDING || message.state() == DeliveryState.RELAYED) {
                pending.add(message);
            }
        }
        return pending;
    }

    private void ensureFileExists() {
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize file store: " + filePath, e);
        }
    }

    private Map<UUID, StoredMessage> loadAll() {
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            Map<UUID, StoredMessage> result = new LinkedHashMap<>();
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                StoredMessage message = deserialize(line);
                result.put(message.envelopeId(), message);
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file store: " + filePath, e);
        }
    }

    private void persistAll(Map<UUID, StoredMessage> all) {
        StringBuilder sb = new StringBuilder();
        for (StoredMessage message : all.values()) {
            sb.append(serialize(message)).append('\n');
        }
        try {
            Files.writeString(
                    filePath,
                    sb.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist file store: " + filePath, e);
        }
    }

    private static String serialize(StoredMessage message) {
        return message.envelopeId() + "|"
                + b64(message.sourceId()) + "|"
                + b64(message.destinationId()) + "|"
                + b64(message.ciphertext()) + "|"
                + message.attempts() + "|"
                + message.state().name();
    }

    private static StoredMessage deserialize(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 6) {
            throw new IllegalStateException("Corrupt record in file message store");
        }

        return new StoredMessage(
                UUID.fromString(parts[0]),
                fromB64(parts[1]),
                fromB64(parts[2]),
                fromB64(parts[3]),
                Integer.parseInt(parts[4]),
                DeliveryState.valueOf(parts[5])
        );
    }

    private static String b64(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String fromB64(String input) {
        return new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
    }
}

package br.edu.trabalho05.common;

import java.time.Instant;
import java.util.UUID;

public record MessagePayload(UUID id, Instant createdAt, String type, String content) {
    public MessagePayload {
        if (id == null || createdAt == null || type == null || type.isBlank()
                || content == null || content.isBlank()) {
            throw new IllegalArgumentException("Mensagem com campos obrigatórios ausentes");
        }
    }
}

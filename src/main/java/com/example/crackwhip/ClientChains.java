package com.example.crackwhip;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Клиентская таблица «id сущности -> id держателя». Чистая Java, безопасна для сервера. */
public final class ClientChains {
    private static final Map<Integer, Integer> MAP = new ConcurrentHashMap<>();

    private ClientChains() {}

    public static void set(int entityId, int holderId) {
        if (holderId < 0) {
            MAP.remove(entityId);
        } else {
            MAP.put(entityId, holderId);
        }
    }

    public static Integer get(int entityId) {
        return MAP.get(entityId);
    }

    public static void clear() {
        MAP.clear();
    }
}

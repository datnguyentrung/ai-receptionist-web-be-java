package com.dat.ai_receptionist_web.service.Training.scan;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Local wake-up registry for open scans. Persistence remains the authority; this
 * component deliberately contains only request IDs and expiry timestamps.
 */
@Component
public class FaceCheckInExpiryRegistry {
    private final DelayQueue<ExpiryToken> expiries = new DelayQueue<>();
    private final ConcurrentMap<UUID, ExpiryToken> byRequestId = new ConcurrentHashMap<>();

    public void track(UUID requestId, Instant expiresAt) {
        ExpiryToken token = new ExpiryToken(requestId, expiresAt);
        ExpiryToken previous = byRequestId.put(requestId, token);
        if (previous != null) expiries.remove(previous);
        expiries.offer(token);
    }

    public void untrack(UUID requestId) {
        ExpiryToken token = byRequestId.remove(requestId);
        if (token != null) expiries.remove(token);
    }

    /** Returns only scans whose local expiry time has arrived; it never reads the database. */
    public List<UUID> pollDue() {
        List<UUID> due = new ArrayList<>();
        ExpiryToken token;
        while ((token = expiries.poll()) != null) {
            if (byRequestId.remove(token.requestId(), token)) due.add(token.requestId());
        }
        return due;
    }

    private record ExpiryToken(UUID requestId, Instant expiresAt) implements Delayed {
        @Override
        public long getDelay(TimeUnit unit) {
            long milliseconds = expiresAt.toEpochMilli() - System.currentTimeMillis();
            return unit.convert(milliseconds, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other instanceof ExpiryToken token) return expiresAt.compareTo(token.expiresAt);
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}

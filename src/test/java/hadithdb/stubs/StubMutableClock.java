package hadithdb.stubs;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

public final class StubMutableClock extends Clock
{
    private final AtomicReference<Instant> now;
    private final ZoneId zone;

    public StubMutableClock(LocalDate date, ZoneId zone)
    {
        this.zone = zone;
        this.now = new AtomicReference<>(date.atStartOfDay(zone).toInstant());
    }

    public void setDate(LocalDate date) { now.set(date.atStartOfDay(zone).toInstant()); }
    @Override public ZoneId getZone() { return zone; }
    @Override public Instant instant() { return now.get(); }
    @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
}

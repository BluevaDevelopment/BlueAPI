package net.blueva.foundation.scheduler;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.task.TaskRegistry;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper over {@link HytaleServer#SCHEDULED_EXECUTOR}. Hytale's own
 * {@link TaskRegistry} only tracks a {@link java.util.concurrent.Future} for
 * cleanup on plugin unload - it has no {@code runTaskLater}/{@code runTaskTimer}
 * scheduling API the way Bukkit's does, so this builds on the JVM's own
 * {@code ScheduledExecutorService} instead.
 */
public class Scheduler {

    protected Scheduler() {
    }

    public static ScheduledFuture<?> runLater(Runnable task, long delay, TimeUnit unit) {
        return HytaleServer.SCHEDULED_EXECUTOR.schedule(task, delay, unit);
    }

    public static ScheduledFuture<?> runTimer(Runnable task, long delay, long period, TimeUnit unit) {
        return HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(task, delay, period, unit);
    }

    /** Same as {@link #runLater}, but registers the task with {@code taskRegistry} so it is cancelled on plugin unload. */
    public static ScheduledFuture<Void> runLaterTracked(TaskRegistry taskRegistry, Runnable task, long delay, TimeUnit unit) {
        Callable<Void> callable = () -> {
            task.run();
            return null;
        };
        ScheduledFuture<Void> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(callable, delay, unit);
        taskRegistry.registerTask(future);
        return future;
    }
}

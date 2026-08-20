package net.blueva.foundation.scheduler;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;

/** Thin wrapper over {@link Plugin#getProxy()}'s {@code TaskScheduler}. */
public class Scheduler {

    protected Scheduler() {
    }

    public static ScheduledTask run(Plugin plugin, Runnable task) {
        return plugin.getProxy().getScheduler().runAsync(plugin, task);
    }

    public static ScheduledTask runLater(Plugin plugin, Runnable task, long delay, TimeUnit unit) {
        return plugin.getProxy().getScheduler().schedule(plugin, task, delay, unit);
    }

    public static ScheduledTask runTimer(Plugin plugin, Runnable task, long delay, long period, TimeUnit unit) {
        return plugin.getProxy().getScheduler().schedule(plugin, task, delay, period, unit);
    }
}

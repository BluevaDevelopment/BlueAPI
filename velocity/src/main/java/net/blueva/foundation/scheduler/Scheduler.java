package net.blueva.foundation.scheduler;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;

/** Thin wrapper over {@link ProxyServer#getScheduler()}. */
public class Scheduler {

    protected Scheduler() {
    }

    public static ScheduledTask run(ProxyServer proxyServer, Object plugin, Runnable task) {
        return proxyServer.getScheduler().buildTask(plugin, task).schedule();
    }

    public static ScheduledTask runLater(ProxyServer proxyServer, Object plugin, Runnable task, long delay, TimeUnit unit) {
        return proxyServer.getScheduler().buildTask(plugin, task).delay(delay, unit).schedule();
    }

    public static ScheduledTask runTimer(ProxyServer proxyServer, Object plugin, Runnable task, long delay, long period, TimeUnit unit) {
        return proxyServer.getScheduler().buildTask(plugin, task).delay(delay, unit).repeat(period, unit).schedule();
    }
}

package com.jruk8.juicytpa.core.commands.tpa;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * Schedules delayed work. The production implementation delegates to the
 * global-region scheduler (Folia-safe); tests substitute a fake.
 */
public interface TaskScheduler {

    /**
     * Runs a task after a delay.
     *
     * @param task the work to run
     * @param delayTicks the delay in ticks
     * @return the scheduled task, cancellable while pending
     */
    ScheduledTask runLater(Runnable task, long delayTicks);
}

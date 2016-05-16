/*
 * Copyright (C) 2015 Axel Müller <axel.mueller@avanux.de>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package de.avanux.smartapplianceenabler.appliance;

import java.util.ArrayList;
import java.util.List;

import de.avanux.smartapplianceenabler.log.ApplianceLogger;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.slf4j.LoggerFactory;

/**
 * Monitors running time of an appliance.
 * Make sure that the timezone is set correctly, otherwise remaining running time will be incorrect.
 * Refer to http://www.gtkdb.de/index_36_2248.html
 */
public class RunningTimeMonitor implements RunningTimeController, ApplianceIdConsumer {
    private ApplianceLogger logger = new ApplianceLogger(LoggerFactory.getLogger(RunningTimeMonitor.class));
    private List<TimeFrame> timeFrames;
    private TimeFrame currentTimeFrame;
    private Instant intervalBeginn;
    private Instant statusChangedAt;
    private Long remainingMinRunningTime;
    private boolean running;

    @Override
    public void setApplianceId(String applianceId) {
        this.logger.setApplianceId(applianceId);
    }

    public void setTimeFrames(List<TimeFrame> timeFrames) {
        this.timeFrames = timeFrames;
        if(logger.isDebugEnabled()) {
            for(TimeFrame timeFrame : timeFrames) {
                logger.debug("Configured timeframe is " + timeFrame.toString());
            }
        }
    }
    
    public List<TimeFrame> getTimeFrames() {
        return timeFrames;
    }

    public void setRunning(boolean running) {
        setRunning(running, new Instant());
    }

    protected void setRunning(boolean running, Instant statusChangedAt) {
        this.running = running;
        this.statusChangedAt = statusChangedAt;
    }

    public long getRemainingMinRunningTimeOfCurrentTimeFrame() {
        return getRemainingMinRunningTimeOfCurrentTimeFrame(new Instant());
    }

    protected long getRemainingMinRunningTimeOfCurrentTimeFrame(Instant now) {
        update(now);
        return remainingMinRunningTime != null ? remainingMinRunningTime : 0;
    }
    
    public TimeFrame getCurrentTimeFrame() {
        return currentTimeFrame;
    }

    public void update() {
        update(new Instant());
    }

    protected void update(Instant now) {
        TimeFrame timeFrameForInstant = findCurrentTimeFrame(now);
        Interval interval = null;
        boolean timeframeExpired = false;
        if(timeFrameForInstant != null) {
            // timeframe found
            if(currentTimeFrame == null || ! timeFrameForInstant.equals(currentTimeFrame)) {
                // new timeframe or timeframe changed
                currentTimeFrame = timeFrameForInstant;
            }
            if(running) {
                // running
                if(intervalBeginn == null) {
                    // running was set to true after interval begin
                    interval = new Interval(statusChangedAt, now);
                }
                else {
                    // no status change in interval
                    interval = new Interval(intervalBeginn, now);
                }
                intervalBeginn = now;
            }
            else if (intervalBeginn != null && statusChangedAt != null) {
                // running was set to false after interval begin
                interval = new Interval(intervalBeginn, statusChangedAt);
                intervalBeginn = null;
                statusChangedAt = null;
            }
            else {
                // not running
                long remainingIntervalSeconds = getRemainingSecondsOfCurrentTimeFrame(currentTimeFrame, now);
                if(currentTimeFrame.getInterval().getStart().isBefore(now) && currentTimeFrame.getMinRunningTime() > remainingIntervalSeconds) {
                    // MinRunningTime does not fit into remainingIntervalSeconds - reduce running time to finish at LatestEnd
                    remainingMinRunningTime = Double.valueOf(new Interval(now, currentTimeFrame.getInterval().getEnd()).toDuration().getMillis() / 1000).longValue();
                }
                else {
                    // MinRunningTime fits into remainingIntervalSeconds - use it
                    remainingMinRunningTime = timeFrameForInstant.getMinRunningTime();
                }
            }
        }
        else if(currentTimeFrame != null) {
            // timeframe not found anymore
            interval = new Interval(intervalBeginn, now);
            timeframeExpired = true;
        }
        if(interval != null) {
            remainingMinRunningTime = remainingMinRunningTime - Double.valueOf(interval.toDuration().getMillis() / 1000).longValue();
            long remainingIntervalSeconds = getRemainingSecondsOfCurrentTimeFrame(currentTimeFrame, now);
            if(remainingIntervalSeconds < remainingMinRunningTime) {
                remainingMinRunningTime = remainingIntervalSeconds;
            }
        }
        if(timeframeExpired) {
            intervalBeginn = null;
            statusChangedAt = null;
            currentTimeFrame = null;
        }
    }

    private long getRemainingSecondsOfCurrentTimeFrame(TimeFrame currentTimeFrame, Instant now) {
        return Double.valueOf(new Interval(now, currentTimeFrame.getInterval().getEnd()).toDurationMillis() / 1000).longValue();
    }
    
    public TimeFrame findCurrentTimeFrame(Instant instant) {
        if(timeFrames != null) {
            for(TimeFrame timeFrame : timeFrames) {
                if(timeFrame.getInterval().contains(instant)) {
                    return timeFrame;
                }
            }
        }
        return null;
    }
    
    public List<TimeFrame> findFutureTimeFrames(Instant instant) {
        List<TimeFrame> futureTimeFrames = new ArrayList<TimeFrame>();
        for(TimeFrame timeFrame : timeFrames) {
            if(timeFrame.getInterval().getStart().isAfter(instant)) {
                futureTimeFrames.add(timeFrame);
            }
        }
        return futureTimeFrames;
    }
}

/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.sample.tfl;

import org.wso2.carbon.sample.tfl.bus.Bus;

import java.util.ArrayList;
import java.util.Collection;

public class BusInfoUpdater extends Thread {
    private long currentTime;
    private long prevTime;
    private long period;
    private String endPoint;

    public BusInfoUpdater(long time, long period, String endPoint) {
        super();
        currentTime = time;
        prevTime = time;
        this.period = period;
        this.endPoint = endPoint;
    }

    public void run() {
        while (true) {
            try {
                if (TflStream.lastTime != 0) {
                    Collection<Bus> buses = TflStream.buses.values();
                    ArrayList<String> jsonList = new ArrayList<String>();
                    for (Bus bus : buses) {
                        String msg = bus.move(currentTime, period);
                        if (msg != null) {
                            jsonList.add(bus.toCsv());
                        }
                    }
                    currentTime += period;
                    TflStream.writeToFile("tfl-bus-data.out", jsonList, true);
                    long diff = currentTime - System.currentTimeMillis();
                    if (diff >= 0) {
                        Thread.sleep(diff);
                    } else {
                        currentTime = System.currentTimeMillis();
                    }
                } else {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
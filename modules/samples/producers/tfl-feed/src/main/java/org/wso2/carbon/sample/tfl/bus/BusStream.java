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

package org.wso2.carbon.sample.tfl.bus;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.sample.tfl.TflStream;
import org.wso2.carbon.sample.tfl.busstop.BusStop;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class BusStream extends Thread {
    private static Log log = LogFactory.getLog(BusStream.class);
    private String url;

    public BusStream(String url) {
        super();
        this.url = url;
    }

    public void run() {
        try {
            long time = System.currentTimeMillis();
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            log.info("\nSending 'GET' request to URL : " + url);
            log.info("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            inputLine = in.readLine();
            inputLine = inputLine.replaceAll("[\\[\\]\"]", "");
            String[] arr = inputLine.split(",");

            TflStream.lastTime = Long.parseLong(arr[2]) + TflStream.timeOffset;

            ArrayList<Bus> newBuses = new ArrayList<Bus>();
            while ((inputLine = in.readLine()) != null) {
                inputLine = inputLine.replaceAll("[\\[\\]\"]", "");
                arr = inputLine.split(",");
                Bus bus = TflStream.buses.get(arr[4]);
                BusStop bs = TflStream.map.get(arr[1]);
                if (bs == null) {
                    continue;
                }
                if (bus == null) {
                    bus = new Bus(arr[4], arr[5], Integer.parseInt(arr[3]));
                    TflStream.buses.put(arr[4], bus);
                    newBuses.add(bus);
                }
                bus.setData(bs, Long.parseLong(arr[6]));
            }
            for (Bus newBus : newBuses) {
                newBus.setNew();
            }
            in.close();
            log.info("Added buses to a List. " + (System.currentTimeMillis() - time) + " millis");
        } catch (Exception e) {
            log.error("Error occurred while getting bus data: " + e.getMessage(), e);
        }
    }
}

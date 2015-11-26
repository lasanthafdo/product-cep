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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.sample.tfl.bus.BusStream;
import org.wso2.carbon.sample.tfl.busstop.BusStop;
import org.wso2.carbon.sample.tfl.traffic.TrafficPollingTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DataPoller extends Thread {

    public static final String recordedBusStopURL = "http://localhost/TFL/stop.txt";
    public static final String recordedTrafficURL = "http://localhost/TFL/tims_feed.xml";
    public static final String recordedBusURL = "http://localhost/TFL/data";

    public static final String liveTrafficURL = "https://data.tfl.gov.uk/tfl/syndication/feeds/tims_feed.xml";
    public static final String liveBusStopURL = "http://countdown.api.tfl.gov.uk/interfaces/ura/instant_V1?ReturnList=StopID,Latitude,Longitude";
    public static final String liveBusURL = "http://countdown.api.tfl.gov.uk/interfaces/ura/instant_V1?LineID=29,25,38&ReturnList=StopID,LineID,VehicleID,EstimatedTime";

    public static String trafficURL;
    public static String busURL;
    public static String busStopURL;

    private static Log log = LogFactory.getLog(DataPoller.class);

    private boolean isBus;

    public DataPoller(boolean isBus, boolean playback) {
        super();
        this.isBus = isBus;

        if (playback) {
            trafficURL = recordedTrafficURL;
            busURL = recordedBusURL;
            busStopURL = recordedBusStopURL;
        } else {
            trafficURL = liveTrafficURL;
            busURL = liveBusURL;
            busStopURL = liveBusStopURL;
        }
    }

    public void run() {

        if (isBus) {
            getStops();
            getBus();
        } else {
            getDisruptions();
        }

    }

    private void getBus() {
        BusStream b;
        long time = System.currentTimeMillis();
        int i = 0;
        while (true) {
            String url = busURL;
            if (busURL.contains("localhost"))
                url += i + ".txt";
            log.info(url);
            b = new BusStream(url);
            b.start();
            try {
                time += 30000;
                Thread.sleep(time - System.currentTimeMillis());
            } catch (InterruptedException e) {
                //ignore
            }

            i = (i + 1) % 100;
        }

    }

    private static void getDisruptions() {
        TrafficPollingTask ds;
        long time = System.currentTimeMillis();

        while (true) {
            ds = new TrafficPollingTask(trafficURL);
            log.info("Getting Disruption Data ");
            ds.start();
            try {
                time += 30000;
                Thread.sleep(time - System.currentTimeMillis());
            } catch (InterruptedException e) {
                //ignore
            }
        }

    }

    private static void getStops() {
        HttpURLConnection con = null;
        BufferedReader in = null;
        try {
            String[] arr;

            URL obj = new URL(busStopURL);
            con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            log.info("\nSending 'GET' request to URL : " + busStopURL);
            log.info("Response Code : " + responseCode);

            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;

            long time = System.currentTimeMillis();
            inputLine = in.readLine();
            inputLine = inputLine.replaceAll("[\\[\\]\"]", "");
            arr = inputLine.split(",");
            TflStream.timeOffset = time - Long.parseLong(arr[2]);

            ArrayList<String> csvBusStopList = new ArrayList<String>();
            while ((inputLine = in.readLine()) != null) {
                inputLine = inputLine.replaceAll("[\\[\\]\"]", "");
                arr = inputLine.split(",");
                BusStop busStop = new BusStop(arr[1], Double.parseDouble(arr[2]),
                        Double.parseDouble(arr[3]));
                TflStream.map.put(arr[1], busStop);
                csvBusStopList.add(busStop.toCsv());
            }
            TflStream.writeToFile("tfl-bus-stop-data.out", csvBusStopList, false);
        } catch (IOException e) {
            log.error("IOException while reading bus stop data: " + e.getMessage(), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (con != null) {
                    con.disconnect();
                }
            } catch (IOException e) {
                log.error("Error while closing stream: " + e.getMessage(), e);
            }
        }
    }


}


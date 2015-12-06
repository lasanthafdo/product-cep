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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.sample.tfl.bus.BusStream;
import org.wso2.carbon.sample.tfl.busstop.BusStop;
import org.wso2.carbon.sample.tfl.busstop.TimetableInfo;
import org.wso2.carbon.sample.tfl.traffic.TrafficPollingTask;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DataPoller extends Thread {

    public static final String recordedBusStopURL = "http://localhost/TFL/stop.txt";
    public static final String recordedTrafficURL = "http://localhost/TFL/tims_feed.xml";
    public static final String recordedBusURL = "http://localhost/TFL/data";
    public static final String liveTrafficURL = "https://data.tfl.gov.uk/tfl/syndication/feeds/tims_feed.xml";
    public static final String liveBusStopURL = "http://countdown.api.tfl.gov.uk/interfaces/ura/instant_V1?LineID=%s&ReturnList=StopPointName,StopID,Latitude,Longitude,DirectionID";
    public static final String liveBusURL = "http://countdown.api.tfl.gov.uk/interfaces/ura/instant_V1?LineID=%s&ReturnList=StopID,LineID,DirectionID,VehicleID,RegistrationNumber,EstimatedTime";
    public static final String stopPointURL = "http://countdown.api.tfl.gov.uk/interfaces/ura/instant_V1?LineID=%s&ReturnList=StopPointName,StopID,StopCode2,Latitude,Longitude,LineID,DirectionID";
    public static final String timeTableURL = "https://api.tfl.gov.uk/Line/%s/Timetable/";
    public static final String[] busLineIds = new String[]{"29", "25", "38"};
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
            busURL = String.format(liveBusURL, StringUtils.join(busLineIds, ','));
            busStopURL = String.format(liveBusStopURL, StringUtils.join(busLineIds, ','));
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
            } catch (InterruptedException ignored) {
            }
        }
    }


    private static void getTimetables() {
        HttpURLConnection con = null;
        BufferedReader in = null;
        List<String> csvTimetableList = new ArrayList<>();
        try {
            String stopsUrl = String.format(stopPointURL, StringUtils.join(busLineIds, ','));
            URL obj = new URL(stopsUrl);
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            log.info("\nSending 'GET' request to URL : " + stopsUrl);
            log.info("Response Code : " + responseCode);

            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            // first line is metadata, getting rid of that
            String inputLine = in.readLine();
            String stopName, stopID, naptanId, lineId;
            Double latitude, longitude;
            Integer directionId;
            String[] arr;
            while ((inputLine = in.readLine()) != null) {
                inputLine = inputLine.replaceAll("[\\[\\]\"]", "");
                arr = inputLine.split(",");
                stopName = arr[1];
                stopID = arr[2];
                naptanId = arr[3];
                latitude = Double.parseDouble(arr[4]);
                longitude = Double.parseDouble(arr[5]);
                lineId = arr[6];
                directionId = Integer.parseInt(arr[7]);
                URL ttUrl = new URL(String.format(timeTableURL, lineId) + naptanId);
                BufferedReader ttBufferedReader = null;
                HttpURLConnection ttUrlConnection = null;
                try {
                    ttUrlConnection = (HttpURLConnection) ttUrl.openConnection();
                    ttUrlConnection.setRequestMethod("GET");

                    int ttResponseCode = ttUrlConnection.getResponseCode();
                    log.info("\nSending 'GET' request to URL : " + ttUrl);
                    log.info("Response Code : " + ttResponseCode);

                    ttBufferedReader = new BufferedReader(new InputStreamReader(ttUrlConnection.getInputStream()));
                    String ttInputLine;
                    while ((ttInputLine = ttBufferedReader.readLine()) != null) {
                        JSONObject timetableObj = new JSONObject(ttInputLine);
                        JSONObject schedulesObj = (JSONObject) ((JSONArray) ((JSONObject)
                                timetableObj.get("timetable")).get("routes")).get(0);
                        JSONArray timetableArray = (JSONArray) ((JSONObject) ((JSONArray)
                                schedulesObj.get("schedules")).get(1)).get("knownJourneys");
                        for (int j = 0; j < timetableArray.length(); j++) {
                            JSONObject timetableInfoObj = (JSONObject) timetableArray.get(j);
                            TimetableInfo timetableInfo = new TimetableInfo(stopID, stopName, directionId, latitude, longitude,
                                    Integer.valueOf((String) timetableInfoObj.get("hour")),
                                    Integer.valueOf((String) timetableInfoObj.get("minute")), "Monday");
                            csvTimetableList.add(timetableInfo.toCsv());
                        }
                    }
                } catch (FileNotFoundException e) {
                    log.error("FileNotFoundException while reading time table data for URL: " + ttUrl, e);
                } catch (ArrayIndexOutOfBoundsException e) {
                    log.error("ArrayIndexOutOfBoundsException while reading time table data for URL: " + ttUrl, e);
                } catch (IOException e) {
                    log.error("IOException while reading time table data for URL: " + ttUrl, e);
                } finally {
                    if (ttUrlConnection != null) {
                        ttUrlConnection.disconnect();
                    }
                    if (ttBufferedReader != null) {
                        ttBufferedReader.close();
                    }
                }
            }
            TflStream.writeToFile("tfl-timetable-data.out", csvTimetableList, true);
        } catch (IOException e) {
            log.error("IOException while reading time table data: " + e.getMessage(), e);
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("ArrayIndexOutOfBoundsException while reading time table data: " + e.getMessage(), e);
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

    private static void getStops() {
        HttpURLConnection con = null;
        BufferedReader in = null;
        try {
            String[] arr;
            URL obj = new URL(busStopURL);
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            log.info("\nSending 'GET' request to URL : " + busStopURL);
            log.info("Response Code : " + responseCode);

            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            inputLine = in.readLine();
            inputLine = inputLine.replaceAll("[\\[\\]\"]", "");
            arr = inputLine.split(",");
            TflStream.timeOffset = System.currentTimeMillis() - Long.parseLong(arr[2]);

            ArrayList<String> csvBusStopList = new ArrayList<String>();
            ArrayList<String> stops = new ArrayList<String>();
            while ((inputLine = in.readLine()) != null) {
                inputLine = inputLine.replaceAll("[\\[\\]\"]", "");
                arr = inputLine.split(",");
                if (!stops.contains(arr[2])) {
                    BusStop busStop = new BusStop(arr[2], arr[1], Integer.parseInt(arr[5]), Double.parseDouble(arr[3]), Double.parseDouble(arr[4]));
                    TflStream.map.put(arr[2], busStop);
                    csvBusStopList.add(busStop.toCsv());
                    stops.add(arr[2]);
                }
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

    public void run() {
        if (isBus) {
            getTimetables();
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
            } catch (InterruptedException ignored) {
            }
            i = (i + 1) % 100;
        }
    }
}


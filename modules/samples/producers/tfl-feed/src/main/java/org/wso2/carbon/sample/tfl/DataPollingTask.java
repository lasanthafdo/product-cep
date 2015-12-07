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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DataPollingTask implements Runnable {

    public static final String recordedStreamURL = "http://localhost/TFL/tims_feed.xml";
    public static final String liveStreamURL = "https://data.tfl.gov.uk/tfl/syndication/feeds/tims_feed.xml";
    public static String streamURL;
    private static Log log = LogFactory.getLog(DataPollingTask.class);
    private boolean isRunning = true;
    private String streamName;

    public DataPollingTask(String streamName, boolean playback) {
        this.streamName = streamName;
        if (playback) {
            streamURL = recordedStreamURL;
        } else {
            streamURL = liveStreamURL;
        }
    }

    public void run() {
        while (isRunning) {
            getData();
        }
    }

    private void getData() {
        HttpURLConnection con = null;
        BufferedReader in = null;
        try {
            URL obj = new URL(streamURL);
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            log.info("\nSending 'GET' request to URL : " + streamURL);
            log.info("Response Code : " + responseCode);

            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            ArrayList<String> csvBusStopList = new ArrayList<String>();
            while ((inputLine = in.readLine()) != null) {
                csvBusStopList.add(inputLine);
            }
            TflStream.writeToFile(streamName + ".out", csvBusStopList, true);
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


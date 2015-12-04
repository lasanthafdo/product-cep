/*
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.sample.tfl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TflEventDispatcher {

    private static Log log = LogFactory.getLog(TflEventDispatcher.class);

    public static void main(String[] args) {
        EventDispatcher trafficDisruptionData = new EventDispatcher(
                "http://localhost:9763/endpoints/GpsDataOverHttpTrafficStream", "tfl-traffic-data.out", 1000);
        EventDispatcher busTrafficData = new EventDispatcher(
                "http://localhost:9763/endpoints/BusTrafficCsvReceiver", "tfl-bus-data.out", 100);
        EventDispatcher busStopData = new EventDispatcher(
                "http://localhost:9763/endpoints/BusTrafficCsvReceiver", "tfl-bus-stop-data.out", 100);
        EventDispatcher timeTableData = new EventDispatcher(
                "http://localhost:9763/endpoints/TimeTableCsvReceiver", "tfl-timetable-data.out", 100);
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.submit(trafficDisruptionData);
        executorService.submit(busTrafficData);
        executorService.submit(busStopData);
        executorService.submit(timeTableData);
    }

    private static class EventDispatcher implements Runnable {

        private HttpClient client = null;
        private HttpPost post = null;
        private String endPoint = "http://localhost:9763/endpoints/GpsDataOverHttpTrafficStream";
        private String filename = "";
        private long delayBetweenEvents = 1000;

        public EventDispatcher(String endpoint, String filename, long delayBetweenEvents) {
            this.delayBetweenEvents = delayBetweenEvents;
            this.endPoint = endpoint;
            this.filename = filename;
            client = new DefaultHttpClient();
            post = new HttpPost(endPoint);
        }

        @Override
        public void run() {
            log.info("Starting thread to dispatch events to " + endPoint + " reading from " + filename);
            BufferedReader br = null;
            try {
                String sCurrentLine;
                br = new BufferedReader(new FileReader(filename));
                while ((sCurrentLine = br.readLine()) != null) {
                    sendEvent(sCurrentLine);
                    Thread.sleep(delayBetweenEvents);
                }
            } catch (IOException e) {
                log.error("IOException when reading from file: " + filename, e);
            } catch (InterruptedException e) {
                log.error("Thread interrupted while sleeping:" + e.getMessage(), e);
            } catch (Throwable t) {
                log.error("Unexpected error occurred: " + t.getMessage(), t);
            } finally {
                try {
                    if (br != null) {
                        br.close();
                    }
                } catch (IOException ex) {
                    log.error("IOException when closing stream after reading file: " + filename, ex);
                }
            }
        }

        public void sendEvent(String event) {
            HttpResponse response = null;
            try {
                StringEntity entity = new StringEntity(event);
                post.setEntity(entity);
                response = client.execute(post);
                log.info("Dispatching event: " + event);
            } catch (IOException e) {
                log.error("IOException when sending via HTTP: " + e.getMessage(), e);
            } finally {
                if (response != null) {
                    try {
                        EntityUtils.consume(response.getEntity());
                    } catch (IOException e) {
                        log.error("Error when consuming response: " + e.getMessage(), e);
                    }
                }
            }

        }
    }
}

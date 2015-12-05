/*
 * Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.sample.tfl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.sample.tfl.bus.Bus;
import org.wso2.carbon.sample.tfl.busstop.BusStop;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class TflStream {
    public static final String endPointBus = "http://localhost:9763/endpoints/GpsDataOverHttpSpatialObjectStream";
    public static HashMap<String, BusStop> map = new HashMap<String, BusStop>();
    public static ConcurrentHashMap<String, Bus> buses = new ConcurrentHashMap<String, Bus>();
    public static long timeOffset;
    public static long lastTime = 0;
    private static Log log = LogFactory.getLog(TflStream.class);

    public static void main(String[] args) throws XMLStreamException {
        boolean playback = false;
        if (args.length != 0) {
            playback = Boolean.parseBoolean(args[0]);
        }
        try {
            BusInfoUpdater busInfoUpdater = new BusInfoUpdater(System.currentTimeMillis(), 5000, endPointBus);
            DataPoller busData = new DataPoller(true, playback);
            // DataPoller trafficData = new DataPoller(false, playback);
            // trafficData.start();
            busData.start();
            System.out.println("Started getting data");
            Thread.sleep(30000);
            busInfoUpdater.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeToFile(String filename, List<String> jsonList, boolean append) {
        File outFile = new File(filename);
        BufferedWriter bw = null;
        try {
            FileWriter fw = new FileWriter(outFile.getAbsoluteFile(), append);
            bw = new BufferedWriter(fw);
            for (String data : jsonList) {
                bw.write(data);
                bw.newLine();
            }
        } catch (IOException e) {
            log.error("IOException occurred when writing to file: " + e.getMessage(), e);
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                log.error("Error while closing stream: " + e.getMessage(), e);
            }
        }
    }
}

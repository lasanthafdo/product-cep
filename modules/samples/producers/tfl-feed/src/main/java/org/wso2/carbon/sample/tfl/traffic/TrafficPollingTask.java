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

package org.wso2.carbon.sample.tfl.traffic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.sample.tfl.TflStream;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class TrafficPollingTask extends Thread {
    private static Log log = LogFactory.getLog(TrafficPollingTask.class);

    private String streamURL;

    public TrafficPollingTask(String url) {
        super();
        this.streamURL = url;
    }

    public void run() {
        try {
            URL obj = new URL(streamURL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            ArrayList<Disruption> disruptionsList = new ArrayList<Disruption>();
            try {
                // optional default is GET
                con.setRequestMethod("GET");
                int responseCode = con.getResponseCode();
                log.info("Sending 'GET' request to URL : " + streamURL);
                log.info("Response Code : " + responseCode);

                double t = System.currentTimeMillis();
                // Get SAX Parser Factory
                SAXParserFactory factory = SAXParserFactory.newInstance();
                // Turn on validation, and turn off namespaces
                factory.setValidating(true);
                factory.setNamespaceAware(false);
                SAXParser parser = factory.newSAXParser();
                parser.parse(con.getInputStream(), new TrafficXMLHandler(disruptionsList));
                log.info("Number of Disruptions added to the list: " + disruptionsList.size());
                log.info("Time taken for parsing: " + (System.currentTimeMillis() - t));
            } catch (ParserConfigurationException e) {
                log.info("The underlying parser does not support " +
                        " the requested features.");
            } catch (FactoryConfigurationError e) {
                log.info("Error occurred obtaining SAX Parser Factory.");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                con.disconnect();
            }

            ArrayList<String> list = new ArrayList<String>();
            int count = 0;
            for (Disruption disruption : disruptionsList) {
                if (disruption.getState().contains("Active")) {
                    list.add(disruption.toJson());
                }
                count++;
            }
            TflStream.writeToFile("tfl-traffic-data.out", list, true);
        } catch (IOException e) {
            log.error("Error occurred while getting traffic data: " + e.getMessage(), e);
        }

    }
}

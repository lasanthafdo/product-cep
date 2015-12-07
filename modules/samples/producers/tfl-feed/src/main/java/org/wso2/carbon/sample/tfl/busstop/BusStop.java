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

package org.wso2.carbon.sample.tfl.busstop;

public class BusStop {
    public String id;
    public String name;
    public int direction;
    public double latitude;
    public double longitude;
    long timeStamp;

    public BusStop(String StopID, String name, int direction, double lat, double lon) {
        this.id = StopID;
        this.name = name.replaceAll(",", "-");
        this.direction = direction;
        timeStamp = System.currentTimeMillis();
        this.latitude = lat;
        this.longitude = lon;
    }

    @Override
    public String toString() {
        return "{'id':'" + id + "','name':" + name + "','direction':" + direction +
                "','timeStamp':" + timeStamp + ", 'latitude': " + latitude +
                ",'longitude': " + longitude + ", 'type' : 'STOP', 'speed' :" + 0 + ", 'angle':" + 0 + "}";
    }

    public String toCsv() {
        return id + "," + name + "," + direction + "," + timeStamp +
                "," + latitude + "," + longitude + ",STOP" + "," + 0 + "," + 0;
    }
}

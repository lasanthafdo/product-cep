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

public class TimetableInfo {
    private String id;
    private double longitude;
    private double latitude;
    private int hour;
    private int min;
    private String day;

    public TimetableInfo(String stopID, double lat, double lon, int hour, int min, String day) {
        this.id = stopID;
        this.latitude = lat;
        this.longitude = lon;
        this.hour = hour;
        this.min = min;
        this.day = day;
    }

    @Override
    public String toString() {
        return "{'id':'" + id + "','timeStamp':" + System.currentTimeMillis() +
                ", 'lattitude': " + latitude + ",'longitude': " + longitude +
                ", 'type' : 'STOP', 'speed' :" + 0 + ", 'angle':" + 0 + "}";
    }

    public String toCsv() {
        return id + "," + System.currentTimeMillis() + "," + latitude + "," + longitude
                + ",STOP" + "," + day + "," + hour + "," + min;
    }
}

/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hazelcast.simulator.tests.map;

import com.hazelcast.map.IMap;
import com.hazelcast.simulator.hz.HazelcastTest;
import com.hazelcast.simulator.hz.IdentifiedDataSerializablePojo;
import com.hazelcast.simulator.test.annotations.Prepare;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Teardown;
import com.hazelcast.simulator.test.annotations.TimeStep;
import com.hazelcast.simulator.worker.loadsupport.Streamer;
import com.hazelcast.simulator.worker.loadsupport.StreamerFactory;

import java.util.Map;
import java.util.Set;


public class MapAllValuesIdentifiedDataSerializableBenchmark extends HazelcastTest {

    // properties
    // the number of map entries
    public int entryCount = 10_000_000;

    // should the lazy deserialization on client's side be invoked
    public boolean forceClientDeserialization;

    //16 byte + N*(20*N
    private IMap<Integer, IdentifiedDataSerializablePojo> map;
    private final int arraySize = 20;

    private Object blackHole;

    @Setup
    public void setUp() {
        this.map = targetInstance.getMap(name);
    }

    @Prepare(global = true)
    public void prepare() {
        Streamer<Integer, IdentifiedDataSerializablePojo> streamer = StreamerFactory.getInstance(map);
        Integer[] sampleArray = new Integer[arraySize];
        for (int i = 0; i < arraySize; i++) {
            sampleArray[i] = i;
        }

        for (int i = 0; i < entryCount; i++) {
            Integer key = i;
            IdentifiedDataSerializablePojo value = new IdentifiedDataSerializablePojo(sampleArray, String.format("%010d", key));
            streamer.pushEntry(key, value);
        }
        streamer.await();
    }

    @TimeStep
    public void timeStep() throws Exception {
        Set<Map.Entry<Integer, IdentifiedDataSerializablePojo>> entries = map.entrySet();
        if (entries.size() != entryCount) {
            throw new Exception("wrong entry count");
        }
        if (forceClientDeserialization) {
            map.entrySet().forEach(this::sink);
        }
    }

    private void sink(Map.Entry<Integer, IdentifiedDataSerializablePojo> entry) {
        this.blackHole = entry;
    }

    @Teardown
    public void tearDown() {
        map.destroy();
    }
}


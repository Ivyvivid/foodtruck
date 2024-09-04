/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.demo.foodtruck.demos.web;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


@Service
public class WebService {
    public final String filepath = this.getClass().getClassLoader().getResource("Mobile_Food_Facility_Permit.csv").getFile();
    public static final int IOthreads = 4;
    ConcurrentHashMap<Long, FoodTruck> origin = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, FoodTruck> result = new ConcurrentHashMap<>();

    void read2Bean() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filepath));

            HeaderColumnNameMappingStrategy<FoodTruck> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(FoodTruck.class);
            origin = new ConcurrentHashMap<>(new CsvToBeanBuilder<FoodTruck>(br)
                    .withType(FoodTruck.class)
                    .withMappingStrategy(strategy)
                    .build().parse().stream().collect(Collectors.toMap(FoodTruck::getLocationid, ft -> ft)));

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    Object handle(String facilityType, String foodItem) {
        ExecutorService es = Executors.newFixedThreadPool(IOthreads);
        Future future = es.submit(new Runnable() {
            @Override
            public void run() {
                Map<Long, FoodTruck> filterList = origin.entrySet().stream()
                        .filter(entry -> facilityType.equals(entry.getValue().getFacilityType()) && entry.getValue().getFoodItems().contains(foodItem))
                        .collect(Collectors.toMap(
                                s -> s.getKey(),
                                s -> s.getValue()
                        ));
                result = new ConcurrentHashMap<>(filterList);
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            es.shutdown();
        }

    }

    public void export(String facilityType, String foodItem) {
        if (CollectionUtils.isEmpty(origin)) {
            read2Bean();
        }
        CSVWriter csvWriter = null;
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            csvWriter = new CSVWriter(new FileWriter("result_" +  LocalDateTime.now().format(dtf) + ".csv"));

            HeaderColumnNameMappingStrategy<FoodTruck> mappingStrategy = new HeaderColumnNameMappingStrategy();
            mappingStrategy.setType(FoodTruck.class);
            StatefulBeanToCsv<FoodTruck> statefulBeanToCsv = new StatefulBeanToCsvBuilder<FoodTruck>(csvWriter)
                    .withMappingStrategy(mappingStrategy)
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .build();
            Object handle = handle(facilityType, foodItem);

            if (handle == null && !CollectionUtils.isEmpty(result)) {
                ArrayList<FoodTruck> list = new ArrayList<>(result.values());
                mappingStrategy.generateHeader(list.get(0));
                statefulBeanToCsv.write(list);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (csvWriter != null) {
                try {
                    csvWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
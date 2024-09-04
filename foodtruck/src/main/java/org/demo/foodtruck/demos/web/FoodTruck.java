package org.demo.foodtruck.demos.web;

import com.opencsv.bean.CsvBindByPosition;
import lombok.*;

import java.math.BigInteger;

@Data
public class FoodTruck {
    @CsvBindByPosition(position = 0)
    long locationid;
    @CsvBindByPosition(position = 1)
    String applicant;
    @CsvBindByPosition(position = 2)
    String facilityType;
    @CsvBindByPosition(position = 4)
    String locationDescription;
    @CsvBindByPosition(position = 5)
    String address;
    @CsvBindByPosition(position = 8)
    String status;
    @CsvBindByPosition(position = 9)
    String foodItems;
    @CsvBindByPosition(position = 12)
    String latitude;
    @CsvBindByPosition(position = 13)
    String longitude;
}

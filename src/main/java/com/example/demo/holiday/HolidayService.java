package com.example.demo.holiday;

import java.time.LocalDate;
import java.util.Optional;

public interface HolidayService {

    Optional<String> findHoliday(LocalDate date);
}

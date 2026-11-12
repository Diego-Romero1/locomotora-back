package com.locomotora.demo.routine;

import com.locomotora.demo.routine.dto.LastPerformanceResponse;
import com.locomotora.demo.routine.dto.TodayResponse;
import com.locomotora.demo.routine.service.TrainingService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrainingController {
    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    /** Pantalla Hoy. {@code tz} es la zona horaria IANA del telefono (por defecto UTC). */
    @GetMapping("/today")
    public TodayResponse today(@RequestParam(required = false) String tz) {
        return trainingService.today(tz, Instant.now());
    }

    /** {@code ids} separados por coma. */
    @GetMapping("/exercises/last-performance")
    public List<LastPerformanceResponse> lastPerformance(@RequestParam List<UUID> ids) {
        return trainingService.lastPerformance(ids);
    }
}

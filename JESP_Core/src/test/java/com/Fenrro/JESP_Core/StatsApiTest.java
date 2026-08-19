package com.Fenrro.JESP_Core;

import com.Fenrro.JESP_Core.entity.SensorHistory;
import com.Fenrro.JESP_Core.repository.SensorHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"jesp.ws-port=0", "jesp.rules-file=target/test-rules.conf"})
@AutoConfigureMockMvc
@DirtiesContext
@Transactional
class StatsApiTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SensorHistoryRepository repository;

    private LocalDateTime rangeStart() {
        return LocalDate.now().atTime(2, 0);
    }

    private void seed(LocalDateTime timestamp, float temp, float hum) {
        SensorHistory row = new SensorHistory(temp, hum);
        row.setTimestamp(timestamp);
        repository.saveAndFlush(row);
    }

    @Test
    void hourlyProfileGroupsByHourOfDay() throws Exception {
        LocalDateTime start = rangeStart();
        seed(start.plusMinutes(10), 20.0f, 40.0f);
        seed(start.plusHours(1).plusMinutes(15), 21.0f, 45.0f);
        seed(start.plusHours(1).plusMinutes(45), 22.0f, 50.0f);

        mockMvc.perform(get("/api/stats/hourly")
                .param("from", start.format(ISO))
                .param("to", start.plusHours(3).format(ISO)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[2].hour").value(2))
            .andExpect(jsonPath("$[2].avgTemp").value(20.0))
            .andExpect(jsonPath("$[2].minTemp").value(20.0))
            .andExpect(jsonPath("$[2].maxTemp").value(20.0))
            .andExpect(jsonPath("$[2].records").value(1))
            .andExpect(jsonPath("$[3].hour").value(3))
            .andExpect(jsonPath("$[3].avgTemp").value(21.5))
            .andExpect(jsonPath("$[3].minTemp").value(21.0))
            .andExpect(jsonPath("$[3].maxTemp").value(22.0))
            .andExpect(jsonPath("$[3].records").value(2))
            .andExpect(jsonPath("$[4].avgTemp").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$[4].records").value(0));
    }

    @Test
    void summaryAggregatesRange() throws Exception {
        LocalDateTime start = rangeStart();
        seed(start.plusMinutes(10), 20.0f, 40.0f);
        seed(start.plusHours(1).plusMinutes(15), 21.0f, 45.0f);
        seed(start.plusHours(1).plusMinutes(45), 22.0f, 50.0f);

        mockMvc.perform(get("/api/stats/summary")
                .param("from", start.format(ISO))
                .param("to", start.plusHours(3).format(ISO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.avgTemp").value(21.0))
            .andExpect(jsonPath("$.minTemp").value(20.0))
            .andExpect(jsonPath("$.maxTemp").value(22.0))
            .andExpect(jsonPath("$.avgHum").value(45.0))
            .andExpect(jsonPath("$.records").value(3));
    }

    @Test
    void summaryWithoutDataReturnsNullsAndZeroRecords() throws Exception {
        LocalDateTime start = rangeStart();

        mockMvc.perform(get("/api/stats/summary")
                .param("from", start.format(ISO))
                .param("to", start.plusHours(3).format(ISO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.avgTemp").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.records").value(0));
    }
}
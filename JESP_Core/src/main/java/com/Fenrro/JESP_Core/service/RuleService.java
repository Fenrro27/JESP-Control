package com.Fenrro.JESP_Core.service;

import com.Fenrro.JESP_Core.entity.RuleEntity;
import com.Fenrro.JESP_Core.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;

    public record RuleDto(Long id, String name, String deviceId, Integer relayIndex,
                          Boolean targetState, Boolean enabled, Integer priority,
                          String timeStart, String timeEnd,
                          Float tempMin, Float tempMax, Float humMin, Float humMax,
                          String conditionLogic, String daysOfWeek,
                          Float hysteresis, Integer minSwitchIntervalSeconds) {

        public static RuleDto from(RuleEntity e) {
            return new RuleDto(e.getId(), e.getName(), e.getDeviceId(), e.getRelayIndex(),
                    e.getTargetState(), e.isEnabled(), e.getPriority(),
                    e.getTimeStart(), e.getTimeEnd(),
                    e.getTempMin(), e.getTempMax(), e.getHumMin(), e.getHumMax(),
                    e.getConditionLogic(), e.getDaysOfWeek(),
                    e.getHysteresis(), e.getMinSwitchIntervalSeconds());
        }
    }

    @Transactional(readOnly = true)
    public List<RuleDto> list() {
        return ruleRepository.findAllByOrderByPriorityAscIdAsc()
                .stream().map(RuleDto::from).toList();
    }

    @Transactional
    public RuleDto create(RuleDto dto) {
        validate(dto);
        return RuleDto.from(ruleRepository.save(toEntity(dto, new RuleEntity())));
    }

    @Transactional
    public RuleDto update(Long id, RuleDto dto) {
        validate(dto);
        RuleEntity entity = ruleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Regla no encontrada"));
        return RuleDto.from(ruleRepository.save(toEntity(dto, entity)));
    }

    @Transactional
    public void delete(Long id) {
        if (!ruleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Regla no encontrada");
        }
        ruleRepository.deleteById(id);
    }

    private void validate(RuleDto dto) {
        if (dto.relayIndex() == null || dto.relayIndex() < 0 || dto.relayIndex() > 5) {
            fail("relayIndex debe estar entre 0 y 5");
        }
        parseTime(dto.timeStart(), "timeStart");
        parseTime(dto.timeEnd(), "timeEnd");
        checkRangeOrder(dto.tempMin(), dto.tempMax(), "temp");
        checkRangeOrder(dto.humMin(), dto.humMax(), "hum");
        if (dto.conditionLogic() != null && !dto.conditionLogic().equalsIgnoreCase("AND")
                && !dto.conditionLogic().equalsIgnoreCase("OR")) {
            fail("conditionLogic debe ser AND u OR");
        }
        if (dto.daysOfWeek() != null && !dto.daysOfWeek().isBlank()) {
            for (String day : dto.daysOfWeek().toUpperCase().split(",")) {
                try {
                    java.time.DayOfWeek.valueOf(day.trim());
                } catch (IllegalArgumentException e) {
                    fail("Día inválido en daysOfWeek: " + day);
                }
            }
        }
        boolean hasCondition = dto.timeStart() != null || dto.timeEnd() != null
                || dto.tempMin() != null || dto.tempMax() != null
                || dto.humMin() != null || dto.humMax() != null;
        if (!hasCondition) {
            fail("La regla necesita al menos una condición (horario, temperatura o humedad)");
        }
    }

    private void checkRangeOrder(Float min, Float max, String label) {
        if (min != null && max != null && min > max) {
            fail(label + "Min no puede ser mayor que " + label + "Max");
        }
    }

    private void parseTime(String time, String field) {
        if (time == null || time.isBlank()) return;
        try {
            LocalTime.parse(time.trim());
        } catch (DateTimeParseException e) {
            fail(field + " debe tener formato HH:mm");
        }
    }

    private void fail(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private RuleEntity toEntity(RuleDto dto, RuleEntity e) {
        e.setName(dto.name());
        e.setDeviceId((dto.deviceId() == null || dto.deviceId().isBlank())
                ? null : dto.deviceId());
        e.setRelayIndex(dto.relayIndex());
        e.setTargetState(dto.targetState() == null || dto.targetState());
        e.setEnabled(dto.enabled() == null || dto.enabled());
        e.setPriority(dto.priority() == null ? 100 : dto.priority());
        e.setTimeStart(blankToNull(dto.timeStart()));
        e.setTimeEnd(blankToNull(dto.timeEnd()));
        e.setTempMin(dto.tempMin());
        e.setTempMax(dto.tempMax());
        e.setHumMin(dto.humMin());
        e.setHumMax(dto.humMax());
        e.setConditionLogic(dto.conditionLogic() == null ? "AND"
                : dto.conditionLogic().toUpperCase());
        e.setDaysOfWeek((dto.daysOfWeek() == null || dto.daysOfWeek().isBlank())
                ? null : dto.daysOfWeek().toUpperCase());
        e.setHysteresis(dto.hysteresis() == null ? 0.5f : Math.max(0f, dto.hysteresis()));
        e.setMinSwitchIntervalSeconds(dto.minSwitchIntervalSeconds() == null
                ? 0 : Math.max(0, dto.minSwitchIntervalSeconds()));
        return e;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}

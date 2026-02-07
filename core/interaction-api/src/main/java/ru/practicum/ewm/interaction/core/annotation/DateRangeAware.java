package ru.practicum.ewm.interaction.core.annotation;

import java.time.LocalDateTime;

public interface DateRangeAware {

    LocalDateTime getStart();

    LocalDateTime getEnd();
}

package com.epam.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling time slot formatting and validation.
 * <p>
 * Primarily used to:
 * <ul>
 *   <li>Convert time slot identifiers (e.g., "slot1", "slot2") into human-readable time ranges.</li>
 *   <li>Validate that a date string is in ISO_LOCAL_DATE format (yyyy-MM-dd).</li>
 * </ul>
 */
public class TimeSlotUtil {

    // Formatter to validate dates in ISO format (e.g., "2024-04-05")
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    // Formatter for time output in 24-hour format (e.g., "10:30")
    private static final DateTimeFormatter TIME_24H_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Converts a list of time slot identifiers (e.g., "slot1", "slot3") into their
     * corresponding time ranges.
     *
     * <p>The base schedule starts at 10:30 AM with 7 consecutive slots, each
     * lasting 90 minutes and separated by a 15-minute gap.
     *
     * @param inputSlots A list of slot identifiers (e.g., "slot1", "slot5").
     * @return A list of time range strings (e.g., "10:30 - 12:00").
     */
    public static List<String> getSlotTimes(List<String> inputSlots) {
        List<String> allSlotTimes = new ArrayList<>();

        // Initial time slot starts at 10:30 AM
        LocalTime startTime = LocalTime.of(10, 30);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // Generate 7 slot ranges with 90 minutes duration and 15 minutes gap
        for (int i = 1; i <= 7; i++) {
            LocalTime slotStart = startTime;
            LocalTime slotEnd = slotStart.plusMinutes(90);

            // Format time range as string (e.g., "10:30 - 12:00")
            String slotTimeRange = formatter.format(slotStart) + " - " + formatter.format(slotEnd);
            allSlotTimes.add(slotTimeRange);

            // Prepare start time for the next slot
            startTime = slotEnd.plusMinutes(15);
        }

        // Extract and match requested slots
        List<String> result = new ArrayList<>();
        for (String slotId : inputSlots) {
            try {
                // Extract numeric part from slot identifier (e.g., "slot2" → 2)
                int slotNumber = Integer.parseInt(slotId.replaceAll("[^0-9]", ""));
                if (slotNumber >= 1 && slotNumber <= allSlotTimes.size()) {
                    result.add(allSlotTimes.get(slotNumber - 1));
                }
            } catch (NumberFormatException e) {
                // Ignore invalid slot format
            }
        }

        return result;
    }

    /**
     * Validates whether the provided date string follows the ISO_LOCAL_DATE format (yyyy-MM-dd).
     *
     * @param date The date string to validate.
     * @return {@code true} if the date is valid; {@code false} otherwise.
     */
    public static boolean validateDateFormat(String date) {
        try {
            LocalDate.parse(date, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}

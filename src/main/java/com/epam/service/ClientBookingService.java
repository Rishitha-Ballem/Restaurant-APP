package com.epam.service;


import com.epam.edai.run8.team15.dto.BookingServiceRequest;
import com.epam.edai.run8.team15.dto.BookingServiceResponse;
import com.epam.edai.run8.team15.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for handling client booking operations. It provides functionalities
 * to validate user booking requests, check slot availability, ensure location and table
 * validity, and persist reservations into DynamoDB.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ClientBookingService {

    private final DynamoDbTable<Reservation> reservationsDynamoDbTable;
    private final DynamoDbTable<Location> locationDynamoDbTable;
    private final DynamoDbTable<ResTable> resTableDynamoDbTable;
    private final DynamoDbTable<Waiter> waiterDynamoDbTable;
    private final DynamoDbTable<User> userDynamoDbTable;

    // Predefined time slots mapping
    private static final Map<String, List<String>> TIME_SLOT_MAP = Map.of(
            "slot1", List.of("10:30", "12:00"),
            "slot2", List.of("12:15", "13:45"),
            "slot3", List.of("14:00", "15:30"),
            "slot4", List.of("15:45", "17:15"),
            "slot5", List.of("17:30", "19:00"),
            "slot6", List.of("19:15", "20:45"),
            "slot7", List.of("21:00", "22:30")
    );

    // All unique time points from slots (start and end)
    private static final Set<String> TIME_POINTS = new HashSet<>();

    static {
        TIME_SLOT_MAP.values().forEach(times -> TIME_POINTS.addAll(times));
    }


    /**
     * Validates and processes a booking request for a given user.
     *
     * @param request Booking request details.
     * @param email    The authenticated user email making the reservation.
     * @return A response map with either success details or an error message.
     */
    public Map<String, Object> saveResponseForBooking(BookingServiceRequest request, String email) {
        try {
            User user = userDynamoDbTable.getItem(Key.builder().partitionValue(email).build());
            String locationId = request.getLocationId();
            String startTime = request.getTimeFrom();
            String endTime = request.getTimeTo();
            String date = request.getDate();
            LocalDate bookingDate = LocalDate.parse(date);
            int guests = Integer.parseInt(request.getGuestsNumber());

            Location location = getLocation(locationId);
            if (location == null) {
                return error(400, "locationId doesn't exist!");
            }


            if (Integer.parseInt(request.getGuestsNumber()) <= 0) {
                return error(400, "Guest numbers must be greater than 0");
            }
            if (!isDateValid(bookingDate)) {
                return error(400, "Can't book in the past date");
            }

            if (!isTimePointValid(startTime, endTime)) {
                return error(400, "Wrong time slot selected");
            }

            if (!isTimeRangeValid(bookingDate, startTime, endTime)) {
                return error(400, "Sorry, can't book the past slot");
            }

            if (isStartTimeAfterEndTime(startTime, endTime)) {
                return error(400, "Start time of slot can't be after End time");
            }


            if (!locationHasTable(request.getLocationId(), request.getTableNumber())) {
                return error(400, "Table not found for location: " + request.getLocationId());
            }

            if (!tableHasCapacity(request.getTableNumber(), guests)) {
                return error(400, "No of guests cannot be more than the table capacity");
            }

            return reserveTable(request, user);
        } catch (Exception e) {
            return error(500, "Error reserving the table");
        }
    }

    /**
     * Checks if the provided date is today or in the future.
     */
    private boolean isDateValid(LocalDate date) {
        return !date.isBefore(LocalDate.now());
    }

    /**
     * Verifies that both start and end times are valid predefined slot times.
     */
    private boolean isTimePointValid(String start, String end) {
        return TIME_POINTS.contains(start) && TIME_POINTS.contains(end);
    }

    /**
     * Ensures that a time slot on the current day isn't already past.
     */
    private boolean isTimeRangeValid(LocalDate bookingDate, String start, String end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime endTime = LocalTime.parse(end, formatter);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        return !LocalDate.now().equals(bookingDate) || !endTime.isBefore(now.toLocalTime());
    }

    /**
     * Returns true if start time is after end time.
     */
    private boolean isStartTimeAfterEndTime(String start, String end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return LocalTime.parse(start, formatter).isAfter(LocalTime.parse(end, formatter));
    }

    private Map<String, Object> reserveTable(BookingServiceRequest request, User user) {
        System.out.println("Starting reserveTable method...");

        List<BookingServiceResponse> responseList = new ArrayList<>();
        System.out.println("Initialized responseList");

        String date = request.getDate();
        System.out.println("Received date: " + date);

        String tableId = request.getTableNumber();
        System.out.println("Received tableId: " + tableId);

        String userId = user.getEmail();
        System.out.println("User ID (email): " + userId);

        String locationId = request.getLocationId();
        System.out.println("Received locationId: " + locationId);

        String noOfGuests = request.getGuestsNumber();
        System.out.println("Received number of guests: " + noOfGuests);

        String feedbackId = "";
        System.out.println("Initialized feedbackId");

        List<String> preOrder = new ArrayList<>();
        System.out.println("Initialized preOrder list");

        LocalTime bookedAt = LocalTime.now();
        System.out.println("Booked at: " + bookedAt);

        String startTime = request.getTimeFrom();
        String endTime = request.getTimeTo();
        System.out.println("Start time: " + startTime + ", End time: " + endTime);

        String slotFrom = getSlotByStartTime(startTime);
        String slotTo = getSlotByEndTime(endTime);
        System.out.println("Mapped slotFrom: " + slotFrom + ", slotTo: " + slotTo);

        if (!isBookingTimeValid(date, startTime, endTime)) {
            System.out.println("Invalid booking time for date: " + date + ", start: " + startTime + ", end: " + endTime);
            return error(400, "Booking date and time is not valid");
        }

        int s = extractSlotIndex(slotFrom);
        int e = extractSlotIndex(slotTo);
        System.out.println("Slot range: " + s + " to " + e);

        for (int i = s; i <= e; i++) {
            String slot = "slot" + i;
            System.out.println("Processing slot: " + slot);

            String id = date + locationId + tableId + slot;
            System.out.println("Generated reservation ID: " + id);

            if (isSlotReserved(id)) {
                System.out.println("Slot already reserved: " + id);
                return error(409, "Table already Reserved!");
            }

            String status = "Reserved";
            System.out.println("Initial status set: " + status);

            status = getSlotStatusForNow(slot, date);
            System.out.println("Updated status based on current time: " + status);

            Reservation reservation = new Reservation();
            System.out.println("Creating Reservation object");

            reservation.setId(id);
            System.out.println("Set reservation ID: " + id);

            reservation.setUserId(userId);
            System.out.println("Set user ID: " + userId);

            reservation.setStatus(status);
            System.out.println("Set status: " + status);

            reservation.setLocationAddress(locationId);
            System.out.println("Set location address: " + locationId);

            reservation.setTableId(tableId);
            System.out.println("Set table ID: " + tableId);

            reservation.setDate(date);
            System.out.println("Set reservation date: " + date);

            reservation.setTimeSlot(slot);
            System.out.println("Set time slot: " + slot);

            reservation.setPreOrder(preOrder);
            System.out.println("Set preOrder list");

            reservation.setNoOfGuests(Integer.parseInt(noOfGuests));
            System.out.println("Set number of guests: " + noOfGuests);

            reservation.setFeedbackId(feedbackId);
            System.out.println("Set feedback ID");

            reservation.setBookedAt(bookedAt);
            System.out.println("Set booked at time: " + bookedAt);

            Location location = getLocation(locationId);
            System.out.println("Fetched location for locationId: " + locationId);

            if (location == null) {
                System.out.println("Invalid locationId: " + locationId);
                return error(400, "locationId doesn't exist!");
            }

            System.out.println("Fetching least busy waiter...");
            String waiterId = getLeastBusyWaiter(locationId, date, slot);
            System.out.println("Least busy waiter ID: " + waiterId);

            reservation.setWaiterId(waiterId);
            System.out.println("Assigned waiter ID to reservation");

            reservation.setBookedBy("Customer");
            System.out.println("Set bookedBy to Customer");

            System.out.println("Saving reservation to DynamoDB...");
            reservationsDynamoDbTable.putItem(PutItemEnhancedRequest.builder(Reservation.class).item(reservation).build());
            System.out.println("Reservation saved successfully");

            Waiter waiter = getWaiter(waiterId);
            System.out.println("Fetched waiter: " + waiterId);

            waiter.getReservationIds().add(id);
            System.out.println("Added reservation ID to waiter");

            System.out.println("Updating waiter in DynamoDB...");
            waiterDynamoDbTable.updateItem(waiter);
            System.out.println("Waiter updated");

            BookingServiceResponse response = BookingServiceResponse.toBookingService(reservation, location.getAddress());
            System.out.println("Created booking service response");

            responseList.add(response);
            System.out.println("Added response to response list");
        }

        System.out.println("All slots processed successfully. Returning 200 response.");
        return Map.of(
                "statusCode", 200,
                "body", responseList
        );
    }


    /**
     * Checks if a slot reservation already exists and is marked as reserved.
     */
    private boolean isSlotReserved(String reservationId) {
        Reservation existing = reservationsDynamoDbTable.getItem(Key.builder().partitionValue(reservationId).build());
        return existing != null && "Reserved".equalsIgnoreCase(existing.getStatus());
    }

    /**
     * Retrieves the slot key (e.g., "slot1") by matching the given start time.
     */
    private String getSlotByStartTime(String start) {
        return TIME_SLOT_MAP.entrySet().stream()
                .filter(e -> e.getValue().get(0).equals(start))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves the slot key (e.g., "slot2") by matching the given end time.
     */
    private String getSlotByEndTime(String end) {
        return TIME_SLOT_MAP.entrySet().stream()
                .filter(e -> e.getValue().get(1).equals(end))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Parses the numeric slot index from a slot key.
     */
    private int extractSlotIndex(String slot) {
        return Integer.parseInt(slot.replace("slot", ""));
    }

    /**
     * Checks if the specified table exists at the given location.
     *
     * @param locationId The location ID.
     * @param tableId    The table number.
     * @return True if the location has the table; false otherwise.
     */
    public boolean locationHasTable(String locationId, String tableId) {
        Location location = locationDynamoDbTable.getItem(Key.builder().partitionValue(locationId).build());
        return location != null && location.getTables().contains(tableId);
    }

    public boolean tableHasCapacity(String tableId, int guests) {
        ResTable table = resTableDynamoDbTable.getItem(Key.builder().partitionValue(tableId).build());
        return table.getCapacity() >= guests;
    }

    /**
     * Validates that booking time is not in the past for the same day.
     *
     * @param requestDateStr Requested date in yyyy-MM-dd format.
     * @param startTimeStr   Requested start time (HH:mm).
     * @param endTimeStr     Requested end time (HH:mm).
     * @return True if booking time is valid.
     */
    public boolean isBookingTimeValid(String requestDateStr, String startTimeStr, String endTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDate requestDate = LocalDate.parse(requestDateStr);
        LocalDate today = LocalDate.now();

        LocalTime slotStart = LocalTime.parse(startTimeStr, formatter);
        return requestDate.isAfter(today) || (requestDate.equals(today) && LocalTime.now().isBefore(slotStart));
    }

    /**
     * Utility method to create a standardized error response map.
     *
     * @param code    HTTP-like status code.
     * @param message Error message.
     * @return A map representing the error.
     */
    private Map<String, Object> error(int code, String message) {
        return Map.of("statusCode", code, "message", message);
    }

    /**
     * Retrieves location data by locationId from the DynamoDB table.
     *
     * @param locationId Location identifier.
     * @return Location object or null if not found.
     */
    private Location getLocation(String locationId) {
        return locationDynamoDbTable.getItem(Key.builder().partitionValue(locationId).build());
    }


    /**
     * Updates reservation slot and number of guests.
     */
    public Map<String, Object> updateReservationById(String id, BookingServiceRequest request, String email) {
        try {
            String startTimeStr = request.getTimeFrom();
            String endTimeStr = request.getTimeTo();
            String guests = request.getGuestsNumber();
            String date = request.getDate();
            String locationId = request.getLocationId();
            String tableId = request.getTableNumber();

            Reservation reservation = reservationsDynamoDbTable.getItem(Key.builder().partitionValue(id).build());

            if (reservation == null) {
                return Map.of("statusCode", 404, "message", "Reservation not found for given ID");
            }

            if (!reservation.getUserId().equals(email)) {
                return Map.of("statusCode", 403, "message", "You are not authorized to modify this reservation.");
            }

            if ("Cancelled".equalsIgnoreCase(reservation.getStatus())) {
                return Map.of("statusCode", 400, "message", "Cannot modify a cancelled reservation");
            }

            if (!reservation.getLocationAddress().equalsIgnoreCase(locationId)) {
                return Map.of(
                        "statusCode", 400,
                        "message", "location cannot be modified"
                );
            }

            if (!reservation.getDate().equalsIgnoreCase(date)) {
                return Map.of(
                        "statusCode", 400,
                        "message", "date cannot be modified"
                );
            }

            // Parse and validate time
            LocalTime startTime, endTime;
            try {
                startTime = LocalTime.parse(startTimeStr);
                endTime = LocalTime.parse(endTimeStr);
            } catch (DateTimeParseException e) {
                return Map.of("statusCode", 400, "message", "Invalid time format");
            }

            if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
                return Map.of("statusCode", 400, "message", "Start time must be before end time");
            }

            // Parse number of guests
            int numGuests;
            try {
                numGuests = Integer.parseInt(guests);
                if (numGuests <= 0) {
                    return Map.of("statusCode", 400, "message", "Guest number must be greater than 0");
                }
            } catch (NumberFormatException e) {
                return Map.of("statusCode", 400, "message", "Invalid number of guests");
            }

            // Check if reservation date is today and time has already passed
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            LocalDate resDate = LocalDate.parse(reservation.getDate());


            if (resDate.isBefore(today)) {
                return Map.of("statusCode", 400, "message", "Cannot update reservation for a past date");
            }

            if (resDate.equals(today) && endTime.isBefore(LocalTime.now(ZoneId.of("Asia/Kolkata")))) {
                return Map.of("statusCode", 400, "message", "Time slot has already passed");
            }

            if (!locationHasTable(request.getLocationId(), request.getTableNumber())) {
                return error(400, "Table not found for location: " + request.getLocationId());
            }

            if (!tableHasCapacity(tableId, numGuests)) {
                return error(400, "number of guests cannot be more than the table capacity");
            }

            String slotFrom = getSlotByStartTime(startTimeStr);
            String slotTo = getSlotByEndTime(endTimeStr);


            int s = extractSlotIndex(slotFrom);
            int e = extractSlotIndex(slotTo);

            //Waiter assigned to this booking
            Waiter waiter = waiterDynamoDbTable.getItem(Key.builder().partitionValue(reservation.getWaiterId()).build());

            for (int i = s; i <= e; i++) {
                String slot = "slot" + i;
                String newId = date + locationId + tableId + slot;

                if (isSlotReserved(newId)) {
                    return error(409, "Table already Reserved!");
                }

//                Reservation newReservation = new Reservation(newId, email, "Reserved", locationId,
//                        tableId, date, slot, new ArrayList<>(), numGuests, "", reservation.getBookedAt());
                Reservation newReservation = new Reservation();

                newReservation.setId(newId);
                newReservation.setUserId(email);
                newReservation.setStatus("Reserved");
                newReservation.setLocationAddress(locationId);
                newReservation.setTableId(tableId);
                newReservation.setDate(date);
                newReservation.setTimeSlot(slot);
                newReservation.setPreOrder(reservation.getPreOrder());
                newReservation.setNoOfGuests(numGuests);
                newReservation.setFeedbackId("");
                newReservation.setBookedAt(reservation.getBookedAt());
                newReservation.setWaiterId(reservation.getWaiterId());
                newReservation.setBookedBy(reservation.getBookedBy());

                Location location = getLocation(locationId);
                if (location == null) {
                    return error(400, "locationId doesn't exist!");
                }
                reservationsDynamoDbTable.putItem(PutItemEnhancedRequest.builder(Reservation.class).item(newReservation).build());
                waiter.getReservationIds().add(newId);

            }

            reservationsDynamoDbTable.deleteItem(Key.builder().partitionValue(id).build());
            //remove old reservation from waiter reservation list
            waiter.getReservationIds().remove(id);
            waiterDynamoDbTable.updateItem(waiter);

            return Map.of("statusCode", 200, "message", "Reservation updated successfully");

        } catch (Exception e) {
            return Map.of("statusCode", 500, "message", "Something went wrong while updating the reservation");
        }
    }


    public String getLeastBusyWaiter(String locationId, String date, String slot) {
        System.out.println("entered busy block");
        Location location = locationDynamoDbTable.getItem(Key.builder().partitionValue(locationId).build());
        System.out.println("finding waiter ids");
        List<String> waiterIds = location.getListOfWaiters();
        System.out.println("waiter ids" + waiterIds);
        List<Waiter> waiters = waiterIds.stream()
                .map(this::getWaiter)
                .collect(Collectors.toList());

        System.out.println("list of waiters " + waiters);

//        String s = waiters.stream()
//                .map(waiter -> Map.entry(waiter.getEmailId(), getReservationCountForWaiter(date, slot, waiter.getReservationIds())))
//                .min(Comparator.comparingLong(Map.Entry::getValue))
//                .map(Map.Entry::getKey)
//                .orElse(null);
        String s = "";
        Long min = Long.MAX_VALUE;
        for (Waiter waiter : waiters) {
            Long count = getReservationCountForWaiter(date, slot, waiter.getReservationIds());
            System.out.println("waiter " + waiter.getEmailId() + " count " + count);
            if (count < min) {
                s = waiter.getEmailId();
                min = count;
            }
        }

        System.out.println("least busy  waiter is : " + s);
        return s;

    }

    public Waiter getWaiter(String id) {
        return waiterDynamoDbTable.getItem(Key.builder().partitionValue(id).build());
    }

    public Long getReservationCountForWaiter(String date, String slot, List<String> reservationIDs) {
        System.out.println("date : " + date);
        System.out.println("slot " + slot);
        Long count = 0L;
        for (String id : reservationIDs) {
            System.out.println("id : " + id);
            System.out.println("Trying to fetch reservation");

            Reservation reservation = reservationsDynamoDbTable.getItem(Key.builder().partitionValue(id).build());
//            System.out.println("fetched reservation id " + reservation.getId());
            if (reservation != null && reservation.getDate().equalsIgnoreCase(date) && reservation.getTimeSlot().equalsIgnoreCase(slot)) {
                count++;
            }
        }
        return count;
    }

    private String getSlotStatusForNow(String slot, String reservationDate) {
        // Get current date and time in Asia/Calcutta timezone
        ZonedDateTime nowInAsia = ZonedDateTime.now(ZoneId.of("Asia/Calcutta"));
        LocalDate today = nowInAsia.toLocalDate();     // Extract local date
        LocalTime currentTime = nowInAsia.toLocalTime(); // Extract local time

        // If reservation date is not today, it's not "In Progress"
        if (!today.toString().equals(reservationDate)) {
            return "Reserved";
        }

        // Fetch the slot's time range ( although this is already being used )
        List<String> timeRange = TIME_SLOT_MAP.get(slot);
        if (timeRange == null || timeRange.size() != 2) {
            return "Reserved"; // fallback in case slot not found
        }

        // Parse slot start and end times
        LocalTime slotStart = LocalTime.parse(timeRange.get(0));
        LocalTime slotEnd = LocalTime.parse(timeRange.get(1));

        // If current time falls within the slot, mark it as In Progress
        if (!currentTime.isBefore(slotStart) && !currentTime.isAfter(slotEnd)) {
            return "In Progress";
        }

        return "Reserved";
    }

}

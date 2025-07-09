package com.epam.service;

import com.epam.edai.run8.team15.dto.WaiterBookingServiceRequest;
import com.epam.edai.run8.team15.dto.WaiterBookingServiceResponse;
import com.epam.edai.run8.team15.dto.WaiterReservationResponse;
import com.epam.edai.run8.team15.dto.WaiterUpdateBookingRequest;
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

@Slf4j
@RequiredArgsConstructor
@Service
public class WaiterService {

    private final DynamoDbTable<Waiter> waiterDynamoDbTable;
    private final DynamoDbTable<Location> locationDynamoDbTable;
    private final DynamoDbTable<Reservation> reservationDynamoDbTable;
    private final DynamoDbTable<ResTable> resTableDynamoDbTable;
    private final DynamoDbTable<User> userDynamoDbTable;


    private static final Map<String, List<String>> TIME_SLOT_MAP = Map.of(
            "slot1", List.of("10:30", "12:00"),
            "slot2", List.of("12:15", "13:45"),
            "slot3", List.of("14:00", "15:30"),
            "slot4", List.of("15:45", "17:15"),
            "slot5", List.of("17:30", "19:00"),
            "slot6", List.of("19:15", "20:45"),
            "slot7", List.of("21:00", "22:30")
    );

    private static final Set<String> TIME_POINTS = new HashSet<>();

    static {
        TIME_SLOT_MAP.values().forEach(times -> TIME_POINTS.addAll(times));
    }



    public Map<String, Object> getAllReservations(String waiterId, String date, String time, String table) {
        try {
            log.info("Fetching waiter with ID: {}", waiterId);
            Waiter waiter = waiterDynamoDbTable.getItem(Key.builder().partitionValue(waiterId).build());
            if (waiter == null) {
                log.warn("Waiter not found for ID: {}", waiterId);
                return Map.of(
                        "statusCode", 404,
                        "message", "no waiter exists with this email id"
                );
            }

            log.info("Parsing date: {}", date);
            LocalDate localDate;
            try {
                localDate = LocalDate.parse(date);
            } catch (DateTimeParseException exception) {
                log.warn("Invalid date format: {}", date);
                return Map.of(
                        "statusCode", 400,
                        "message", "date format is not correct"
                );
            }

            if (LocalDate.now().isAfter(localDate)) {
                log.warn("Date is in the past: {}", localDate);
                return Map.of(
                        "statusCode", 400,
                        "message", "can't travel in past"
                );
            }

            log.info("Parsing time: {}", time);
            LocalTime localTime;
            try {
                if (time == null) {
                    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
                    localTime = now.toLocalTime().withSecond(0).withNano(0); // remove seconds & nanos
                    log.info("No time provided. Defaulting to current time: {}", localTime);
                } else {
                    localTime = LocalTime.parse(time);
                }
            } catch (DateTimeParseException exception) {
                log.warn("Invalid time format: {}", time);
                return Map.of(
                        "statusCode", 400,
                        "message", "Invalid time format"
                );
            }

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
            LocalTime currentTime = now.toLocalTime().withSecond(0).withNano(0);
            log.info("Current time: {}, Input time: {}", currentTime, localTime);
            if (localDate.isEqual(LocalDate.now()) && currentTime.isAfter(localTime)) {
                log.warn("Time is in the past for today.");
                return Map.of(
                        "statusCode", 400,
                        "message", "can't check for previous slots"
                );
            }

            log.info("Finding future slots after time: " + localTime);
            List<String> futureSlots = TIME_SLOT_MAP.entrySet().stream()
                    .filter(entry -> {
                        LocalTime slotStart = LocalTime.parse(entry.getValue().get(0));
                        return !localTime.isAfter(slotStart);
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (futureSlots.isEmpty()) {
                log.info("No slots available after the given time.");
                return Map.of(
                        "statusCode", 204,
                        "message", "No slots available after the given time"
                );
            }

            log.info("Future slots found: {}", futureSlots);
            List<Reservation> matchingReservations = new ArrayList<>();

            if (table.equalsIgnoreCase("Any Table")) {
                log.info("Fetching reservations for any table...");
                List<String> reservationIds = waiter.getReservationIds();
                for (String resId : reservationIds) {
                    Reservation reservation = reservationDynamoDbTable.getItem(Key.builder().partitionValue(resId).build());
                    if (reservation != null &&
                            reservation.getDate().equalsIgnoreCase(date) &&
                            futureSlots.contains(reservation.getTimeSlot()) &&
                            reservation.getStatus().equalsIgnoreCase("Reserved")) {
                        log.info("Matched reservation: {}", resId);
                        matchingReservations.add(reservation);
                    }
                }
            } else {
                log.info("Fetching reservations for specific table: {}", table);
                Location location = locationDynamoDbTable.getItem(Key.builder().partitionValue(waiter.getLocationId()).build());

                boolean isValidTable = location.getTables().stream()
                        .anyMatch(tableId -> tableId.equalsIgnoreCase(table));

                if (!isValidTable) {
                    log.warn("Invalid table for this location: {}", table);
                    return Map.of(
                            "statusCode", 400,
                            "message", "this table doesn't belong to the location"
                    );
                }

                for (String slot : futureSlots) {
                    String resId = date + location.getId() + table + slot;
                    if (waiter.getReservationIds().contains(resId)) {
                        Reservation reservation = reservationDynamoDbTable
                                .getItem(Key.builder().partitionValue(resId).build());

                        if (reservation != null && reservation.getStatus().equalsIgnoreCase("Reserved")) {
                            log.info("Matched reservation: {}", resId);
                            matchingReservations.add(reservation);
                        }
                    }
                }
            }

            if (matchingReservations.isEmpty()) {
                log.info("No matching reservations found.");
                return Map.of(
                        "statusCode", 204,
                        "message", "no reservation exist"
                );
            }

            log.info("Converting reservations to response...");
            List<WaiterReservationResponse> response = toWaiterReservationResponse(matchingReservations);
            log.info("Returning successful response with {} reservations.", response.size() );

            return Map.of(
                    "statusCode", 200,
                    "body", response
            );

        } catch (Exception e) {
            log.warn("Exception occurred: {}",  e.getMessage());
            e.printStackTrace();
            return Map.of(
                    "statusCode", 500,
                    "message", "Error retrieving reservations"
            );
        }
    }


    public List<WaiterReservationResponse> toWaiterReservationResponse(List<Reservation> reservations) {
        List<WaiterReservationResponse> result = new ArrayList<>();
        for (Reservation reservation : reservations) {
            WaiterReservationResponse waiterReservationResponse = new WaiterReservationResponse();
            log.info("trying to fetch address");

            String locationAddress = locationDynamoDbTable.getItem(Key.builder().partitionValue(reservation.getLocationAddress()).build()).getAddress();
            log.info("address fetched : {}", locationAddress);

            log.info("fetching slot");
            String slot = convertToSlotTimings(reservation.getTimeSlot());
            log.info("slot fetched: {}", slot);

            waiterReservationResponse.setDate(reservation.getDate());
            waiterReservationResponse.setLocationAddress(locationAddress);
            waiterReservationResponse.setSlotTime(slot);
            waiterReservationResponse.setGuests(reservation.getNoOfGuests().toString());
            waiterReservationResponse.setBookedBy(reservation.getBookedBy());
            waiterReservationResponse.setTableId(reservation.getTableId());
            waiterReservationResponse.setReservationId(reservation.getId());
            if (reservation.getUserId().equalsIgnoreCase("visitor")) waiterReservationResponse.setBookedFor("Visitor");
            else {
                User user = userDynamoDbTable.getItem(Key.builder().partitionValue(reservation.getUserId()).build());
                if(user != null) waiterReservationResponse.setBookedFor(user.getFirstName() + " " + user.getLastName());
            }

            result.add(waiterReservationResponse);
        }
        return result;
    }

    public String convertToSlotTimings(String slot) {
        List<String> list = TIME_SLOT_MAP.get(slot);
        return list.get(0) + " - " + list.get(1);
    }

    //booking by waiter -Raushan Raj
    public Map<String, Object> saveBooking(WaiterBookingServiceRequest request, String email) {
        try {

            User user = userDynamoDbTable.getItem(Key.builder().partitionValue(email).build());

            String startTime = request.getTimeFrom();
            log.info("starttime: {}", startTime);

            String endTime = request.getTimeTo();
            log.info("endtime {}", endTime);


            String tableId = request.getTableNumber();

            String userId = user.getEmail(); //waiter email id
            log.info("waiter email {}", userId);

            String locationId = request.getLocationId();

            Key key = Key.builder().partitionValue(locationId).build();
            Location location = locationDynamoDbTable.getItem(key);

            if (location == null) {
                return Map.of(
                        "statusCode", 400,
                        "message", "locationId doesn't exists!"
                );
            }

            if (!location.getListOfWaiters().contains(userId)) {
                return Map.of(
                        "statusCode", 401,
                        "message", "you are not authorised to do this reservation"
                );
            }

            String date = request.getDate();

            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate.parse(date, formatter);
                log.info("Date is in valid format: yyyy-MM-dd");
            } catch (DateTimeParseException e) {
                log.warn("Invalid date format. Expected yyyy-MM-dd");
                return error(400, "Invalid date format. Expected yyyy-MM-dd");
            }

            String status = "Reserved";


            Integer noOfGuest = request.getGuestsNumber();

            String feedbackId = "";

            String clientType = request.getClientType();

            if(!(clientType.equalsIgnoreCase("VISITOR") || clientType.equalsIgnoreCase("Existing"))) {
                return error(400, "client type can be either existing or visitor");
            }

            String customerEmail = request.getCustomerEmail(); // this would contain customer email id or a constant "Visitor"
            //list of dishes customer will book with table booking now its empty

            List<String> preOrder = new ArrayList<>();

            LocalTime bookedAt = LocalTime.now();

            if (clientType.equalsIgnoreCase("Existing") && !checkIfCustomer(customerEmail)) {
                return error(404, "Customer doesn't exist");
            }

            if (noOfGuest <= 0) {
                return error(400, "Guest numbers must be greater than 0");
            }
            if (!isDateValid(LocalDate.parse(date))) {
                return error(400, "Can't book in the past date");
            }

            if (!isTimePointValid(startTime, endTime)) {
                return error(400, "Wrong time slot selected");
            }

            if (!isTimeRangeValid(LocalDate.parse(date), startTime, endTime)) {
                return error(400, "Sorry, can't book the past slot");
            }

            if (isStartTimeAfterEndTime(startTime, endTime)) {
                return error(400, "Start time of slot can't be after End time");
            }

            if (!locationHasTable(locationId, tableId)) {
                return error(400, "Table not found for location: " + request.getLocationId());
            }
            System.out.println("going to check table capacity with guest number");
            if (!tableHasCapacity(tableId, noOfGuest)) {
                return error(400, "number of guests can't be more than table capacity");
            }

            log.info("table capacity checked and now entering reservetable");

            String timeSlot1 = null;

            for (Map.Entry<String, List<String>> entry : TIME_SLOT_MAP.entrySet()) {
                List<String> list = entry.getValue();
                if (list.get(0).equalsIgnoreCase(startTime)) {
                    log.info("booking slot found starttime");
                    timeSlot1 = entry.getKey();
                    log.info("slot for booking is : " + timeSlot1);
                    break;
                }
                System.out.println(startTime + "==" + list.get(0));
            }
            System.out.println(timeSlot1);

            String timeSlot2 = null;
            for (Map.Entry<String, List<String>> entry : TIME_SLOT_MAP.entrySet()) {
                List<String> list = entry.getValue();
                if (list.get(1).equals(endTime)) {
                    log.info("booking slot found endtime");
                    timeSlot2 = entry.getKey();
                    log.info("s");
                    break;
                }
            }

            log.info(timeSlot2);

            int s = Integer.parseInt(String.valueOf(timeSlot1.charAt(timeSlot1.length() - 1)));
            int e = Integer.parseInt(String.valueOf(timeSlot2.charAt(timeSlot2.length() - 1)));
            log.info("waiter booking starttime, endtime {}, {}", s , e);

            WaiterBookingServiceResponse response = new WaiterBookingServiceResponse();
            for (int i = s; i <= e; i++) {
                String slot = "slot" + i;
                String id = date + locationId + tableId + slot;

                Key key1 = Key.builder().partitionValue(id).build();
                Reservation already_reserved_or_not = reservationDynamoDbTable.getItem(key1);
                if (already_reserved_or_not != null && (already_reserved_or_not.getStatus().equalsIgnoreCase("Reserved")
                        || already_reserved_or_not.getStatus().equalsIgnoreCase("In Progress"))) {
                    return Map.of(
                            "statusCode", 409,
                            "message", "table already booked!"

                    );
                }
//bookedFor field holds either anonymous or holds email id of existing customer
                Reservation reservation = new Reservation();
                ;
                log.info("clientType : {}, {}, {}", clientType, customerEmail, noOfGuest);

//                reservation = new Reservation(id, customerEmail, status, locationId, tableId, date, slot, preOrder, noOfGuest, feedbackId, bookedAt);

                status = getSlotStatusForNow(slot, date);
                reservation.setId(id);
                reservation.setStatus(status);
                reservation.setLocationAddress(locationId);
                reservation.setTableId(tableId);
                reservation.setDate(date);
                reservation.setTimeSlot(slot);
                reservation.setPreOrder(preOrder);
                reservation.setNoOfGuests(noOfGuest);
                reservation.setFeedbackId(feedbackId);
                reservation.setBookedAt(bookedAt);
                reservation.setBookedBy("Waiter");
                if (clientType.equalsIgnoreCase("visitor")) reservation.setUserId("Visitor");
                else reservation.setUserId(customerEmail);

                reservation.setWaiterId(userId);

                //add resesrvationId in waiter table list
//                Waiter waiter=waiterDynamoDbTable.getItem(Key.builder().partitionValue(userId).build());
//                List<String> reservationIds=waiter.getReservationIds();
//                reservationIds.add(id);

                log.info("after reservation object creation");
                log.info("reservation location address {}", reservation.getLocationAddress());
                Key k = Key.builder().partitionValue(reservation.getLocationAddress()).build();
                location = locationDynamoDbTable.getItem(k);

                if (location == null) {
                    return Map.of(
                            "statusCode", 400,
                            "message", "locationId doesn't exists!"
                    );
                }
                String locationAddress = location.getAddress();
                log.info("location address {}", locationAddress);
                // userTable.putItem(PutItemEnhancedRequest.builder(User.class).item(user).build());
                log.info("waiterbooking before putting reservation in table");

                reservationDynamoDbTable.putItem(PutItemEnhancedRequest.builder(Reservation.class).item(reservation).build());
                log.info("waiterbooking, after putting reservation in table");
                log.info("now adding reservation to waiter reservation list");
                Waiter waiter = waiterDynamoDbTable.getItem(Key.builder().partitionValue(userId).build());
                log.info("waiter {}", waiter);
                waiter.getReservationIds().add(id);

                log.info("saving waiter to waiter table");
                waiterDynamoDbTable.updateItem(waiter);

                if (clientType.equalsIgnoreCase("VISITOR"))
                    response = WaiterBookingServiceResponse.toBookingService(reservation, locationAddress, "Visitor");
                else {
                    User customer = userDynamoDbTable.getItem(Key.builder().partitionValue(customerEmail).build());
                    response = WaiterBookingServiceResponse.toBookingService(reservation, locationAddress, "Customer " + customer.getFirstName() + " " + customer.getLastName());
                }

            }


            return Map.of(
                    "statusCode", 200,
                    "body", response
            );
        } catch (Exception e) {
            return Map.of(
                    "statusCode", 500,
                    "message", "Error reserving the table "
            );
        }


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

    public boolean isBookingTimeValid(String requestDateStr, String startTimeStr, String endTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDate requestDate = LocalDate.parse(requestDateStr);
        LocalDate today = LocalDate.now();

        LocalTime slotStart = LocalTime.parse(startTimeStr, formatter);
        return requestDate.isAfter(today) || (requestDate.equals(today) && LocalTime.now().isBefore(slotStart));
    }

    public boolean tableHasCapacity(String tableId, int guests) {
        log.info("Inside table capacity check");
        ResTable table = resTableDynamoDbTable.getItem(Key.builder().partitionValue(tableId).build());
        log.info("found table to check capacity");
        return table.getCapacity() >= guests;

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

    private Map<String, Object> error(int code, String message) {
        return Map.of("statusCode", code, "message", message);
    }

    public boolean checkIfCustomer(String email) {
        User user = userDynamoDbTable.getItem(Key.builder().partitionValue(email).build());
        return user != null && "CUSTOMER".equalsIgnoreCase(user.getRole());

    }

    public Map<String, Object> cancelReservationById(String reservationId, String emailId) {
        try {
            log.info("Fetching waiter with emailId: {}", emailId);
            Waiter waiter = waiterDynamoDbTable.getItem(Key.builder().partitionValue(emailId).build());

            if (waiter == null) {
                log.warn("Waiter not found.");
                return Map.of(
                        "statusCode", 404,
                        "message", "Waiter not found"
                );
            }

            List<String> reservationIds = waiter.getReservationIds();
            log.info("Waiter's reservations: {}", reservationIds);


            log.info("Fetching reservation with ID: {}", reservationId);
            Reservation reservation = reservationDynamoDbTable.getItem(Key.builder().partitionValue(reservationId).build());

            if (reservation == null) {
                log.info("Reservation not found.");
                return Map.of(
                        "statusCode", 400,
                        "message", "Reservation does not exist"
                );
            }

            log.info("Reservation status: {}", reservation.getStatus());
            if (!reservation.getStatus().equalsIgnoreCase("Reserved")) {
                log.warn("Reservation is not in a cancellable state.");
                return Map.of(
                        "statusCode", 400,
                        "message", "Reservation not available for cancellation"
                );
            }

            if (!reservationIds.contains(reservationId)) {
                log.warn("Reservation ID " + reservationId + " not found in waiter's list.");
                return Map.of(
                        "statusCode", 403,
                        "message", "Waiter unauthorized to cancel this reservation"
                );
            }


            log.info("Cancelling reservation...");
            reservation.setStatus("Cancelled");
            reservationDynamoDbTable.updateItem(reservation);

            log.info("Removing reservation from waiter's list...");
            waiter.getReservationIds().remove(reservationId);
            waiterDynamoDbTable.updateItem(waiter);

            log.info("Reservation cancelled successfully.");
            return Map.of(
                    "statusCode", 204,
                    "message", "Reservation cancelled successfully"
            );

        } catch (Exception e) {
            log.warn("Exception occurred: {}", e.getMessage());
            return Map.of(
                    "statusCode", 500,
                    "message", "Error cancelling reservation"
            );
        }
    }


    public Map<String, Object> postponeBooking(String reservationId, WaiterUpdateBookingRequest waiterUpdateBookingRequest, String email) {
        try {
            String date = waiterUpdateBookingRequest.getDate();
            String timeFrom = waiterUpdateBookingRequest.getTimeFrom();
            String timeTo = waiterUpdateBookingRequest.getTimeTo();
            String table = waiterUpdateBookingRequest.getTableNumber();
            log.info("Fetching user with email: " + email);
            User user = userDynamoDbTable.getItem(Key.builder().partitionValue(email).build());

            if (!user.getRole().equalsIgnoreCase("Waiter")) {
                log.warn("Unauthorized access: Not a waiter");
                return Map.of("statusCode", 401, "message", "not a waiter");
            }

            log.info("Fetching reservation with ID: {}", reservationId);
            Reservation reservation = reservationDynamoDbTable.getItem(Key.builder().partitionValue(reservationId).build());
            if (reservation == null) {
                log.warn("Reservation not found");
                return Map.of("statusCode", 400, "message", "no reservation exists");
            }

            System.out.println("Checking if waiter is authorized for the reservation");
            if (!reservation.getWaiterId().equalsIgnoreCase(email)) {
                log.warn("Waiter not authorized for this reservation");
                return Map.of("statusCode", 403, "message", "Not authorized to postpone this reservation");
            }

            log.info("Checking reservation status");
            if (!"Reserved".equalsIgnoreCase(reservation.getStatus())) {
                log.warn("Reservation status is not 'Reserved'");
                return Map.of("statusCode", 400, "message", "Only 'Reserved' bookings can be postponed");
            }

            LocalDate today = LocalDate.now();
            LocalDate reservationDate = LocalDate.parse(reservation.getDate());
            if (reservationDate.isBefore(today)) {
                log.warn("Reservation date is in the past");
                return Map.of("statusCode", 400, "message", "Cannot postpone a past reservation");
            }

            if (reservationDate.isEqual(today)) {
                log.warn("Checking time for today's reservation");
                List<String> currentSlotTimes = TIME_SLOT_MAP.get(reservation.getTimeSlot());
                if (currentSlotTimes == null) {
                    log.warn("Invalid time slot for current reservation");
                    return Map.of("statusCode", 400, "message", "Invalid current reservation slot");
                }

                LocalTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalTime();
                LocalTime slotStart = LocalTime.parse(currentSlotTimes.get(0));

                if (now.isAfter(slotStart)) {
                    log.warn("Time slot has already started or passed for today's reservation");
                    return Map.of("statusCode", 400, "message", "Cannot postpone a past-time slot today");
                }
            }

            LocalDate newDate;
            try {
                log.info("Parsing new date: {}", date);
                newDate = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                log.warn("Invalid new date format: {}", date);
                return Map.of("statusCode", 400, "message", "Invalid new date format");
            }

            if (newDate.isBefore(today)) {
                log.warn("New reservation date is in the past");
                return Map.of("statusCode", 400, "message", "Cannot postpone to a past date");
            }

            LocalTime parsedTimeFrom;
            LocalTime parsedTimeTo;
            try {
                log.warn("Parsing new times: from = {}, to = {}",timeFrom, timeTo);
                parsedTimeFrom = LocalTime.parse(timeFrom);
                parsedTimeTo = LocalTime.parse(timeTo);
            } catch (DateTimeParseException e) {
                log.warn("Invalid time format");
                return Map.of("statusCode", 400, "message", "Invalid time format. Use HH:mm (e.g., 14:00)");
            }

            if (newDate.isEqual(today)) {
                LocalTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalTime();
                if (now.isAfter(parsedTimeFrom)) {
                    System.out.println("New time slot starts in the past for today's reservation");
                    return Map.of("statusCode", 400, "message", "Cannot postpone to a past slot today");
                }
            }

            log.info("Fetching waiter details");
            Waiter waiter = waiterDynamoDbTable.getItem(Key.builder().partitionValue(email).build());
            String locationId = waiter.getLocationId();

            log.info("Checking if table exists in location: {}", locationId);
            if (!locationHasTable(locationId, table)) {
                log.warn("Table does not exist in location");
                return error(400, "Table does not exist for the location");
            }

            log.info("Checking if table has sufficient capacity");
            if (!tableHasCapacity(table, reservation.getNoOfGuests())) {
                log.warn("Table does not have enough capacity");
                return error(400, "No of guests cannot be more than the table capacity");
            }

            log.info("Getting target slot for new time range");
            String targetSlot = getExactSlot(timeFrom, timeTo);
            if (targetSlot == null) {
                log.warn("Invalid time slot range");
                return Map.of("statusCode", 400, "message", "timeFrom and timeTo do not match any valid slot");
            }

            log.info("Getting slot indices");
            String slotFrom = getSlotByStartTime(timeFrom);
            String slotTo = getSlotByEndTime(timeTo);
            int s = extractSlotIndex(slotFrom);
            int e = extractSlotIndex(slotTo);

            for (int i = s; i <= e; i++) {
                String slot = "slot" + i;
                String newId = date + locationId + table + slot;

                log.warn("Checking if slot is already reserved: {}", newId);
                if (isSlotReserved(newId)) {
                    log.warn("Slot already reserved: {}", newId);
                    return error(409, "Table already Reserved!");
                }

                log.info("Creating new reservation with ID: {}", newId);
                Reservation newReservation = new Reservation();
                newReservation.setId(newId);
                newReservation.setUserId(reservation.getUserId());
                newReservation.setStatus("Reserved");
                newReservation.setLocationAddress(locationId);
                newReservation.setTableId(table);
                newReservation.setDate(date);
                newReservation.setTimeSlot(slot);
                newReservation.setPreOrder(reservation.getPreOrder());
                newReservation.setNoOfGuests(reservation.getNoOfGuests());
                newReservation.setFeedbackId("");
                newReservation.setBookedAt(reservation.getBookedAt());
                newReservation.setWaiterId(reservation.getWaiterId());
                newReservation.setBookedBy(reservation.getBookedBy());

                reservationDynamoDbTable.putItem(PutItemEnhancedRequest.builder(Reservation.class).item(newReservation).build());
                waiter.getReservationIds().add(newId);
            }

            log.info("Deleting old reservation: {}", reservationId);
            reservationDynamoDbTable.deleteItem(Key.builder().partitionValue(reservationId).build());

            log.info("Removing reservation from waiter");
            waiter.getReservationIds().remove(reservationId);
            waiterDynamoDbTable.updateItem(waiter);

            log.info("Reservation updated successfully");
            return Map.of("statusCode", 200, "message", "Reservation updated successfully");
        } catch (Exception e) {
            log.warn("Exception occurred: {}", e.getMessage());
            return Map.of("statusCode", 500, "message", "Something went wrong while updating the reservation");
        }
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


    private String getExactSlot(String timeFrom, String timeTo) {
        for (Map.Entry<String, List<String>> entry : TIME_SLOT_MAP.entrySet()) {
            String start = entry.getValue().get(0);
            String end = entry.getValue().get(1);

            if (start.equals(timeFrom) && end.equals(timeTo)) {
                return entry.getKey();
            }
        }
        return null;
    }


    /**
     * Checks if a slot reservation already exists and is marked as reserved.
     */
    private boolean isSlotReserved(String reservationId) {
        Reservation existing = reservationDynamoDbTable.getItem(Key.builder().partitionValue(reservationId).build());
        return existing != null && "Reserved".equalsIgnoreCase(existing.getStatus());
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

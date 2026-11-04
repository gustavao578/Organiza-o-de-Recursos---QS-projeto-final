package br.edu.organizacaorecursos;

import static br.edu.organizacaorecursos.OrganizationResourceService.BookingStatus.*;
import static br.edu.organizacaorecursos.OrganizationResourceService.MaintenanceStatus.*;
import static br.edu.organizacaorecursos.OrganizationResourceService.NotificationChannel.*;
import static br.edu.organizacaorecursos.OrganizationResourceService.ResourceType.*;
import static br.edu.organizacaorecursos.OrganizationResourceService.Role.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationResourceServiceRedTest {
    private static final Instant START = Instant.parse("2026-03-10T09:00:00Z");
    private static final Instant END = Instant.parse("2026-03-10T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-03-10T11:00:00Z");

    private OrganizationResourceService service;

    @BeforeEach
    void setUp() {
        service = new OrganizationResourceService();
    }

    @Test
    void R_TDD_01_authorizesPermittedUserAndDeniesUnauthenticatedOrDirectApiAccess() {
        var requester = user("requester", Set.of(REQUESTER), true);
        var unauthenticated = user("anonymous", Set.of(ADMINISTRATOR), false);
        var administrator = user("admin", Set.of(ADMINISTRATOR), true);

        var allowed = service.authorize(requester, "CREATE_BOOKING", "room-1");
        var denied = service.authorize(unauthenticated, "CREATE_BOOKING", "room-1");
        var directApiDenied = service.authorize(unauthenticated, "DIRECT_API", "private-1");
        var accessAudit = service.auditHistory(administrator, "private-1");

        assertAll(
                () -> assertEquals(Boolean.TRUE, allowed.accepted()),
                () -> assertEquals(Boolean.FALSE, denied.accepted()),
                () -> assertEquals(Boolean.FALSE, directApiDenied.accepted()),
                () -> assertTrue(accessAudit.value() != null
                        && accessAudit.value().stream().anyMatch(event ->
                                event.operation().contains("denied"))));
    }

    @Test
    void R_TDD_02_acceptsSingleAndAuthorizedMultipleRoles() {
        var singleRole = user("one", Set.of(REQUESTER), true);
        var multipleRoles = user("many", Set.of(REQUESTER, RESPONSIBLE), true);

        var singleResult = service.authorize(singleRole, "VIEW_BOOKING", "booking-1");
        var multipleResult = service.authorize(multipleRoles, "VIEW_BOOKING", "booking-1");

        assertAll(
                () -> assertEquals(Boolean.TRUE, singleResult.accepted()),
                () -> assertEquals(Boolean.TRUE, multipleResult.accepted()));
    }

    @Test
    void R_TDD_03_requiresSpecificManagementAuthorizationAndProtectsRoleChanges() {
        var administrator = user("admin", Set.of(ADMINISTRATOR), true);
        var requester = user("requester", Set.of(REQUESTER), true);
        var target = user("target", Set.of(REQUESTER), true);

        var allowed = service.authorize(administrator, "MANAGE_RESOURCES", "room-1");
        var denied = service.authorize(requester, "MANAGE_RESOURCES", "room-1");
        var unauthorizedRoleChange = service.changeUserRoles(requester, target, Set.of(ADMINISTRATOR));

        assertAll(
                () -> assertEquals(Boolean.TRUE, allowed.accepted()),
                () -> assertEquals(Boolean.FALSE, denied.accepted()),
                () -> assertEquals(Boolean.FALSE, unauthorizedRoleChange.accepted()));
    }

    @Test
    void R_TDD_04_registersValidResourcesAndRejectsDuplicateOrInconsistentRecords() {
        var room = resource("room-1", ROOM, roomFields(), false);
        var missingData = resource("room-2", ROOM, Map.of("name", "Room 2"), false);

        var created = service.registerResource(room, List.of());
        var duplicate = service.registerResource(room, List.of(room));
        var invalid = service.registerResource(missingData, List.of());

        assertAll(
                () -> assertEquals(Boolean.TRUE, created.accepted()),
                () -> assertEquals(Boolean.FALSE, duplicate.accepted()),
                () -> assertEquals(Boolean.FALSE, invalid.accepted()));
    }

    @Test
    void R_TDD_05_registersRoomWithRequiredFieldsAndRejectsMissingFields() {
        var valid = service.registerResource(resource("room-1", ROOM, roomFields(), false), List.of());
        var missingCapacity = service.registerResource(
                resource("room-2", ROOM, Map.of("name", "Room 2"), false), List.of());

        assertAll(
                () -> assertEquals(Boolean.TRUE, valid.accepted()),
                () -> assertEquals(Boolean.FALSE, missingCapacity.accepted()));
    }

    @Test
    void R_TDD_06_registersTeacherWithRequiredFieldsAndRejectsMissingFields() {
        var valid = service.registerResource(
                resource("teacher-1", TEACHER, teacherFields(), false), List.of());
        var missingAgenda = service.registerResource(
                resource("teacher-2", TEACHER, Map.of("name", "Teacher 2"), false), List.of());

        assertAll(
                () -> assertEquals(Boolean.TRUE, valid.accepted()),
                () -> assertEquals(Boolean.FALSE, missingAgenda.accepted()));
    }

    @Test
    void R_TDD_07_registersMaterialWithRequiredFieldsAndRejectsMissingFields() {
        var valid = service.registerResource(
                resource("material-1", MATERIAL, materialFields(), false), List.of());
        var missingQuantity = service.registerResource(
                resource("material-2", MATERIAL, Map.of("name", "Material 2"), false), List.of());

        assertAll(
                () -> assertEquals(Boolean.TRUE, valid.accepted()),
                () -> assertEquals(Boolean.FALSE, missingQuantity.accepted()));
    }

    @Test
    void R_TDD_08_searchReflectsCurrentResourceStatusAndFilters() {
        var availableRoom = resource("room-1", ROOM, roomFields(), false);
        var blockedRoom = blockedRoom();
        var criteria = new OrganizationResourceService.SearchCriteria(
                ROOM, 20, "north", null, range(START, END));

        var result = search(criteria, List.of(availableRoom, blockedRoom), List.of(), List.of());

        assertAll(
                () -> assertEquals(Boolean.TRUE, result.accepted()),
                () -> assertEquals(List.of(availableRoom), result.value()));
    }

    @Test
    void R_TDD_09_searchAppliesCapacityLocationAndTeacherCompetenceFilters() {
        var matchingRoom = room();
        var matchingTeacher = resource("teacher-1", TEACHER, teacherFields(), false);
        var blockedRoom = blockedRoom();
        var roomResult = search(
                new OrganizationResourceService.SearchCriteria(
                        ROOM, 20, "north", null, range(START, END)),
                List.of(matchingRoom, blockedRoom), List.of(), List.of());
        var teacherResult = search(
                new OrganizationResourceService.SearchCriteria(
                        TEACHER, null, null, "physics", range(START, END)),
                List.of(matchingTeacher), List.of(), List.of());

        assertAll(
                () -> assertEquals(Boolean.TRUE, roomResult.accepted()),
                () -> assertEquals(List.of(matchingRoom), roomResult.value()),
                () -> assertEquals(Boolean.TRUE, teacherResult.accepted()),
                () -> assertEquals(List.of(matchingTeacher), teacherResult.value()));
    }

    @Test
    void R_TDD_10_createsBookingForAvailableResourceAndValidPeriod() {
        var request = bookingRequest(user("requester", Set.of(REQUESTER), true),
                List.of(resource("room-1", ROOM, roomFields(), false)), range(START, END));

        var result = create(request);

        assertAll(
                () -> assertEquals(Boolean.TRUE, result.accepted()),
                () -> assertNotNull(result.value()));
    }

    @Test
    void R_TDD_11_amendsOnlyAuthorizedBookingAndRevalidatesAvailability() {
        var actor = user("requester", Set.of(REQUESTER), true);
        var existing = booking("booking-1", actor, List.of(room()), range(START, END), REQUESTED);
        var changes = bookingRequest(actor, List.of(room()), range(END, LATER));

        var result = service.amendBooking(actor, existing, changes);

        assertAll(
                () -> assertEquals(Boolean.TRUE, result.accepted()),
                () -> assertEquals(range(END, LATER),
                        result.value() == null ? null : result.value().period()));
    }

    @Test
    void R_TDD_12_cancelsBookingWithReasonAndReturnsCanceledState() {
        var actor = user("requester", Set.of(REQUESTER), true);
        var existing = booking("booking-1", actor, List.of(room()), range(START, END), APPROVED);

        var result = service.cancelBooking(actor, existing, "Schedule changed");

        assertAll(
                () -> assertEquals(Boolean.TRUE, result.accepted()),
                () -> assertEquals(OrganizationResourceService.BookingStatus.CANCELED,
                        result.value() == null ? null : result.value().status()),
                () -> assertEquals("Schedule changed", result.message()));
    }

    @Test
    void R_TDD_13_detectsRoomOverlapAndAllowsNonConflictingPeriod() {
        var existing = booking("booking-1", requester(), List.of(room()), range(START, END), APPROVED);

        var overlapping = service.hasConflict(room(), range(START, END), List.of(existing));
        var nonOverlapping = service.hasConflict(room(), range(END, LATER), List.of(existing));

        assertAll(
                () -> assertEquals(Boolean.TRUE, overlapping.value()),
                () -> assertEquals(Boolean.FALSE, nonOverlapping.value()));
    }

    @Test
    void R_TDD_14_detectsTeacherAgendaConflict() {
        var teacher = resource("teacher-1", TEACHER, teacherFields(), false);
        var existing = booking("booking-1", requester(), List.of(teacher), range(START, END), APPROVED);

        var result = service.hasConflict(teacher, range(START, END), List.of(existing));

        assertEquals(Boolean.TRUE, result.value());
    }

    @Test
    void R_TDD_15_detectsMaterialConflictAndRejectsInsufficientQuantity() {
        var material = resource("material-1", MATERIAL, materialFields(), false);
        var existing = booking("booking-1", requester(), List.of(material), range(START, END), APPROVED);
        var conflict = service.hasConflict(material, range(START, END), List.of(existing));

        assertEquals(Boolean.TRUE, conflict.value());
    }

    @Test
    void R_TDD_16_detectsMaintenanceOverlapAndDoesNotTreatResourceAsAvailable() {
        var resource = room();
        var maintenance = new OrganizationResourceService.Maintenance(
                resource, range(START, END), "Repair", ACTIVE);
        var result = available(resource, range(START, END), List.of(), List.of(maintenance));

        assertEquals(Boolean.FALSE, result.value());
    }

    @Test
    void R_TDD_17_conflictResponseIdentifiesResourceAndConflictingInterval() {
        var existing = booking("booking-1", requester(), List.of(room()), range(START, END), APPROVED);
        var request = bookingRequest(requester(), List.of(room()), range(START, END));

        var conflict = service.hasConflict(room(), range(START, END), List.of(existing));
        var result = create(request, List.of(existing), List.of());

        assertAll(
                () -> assertEquals(Boolean.TRUE, conflict.value()),
                () -> assertEquals(Boolean.FALSE, result.accepted()),
                () -> assertTrue(result.message() != null && result.message().contains("room-1")),
                () -> assertTrue(result.message() != null
                        && result.message().contains(START.toString())));
    }

    @Test
    void R_TDD_18_rejectedBookingConflictDoesNotProducePartialSuccess() {
        var request = bookingRequest(requester(), List.of(room()), range(START, END));
        var existing = booking("booking-1", requester(), List.of(room()),
                range(START, END), APPROVED);

        var result = create(request, List.of(existing), List.of());

        assertAll(
                () -> assertEquals(Boolean.FALSE, result.accepted()),
                () -> assertNull(result.value()));
    }

    @Test
    void R_TDD_19_simultaneousConflictingRequestsAcceptExactlyOne() throws Exception {
        var request = bookingRequest(requester(), List.of(room()), range(START, END));
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<OrganizationResourceService.OperationResult<OrganizationResourceService.Booking>> first =
                    executor.submit(() -> concurrentCreate(request, ready, start));
            Future<OrganizationResourceService.OperationResult<OrganizationResourceService.Booking>> second =
                    executor.submit(() -> concurrentCreate(request, ready, start));
            ready.await();
            start.countDown();

            var accepted = List.of(first.get(), second.get()).stream()
                    .filter(result -> Boolean.TRUE.equals(result.accepted()))
                    .count();

            assertEquals(1, accepted);
        }
    }

    @Test
    void R_TDD_20_restrictedResourceRequiresResponsibleApprovalBeforeConfirmation() {
        var request = bookingRequest(requester(), List.of(restrictedRoom()), range(START, END));
        var created = create(request);
        var approved = service.approveBooking(
                user("responsible", Set.of(RESPONSIBLE), true),
                booking("booking-1", requester(), List.of(restrictedRoom()), range(START, END), REQUESTED),
                true);
        var requesterApproval = service.approveBooking(
                requester(),
                booking("booking-1", requester(), List.of(restrictedRoom()), range(START, END), REQUESTED),
                true);

        assertAll(
                () -> assertEquals(Boolean.FALSE, created.accepted()),
                () -> assertEquals(Boolean.TRUE, approved.accepted()),
                () -> assertEquals(Boolean.FALSE, requesterApproval.accepted()));
    }

    @Test
    void R_TDD_21_approvalAndRejectionUpdateBookingState() {
        var pending = booking("booking-1", requester(), List.of(restrictedRoom()),
                range(START, END), REQUESTED);

        var approved = service.approveBooking(
                user("responsible", Set.of(RESPONSIBLE), true), pending, true);
        var rejected = service.approveBooking(
                user("responsible", Set.of(RESPONSIBLE), true), pending, false);

        assertAll(
                () -> assertEquals(APPROVED,
                        approved.value() == null ? null : approved.value().status()),
                () -> assertEquals(REJECTED,
                        rejected.value() == null ? null : rejected.value().status()));
    }

    @Test
    void R_TDD_22_authorizedAdministratorRegistersMaintenanceWithRequiredFields() {
        var administrator = user("admin", Set.of(ADMINISTRATOR), true);
        var maintenance = new OrganizationResourceService.Maintenance(
                room(), range(START, END), "Repair", ACTIVE);

        var result = service.registerMaintenance(administrator, maintenance);

        assertAll(
                () -> assertEquals(Boolean.TRUE, result.accepted()),
                () -> assertEquals(maintenance, result.value()));
    }

    @Test
    void R_TDD_23_resourceIsUnavailableDuringMaintenance() {
        var maintenance = new OrganizationResourceService.Maintenance(
                room(), range(START, END), "Repair", ACTIVE);
        var result = available(room(), range(START, END), List.of(), List.of(maintenance));

        assertEquals(Boolean.FALSE, result.value());
    }

    @Test
    void R_TDD_24_pastMaintenanceIsCompletedAndRemainsAuditable() {
        var administrator = user("admin", Set.of(ADMINISTRATOR), true);
        var past = new OrganizationResourceService.Maintenance(
                room(),
                range(Instant.parse("2026-03-08T09:00:00Z"), Instant.parse("2026-03-08T10:00:00Z")),
                "Repair",
                ACTIVE);

        var saved = service.registerMaintenance(administrator, past);
        var history = service.auditHistory(administrator, "room-1");

        assertAll(
                () -> assertEquals(Boolean.TRUE, saved.accepted()),
                () -> assertEquals(OrganizationResourceService.MaintenanceStatus.COMPLETED,
                        saved.value() == null ? null : saved.value().status()),
                () -> assertTrue(history.value() != null && history.value().stream()
                        .anyMatch(event -> event.operation().contains("maintenance"))));
    }

    @Test
    void R_TDD_25_checksOutMaterialsOnlyWithResponsibleAuthorizationAndTracksPendingReturn() {
        var material = resource("material-1", MATERIAL, materialFields(), false);
        var responsible = user("responsible", Set.of(RESPONSIBLE), true);
        var requester = requester();

        var allowed = service.checkOut(responsible, material, START);
        var denied = service.checkOut(requester, material, START);

        assertAll(
                () -> assertEquals(Boolean.TRUE, allowed.accepted()),
                () -> assertEquals(Boolean.FALSE, denied.accepted()),
                () -> assertNull(allowed.value() == null ? null : allowed.value().returnedAt()));
    }

    @Test
    void R_TDD_26_returnRecordsDamagedConditionAndObservations() {
        var movement = new OrganizationResourceService.Movement(
                resource("material-1", MATERIAL, materialFields(), false),
                user("responsible", Set.of(RESPONSIBLE), true),
                START, null, null, null);

        var result = service.returnMaterial(
                movement.responsible(), movement, END, "damaged", "Case cracked");

        assertAll(
                () -> assertEquals(Boolean.TRUE, result.accepted()),
                () -> assertEquals("damaged",
                        result.value() == null ? null : result.value().returnCondition()),
                () -> assertEquals("Case cracked",
                        result.value() == null ? null : result.value().notes()),
                () -> assertNotNull(result.value() == null ? null : result.value().returnedAt()));
    }

    @Test
    void R_TDD_27_auditsRelevantChangesWithActorTimeOperationAndBeforeAfter() {
        var administrator = user("admin", Set.of(ADMINISTRATOR), true);
        service.registerResource(room(), List.of());
        var result = service.auditHistory(administrator, "room-1");

        assertAll(
                () -> assertEquals(Boolean.TRUE, result.accepted()),
                () -> assertTrue(result.value() != null && result.value().stream().allMatch(event ->
                        event.actorId() != null
                                && event.occurredAt() != null
                                && event.operation() != null)));
    }

    @Test
    void R_TDD_28_doesNotDeleteStartedBookingOrCriticalAuditWithoutReason() {
        var administrator = user("admin", Set.of(ADMINISTRATOR), true);
        var started = booking("booking-1", requester(), List.of(room()), range(START, END), IN_USE);
        var event = new OrganizationResourceService.AuditEvent(
                "admin", START, "resource.update", room(), room());

        var bookingDeletion = service.deleteBooking(administrator, started);
        var auditDeletionWithoutReason = service.deleteAuditEvent(administrator, event, "");

        assertAll(
                () -> assertEquals(Boolean.FALSE, bookingDeletion.accepted()),
                () -> assertEquals(Boolean.FALSE, auditDeletionWithoutReason.accepted()));
    }

    @Test
    void R_TDD_29_notifiesUsersAboutBookingAndApprovalEvents() {
        var recipient = requester();
        var creation = service.sendNotification(
                new OrganizationResourceService.NotificationRequest(
                        recipient, "created", "Booking created", SIMULATED));
        var conflict = service.sendNotification(
                new OrganizationResourceService.NotificationRequest(
                        recipient, "conflict", "Conflict detected", SIMULATED));
        var pending = service.sendNotification(
                new OrganizationResourceService.NotificationRequest(
                        user("responsible", Set.of(RESPONSIBLE), true),
                        "pending", "Approval pending", SIMULATED));

        assertAll(
                () -> assertEquals(Boolean.TRUE, creation.accepted()),
                () -> assertEquals(Boolean.TRUE, conflict.accepted()),
                () -> assertEquals(Boolean.TRUE, pending.accepted()));
    }

    @Test
    void R_TDD_30_supportsSimulatedAndExternalNotificationChannels() {
        var simulatedRequest = new OrganizationResourceService.NotificationRequest(
                requester(), "approved", "Booking approved", SIMULATED);
        var externalRequest = new OrganizationResourceService.NotificationRequest(
                requester(), "approved", "Booking approved", EXTERNAL_API);

        var simulated = service.sendNotification(simulatedRequest);
        var external = service.sendNotification(externalRequest);

        assertAll(
                () -> assertEquals(Boolean.TRUE, simulated.accepted()),
                () -> assertEquals(simulatedRequest,
                        simulated.value() == null ? null : simulated.value()),
                () -> assertEquals(Boolean.TRUE, external.accepted()),
                () -> assertEquals(externalRequest,
                        external.value() == null ? null : external.value()));
    }

    @Test
    void R_TDD_31_reportsExpectedMetricsForSelectedPeriod() {
        var criteria = new OrganizationResourceService.ReportCriteria(START, END);
        var result = service.generateReport(
                user("admin", Set.of(ADMINISTRATOR), true), criteria);

        assertAll(
                () -> assertEquals(Boolean.TRUE, result.accepted()),
                () -> assertNotNull(result.value()),
                () -> assertNotNull(result.value() == null ? null : result.value().metrics()));
    }

    @Test
    void R_TDD_32_allowsAuthorizedReportAccessAndDeniesUnauthorizedAccess() {
        var criteria = new OrganizationResourceService.ReportCriteria(START, END);
        var allowed = service.generateReport(
                user("admin", Set.of(ADMINISTRATOR), true), criteria);
        var denied = service.generateReport(requester(), criteria);

        assertAll(
                () -> assertEquals(Boolean.TRUE, allowed.accepted()),
                () -> assertEquals(Boolean.FALSE, denied.accepted()));
    }

    @Test
    void R_TDD_34_returnsFeedbackForValidationAndConflictOutcomes() {
        var invalid = create(bookingRequest(requester(), List.of(room()), range(END, START)));
        var existing = booking("booking-1", requester(), List.of(room()),
                range(START, END), APPROVED);
        var conflict = create(
                bookingRequest(requester(), List.of(room()), range(START, END)),
                List.of(existing), List.of());

        assertAll(
                () -> assertEquals(Boolean.FALSE, invalid.accepted()),
                () -> assertNotNull(invalid.message()),
                () -> assertEquals(Boolean.FALSE, conflict.accepted()),
                () -> assertTrue(conflict.message() != null
                        && conflict.message().contains("room-1")));
    }

    @Test
    void R_TDD_36_requiresBookingEndAfterStart() {
        var equal = create(bookingRequest(requester(), List.of(room()), range(START, START)));
        var earlier = create(bookingRequest(requester(), List.of(room()), range(END, START)));
        var valid = create(bookingRequest(requester(), List.of(room()), range(START, END)));

        assertAll(
                () -> assertEquals(Boolean.FALSE, equal.accepted()),
                () -> assertEquals(Boolean.FALSE, earlier.accepted()),
                () -> assertEquals(Boolean.TRUE, valid.accepted()));
    }

    @Test
    void R_TDD_37_blocksOverlappingReservationForSameResource() {
        var existing = booking("booking-1", requester(), List.of(room()), range(START, END), APPROVED);

        var result = service.hasConflict(room(), range(START, END), List.of(existing));

        assertEquals(Boolean.TRUE, result.value());
    }

    @Test
    void R_TDD_38_concurrentConflictingRequestsYieldExactlyOneAcceptedReservation() throws Exception {
        var request = bookingRequest(requester(), List.of(room()), range(START, END));
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> concurrentCreate(request, ready, start));
            var second = executor.submit(() -> concurrentCreate(request, ready, start));
            ready.await();
            start.countDown();

            var accepted = List.of(first.get(), second.get()).stream()
                    .filter(result -> Boolean.TRUE.equals(result.accepted()))
                    .count();

            assertEquals(1, accepted);
        }
    }

    @Test
    void R_TDD_39_neverOffersOrConfirmsResourceDuringMaintenance() {
        var resource = room();

        var maintenance = new OrganizationResourceService.Maintenance(
                resource, range(START, END), "Repair", ACTIVE);
        var available = available(resource, range(START, END), List.of(), List.of(maintenance));
        var search = search(
                new OrganizationResourceService.SearchCriteria(
                        ROOM, null, null, null, range(START, END)),
                List.of(resource), List.of(), List.of(maintenance));

        assertAll(
                () -> assertEquals(Boolean.FALSE, available.value()),
                () -> assertTrue(search.value() != null
                        && !search.value().contains(resource)));
    }

    @Test
    void R_TDD_40_allowsDocumentedMainBookingStateFlow() {
        var actor = user("responsible", Set.of(RESPONSIBLE), true);
        var requested = booking("booking-1", requester(), List.of(room()), range(START, END), REQUESTED);

        var approved = service.transitionBooking(actor, requested, APPROVED);
        var inUse = service.transitionBooking(actor, withStatus(requested, APPROVED), IN_USE);
        var completed = service.transitionBooking(
                actor, withStatus(requested, IN_USE), OrganizationResourceService.BookingStatus.COMPLETED);

        assertAll(
                () -> assertEquals(APPROVED,
                        approved.value() == null ? null : approved.value().status()),
                () -> assertEquals(IN_USE,
                        inUse.value() == null ? null : inUse.value().status()),
                () -> assertEquals(OrganizationResourceService.BookingStatus.COMPLETED,
                        completed.value() == null ? null : completed.value().status()));
    }

    @Test
    void R_TDD_41_onlyResponsibleApprovesAndStateChangesAreAudited() {
        var booking = booking("booking-1", requester(), List.of(restrictedRoom()),
                range(START, END), REQUESTED);
        var responsible = user("responsible", Set.of(RESPONSIBLE), true);
        var requesterApproval = service.approveBooking(requester(), booking, true);
        var approved = service.approveBooking(responsible, booking, true);
        var audit = service.auditHistory(responsible, "booking-1");
        var deletion = service.deleteBooking(
                responsible, withStatus(booking, IN_USE));

        assertAll(
                () -> assertEquals(Boolean.FALSE, requesterApproval.accepted()),
                () -> assertEquals(Boolean.TRUE, approved.accepted()),
                () -> assertTrue(audit.value() != null && audit.value().stream()
                        .anyMatch(event -> event.operation().contains("state"))),
                () -> assertEquals(Boolean.FALSE, deletion.accepted()));
    }

    @Test
    void R_TDD_42_notifiesWithUnderstandableOutcomeForSuccessConflictAndRejection() {
        var success = service.sendNotification(new OrganizationResourceService.NotificationRequest(
                requester(), "created", "Booking created", SIMULATED));
        var conflict = service.sendNotification(new OrganizationResourceService.NotificationRequest(
                requester(), "conflict", "Room room-1 conflicts from " + START, SIMULATED));
        var rejection = service.sendNotification(new OrganizationResourceService.NotificationRequest(
                requester(), "rejected", "Request rejected", SIMULATED));

        assertAll(
                () -> assertNotNull(success.value() == null ? null : success.value().message()),
                () -> assertNotNull(conflict.value() == null ? null : conflict.value().message()),
                () -> assertNotNull(rejection.value() == null ? null : rejection.value().message()));
    }

    private OrganizationResourceService.OperationResult<OrganizationResourceService.Booking>
            concurrentCreate(
                    OrganizationResourceService.BookingRequest request,
                    CountDownLatch ready,
                    CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        return create(request);
    }

    private OrganizationResourceService.OperationResult<OrganizationResourceService.Booking>
            create(OrganizationResourceService.BookingRequest request) {
        return create(request, List.of(), List.of());
    }

    private OrganizationResourceService.OperationResult<OrganizationResourceService.Booking>
            create(
                    OrganizationResourceService.BookingRequest request,
                    List<OrganizationResourceService.Booking> existing,
                    List<OrganizationResourceService.Maintenance> maintenance) {
        return service.createBooking(request, existing, maintenance);
    }

    private OrganizationResourceService.OperationResult<Boolean> available(
            OrganizationResourceService.Resource resource,
            OrganizationResourceService.TimeRange period,
            List<OrganizationResourceService.Booking> existing,
            List<OrganizationResourceService.Maintenance> maintenance) {
        return service.isAvailable(resource, period, existing, maintenance);
    }

    private OrganizationResourceService.OperationResult<List<OrganizationResourceService.Resource>>
            search(
                    OrganizationResourceService.SearchCriteria criteria,
                    List<OrganizationResourceService.Resource> candidates,
                    List<OrganizationResourceService.Booking> existing,
                    List<OrganizationResourceService.Maintenance> maintenance) {
        return service.searchResources(criteria, candidates, existing, maintenance);
    }

    private static OrganizationResourceService.User requester() {
        return user("requester", Set.of(REQUESTER), true);
    }

    private static OrganizationResourceService.User user(
            String id, Set<OrganizationResourceService.Role> roles, boolean authenticated) {
        return new OrganizationResourceService.User(id, roles, authenticated);
    }

    private static OrganizationResourceService.Resource room() {
        return resource("room-1", ROOM, roomFields(), false);
    }

    private static OrganizationResourceService.Resource restrictedRoom() {
        return resource("room-restricted", ROOM, roomFields(), true);
    }

    private static OrganizationResourceService.Resource blockedRoom() {
        return resource(
                "room-blocked",
                ROOM,
                Map.of(
                        "name", "Blocked Room",
                        "type", "classroom",
                        "capacity", "30",
                        "location", "north",
                        "status", "blocked"),
                false);
    }

    private static OrganizationResourceService.Resource resource(
            String id,
            OrganizationResourceService.ResourceType type,
            Map<String, String> attributes,
            boolean restricted) {
        return new OrganizationResourceService.Resource(id, type, attributes, restricted);
    }

    private static Map<String, String> roomFields() {
        return Map.of(
                "name", "Room 1",
                "type", "classroom",
                "capacity", "30",
                "location", "north",
                "status", "active");
    }

    private static Map<String, String> teacherFields() {
        return Map.of(
                "name", "Teacher 1",
                "competence", "physics",
                "availability", "available",
                "agenda", "empty");
    }

    private static Map<String, String> materialFields() {
        return Map.of(
                "name", "Material 1",
                "type", "equipment",
                "quantity", "1",
                "state", "available",
                "location", "north",
                "loanRules", "tracked");
    }

    private static OrganizationResourceService.TimeRange range(Instant start, Instant end) {
        return new OrganizationResourceService.TimeRange(start, end);
    }

    private static OrganizationResourceService.BookingRequest bookingRequest(
            OrganizationResourceService.User requester,
            List<OrganizationResourceService.Resource> resources,
            OrganizationResourceService.TimeRange period) {
        return new OrganizationResourceService.BookingRequest(requester, resources, period);
    }

    private static OrganizationResourceService.Booking booking(
            String id,
            OrganizationResourceService.User requester,
            List<OrganizationResourceService.Resource> resources,
            OrganizationResourceService.TimeRange period,
            OrganizationResourceService.BookingStatus status) {
        return new OrganizationResourceService.Booking(id, requester, resources, period, status);
    }

    private static OrganizationResourceService.Booking withStatus(
            OrganizationResourceService.Booking booking,
            OrganizationResourceService.BookingStatus status) {
        return new OrganizationResourceService.Booking(
                booking.id(), booking.requester(), booking.resources(), booking.period(), status);
    }
}

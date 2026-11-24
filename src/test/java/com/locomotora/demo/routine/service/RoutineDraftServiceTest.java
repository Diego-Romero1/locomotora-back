package com.locomotora.demo.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.RoutineDraftDtos.RoutineDraftRequest;
import com.locomotora.demo.routine.dto.RoutineDraftDtos.RoutineDraftResponse;
import com.locomotora.demo.routine.repository.RoutineDraftRepository;
import com.locomotora.demo.routine.repository.RoutineDraftRepository.DraftRow;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RoutineDraftServiceTest {
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID draftId = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private final UUID routineId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private final ObjectMapper json = new ObjectMapper();

    @Mock
    private RoutineDraftRepository repository;

    private RoutineDraftService service;

    @BeforeEach
    void setUp() {
        service = new RoutineDraftService(repository);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ObjectNode payload() {
        ObjectNode node = json.createObjectNode();
        node.put("title", "Torso/Pierna");
        return node;
    }

    private DraftRow stored(DraftRow row) {
        return row;
    }

    @Test
    void savesANewDraftClampingTheSummaryAndReturnsWhatIsStored() {
        when(repository.findById(draftId)).thenReturn(Optional.empty());
        when(repository.countByUser(userId)).thenReturn(0);
        ArgumentCaptor<DraftRow> saved = ArgumentCaptor.forClass(DraftRow.class);
        when(repository.upsert(saved.capture())).thenReturn(true);

        RoutineDraftRequest request = new RoutineDraftRequest(
                "new", routineId.toString(), "  Torso/Pierna  ", 9, 12, 500, payload(), "2026-11-23T10:00:00Z");
        when(repository.findById(draftId)).thenAnswer(invocation -> saved.getAllValues().isEmpty()
                ? Optional.empty()
                : Optional.of(stored(saved.getValue())));

        RoutineDraftResponse response = service.save(draftId, request);

        DraftRow row = saved.getValue();
        assertEquals("NEW", row.kind());
        // Una rutina nueva no queda atada a una existente aunque la app mande el id.
        assertNull(row.routineId());
        assertEquals("Torso/Pierna", row.title());
        assertEquals(3, row.step());
        assertEquals(7, row.dayCount());
        assertEquals(140, row.exerciseCount());
        assertEquals(Instant.parse("2026-11-23T10:00:00Z"), row.updatedAt());
        assertEquals("Torso/Pierna", response.payload().get("title").asText());
    }

    @Test
    void editingADraftNeedsAnActiveRoutineOfTheUser() {
        when(repository.isActiveRoutineOf(routineId, userId)).thenReturn(false);
        RoutineDraftRequest request = new RoutineDraftRequest("EDIT", routineId.toString(), null, 1, 2, 4, payload(), null);

        ApiException exception = assertThrows(ApiException.class, () -> service.save(draftId, request));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        verify(repository, never()).upsert(any());
    }

    @Test
    void rejectsBadKindsPayloadsAndTooManyDrafts() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.save(draftId, new RoutineDraftRequest("COPY", null, null, 1, 0, 0, payload(), null))).status());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.save(draftId, new RoutineDraftRequest("NEW", null, null, 1, 0, 0, json.createArrayNode(), null))).status());

        when(repository.findById(draftId)).thenReturn(Optional.empty());
        when(repository.countByUser(userId)).thenReturn(RoutineDraftService.MAX_DRAFTS);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class,
                () -> service.save(draftId, new RoutineDraftRequest("NEW", null, null, 1, 0, 0, payload(), null))).status());
        verify(repository, never()).upsert(any());
    }

    @Test
    void aDraftOfAnotherUserIsNotFound() {
        UUID other = UUID.randomUUID();
        when(repository.findById(draftId)).thenReturn(Optional.of(
                new DraftRow(draftId, other, "NEW", null, "Ajena", 1, 1, 1, "{}", Instant.now())));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.save(draftId, new RoutineDraftRequest("NEW", null, null, 1, 0, 0, payload(), null)));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        verify(repository, never()).upsert(any());
    }
}

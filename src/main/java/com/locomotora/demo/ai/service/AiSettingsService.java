package com.locomotora.demo.ai.service;

import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.config.AiModelCatalog;
import com.locomotora.demo.ai.config.AiModelCatalog.Feature;
import com.locomotora.demo.ai.config.AiProperties;
import com.locomotora.demo.ai.dto.AiModelsResponse;
import com.locomotora.demo.ai.dto.AiPreferencesRequest;
import com.locomotora.demo.ai.repository.AiRepository;
import com.locomotora.demo.ai.repository.AiRepository.ModelChoice;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Que modelo usa cada usuario para cada tarea, y cuanto se usa la IA (por usuario y en toda la app). */
@Service
public class AiSettingsService {
    private final AiModelCatalog catalog;
    private final AiProperties properties;
    private final AiRepository aiRepository;
    private final AiModelClient client;

    public AiSettingsService(AiModelCatalog catalog, AiProperties properties, AiRepository aiRepository, AiModelClient client) {
        this.catalog = catalog;
        this.properties = properties;
        this.aiRepository = aiRepository;
        this.client = client;
    }

    public AiModelsResponse models() {
        return models(CurrentUser.id());
    }

    public AiModelsResponse updatePreferences(AiPreferencesRequest request) {
        UUID userId = CurrentUser.id();
        String coach = validated(request.coachModel());
        String routine = validated(request.routineModel());
        aiRepository.saveModelChoice(userId, coach, routine);
        return models(userId);
    }

    /** Modelo a usar: el pedido en la llamada, si no el elegido por el usuario, si no el de por defecto. */
    public String modelFor(UUID userId, Feature feature, String requested) {
        if (requested != null && !requested.isBlank()) {
            String model = requested.trim();
            if (!catalog.isAllowed(model)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Model not available: " + model);
            }
            return model;
        }
        ModelChoice choice = aiRepository.findModelChoice(userId);
        return catalog.resolve(feature, feature == Feature.COACH ? choice.coachModel() : choice.routineModel());
    }

    public boolean strictJson(String model) {
        return catalog.describe(model).strictJson();
    }

    /** 429 si el usuario ya llego al limite diario de esa tarea. */
    public void checkQuota(UUID userId, Feature feature) {
        int limit = limitFor(feature);
        if (limit > 0 && aiRepository.countUsageLastDay(userId, feature.name()) >= limit) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Daily AI limit reached");
        }
    }

    /** true si la app ya gasto su presupuesto diario de tokens (todos los usuarios comparten la clave). */
    public boolean globalBudgetExhausted() {
        long budget = properties.dailyTokenBudget();
        return budget > 0 && aiRepository.sumTokensLastDay() >= budget;
    }

    /** Una fila por mensaje o rutina, con los tokens de todas sus llamadas al modelo. */
    public void recordUsage(UUID userId, Feature feature, String model, long inputTokens, long outputTokens) {
        aiRepository.recordUsage(userId, feature.name(), model, inputTokens, outputTokens);
    }

    private AiModelsResponse models(UUID userId) {
        ModelChoice choice = aiRepository.findModelChoice(userId);
        return new AiModelsResponse(
                client.isConfigured(),
                catalog.selectable(),
                selection(userId, Feature.COACH, choice.coachModel()),
                selection(userId, Feature.ROUTINE, choice.routineModel()),
                catalog.available().stream()
                        .map(model -> new AiModelsResponse.Model(model.id(), model.label(), model.description(), model.speed(), model.cost()))
                        .toList()
        );
    }

    private AiModelsResponse.Selection selection(UUID userId, Feature feature, String userChoice) {
        String selected = catalog.resolve(feature, userChoice);
        String defaultModel = catalog.defaultFor(feature);
        return new AiModelsResponse.Selection(
                selected,
                defaultModel,
                !selected.equals(defaultModel),
                limitFor(feature),
                aiRepository.countUsageLastDay(userId, feature.name())
        );
    }

    private int limitFor(Feature feature) {
        return feature == Feature.COACH ? properties.dailyCoachMessages() : properties.dailyRoutineGenerations();
    }

    /** null = por defecto; un id no permitido es un error del cliente. */
    private String validated(String model) {
        if (model == null || model.isBlank()) {
            return null;
        }
        String trimmed = model.trim();
        if (!catalog.isAllowed(trimmed)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Model not available: " + trimmed);
        }
        return trimmed;
    }
}

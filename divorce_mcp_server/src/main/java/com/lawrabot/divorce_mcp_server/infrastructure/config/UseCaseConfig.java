package com.lawrabot.divorce_mcp_server.infrastructure.config;

import com.lawrabot.divorce_mcp_server.application.port.in.*;
import com.lawrabot.divorce_mcp_server.application.port.out.*;
import com.lawrabot.divorce_mcp_server.application.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Punto de entrada de Spring para la capa de Aplicación.
 * 
 * Los Servicios de Aplicación son clases Java puras (sin @Service) para garantizar
 * la independencia del framework. Este @Configuration es el ÚNICO lugar donde
 * Spring Boot los instancia e inyecta sus dependencias reales.
 *
 * Esto cumple con el Principio de Inversión de Dependencias (DIP) del DDD:
 *   - El Dominio define los Puertos (interfaces).
 *   - La Infraestructura provee los Adaptadores (implementaciones JPA).
 *   - Esta clase "conecta" todo sin contaminar ni el Dominio ni la Aplicación.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public CreateDivorceDossierUseCase createDivorceDossierUseCase(
            IExpedienteRepository expedienteRepo,
            com.lawrabot.divorce_mcp_server.application.port.out.ICitizenRepository citizenRepo) {
        return new CreateDivorceDossierService(expedienteRepo, citizenRepo);
    }

    @Bean
    public ProcessBlsgScrapingResultUseCase processBlsgScrapingResultUseCase(
            IExpedienteRepository expedienteRepo) {
        return new ProcessBlsgScrapingResultService(expedienteRepo);
    }

    @Bean
    public SubmitSocioEconomicEvaluationUseCase submitSocioEconomicEvaluationUseCase(
            IExpedienteRepository expedienteRepo) {
        return new SubmitSocioEconomicEvaluationService(expedienteRepo);
    }

    @Bean
    public SubmitMarriageDetailsUseCase submitMarriageDetailsUseCase(
            IExpedienteRepository expedienteRepo) {
        return new SubmitMarriageDetailsService(expedienteRepo);
    }

    @Bean
    public SubmitChildrenInfoUseCase submitChildrenInfoUseCase(
            IExpedienteRepository expedienteRepo) {
        return new SubmitChildrenInfoService(expedienteRepo);
    }

    @Bean
    public DraftRegulatoryAgreementUseCase draftRegulatoryAgreementUseCase(
            IExpedienteRepository expedienteRepo) {
        return new RegulatoryAgreementService(expedienteRepo);
    }

    @Bean
    public ValidateAgreementLegalityUseCase validateAgreementLegalityUseCase(
            IExpedienteRepository expedienteRepo) {
        return new ValidateAgreementService(expedienteRepo);
    }

    @Bean
    public GetExpedienteCollectionStageUseCase getExpedienteCollectionStageUseCase(
            IExpedienteRepository expedienteRepo) {
        return new GetExpedienteCollectionStageService(expedienteRepo);
    }

    @Bean
    public ManageObservationsUseCase manageObservationsUseCase(
            IObservationRepository observationRepo,
            ITaskRepository taskRepo,
            IExpedienteRepository expedienteRepo,
            WebClient.Builder webClientBuilder,
            @Value("${lawrabot.agent.push-url:http://localhost:18789/push}") String agentPushUrl) {
        return new ObservationService(observationRepo, taskRepo, expedienteRepo, webClientBuilder, agentPushUrl);
    }
}

package com.lawrabot.divorce_mcp_server.application.port.in;

import com.lawrabot.divorce_mcp_server.domain.model.Child;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de Entrada invocada por el Bot al concluir la etapa 3:
 * Identificación de hijos.
 */
public interface SubmitChildrenInfoUseCase {

    /**
     * @param expedienteId ID central del caso.
     * @param children Lista de Hijos a registrar. Lista vacía si no hay hijos.
     */
    void execute(UUID expedienteId, List<Child> children);
}

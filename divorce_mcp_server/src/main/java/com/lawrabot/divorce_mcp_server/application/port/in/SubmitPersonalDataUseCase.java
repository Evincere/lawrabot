package com.lawrabot.divorce_mcp_server.application.port.in;

import com.lawrabot.divorce_mcp_server.domain.enums.CaseRole;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;

/**
 * Puerto de Entrada (Use Case) para recolectar datos personales de las partes.
 */
public interface SubmitPersonalDataUseCase {

    /**
     * Guarda los datos personales de un participante en el expediente.
     *
     * @param phoneNumber Teléfono del peticionante (para identificar el expediente)
     * @param role Rol del participante (PETITIONER o RESPONDENT)
     * @param fullName Nombre completo (solo usado para RESPONDENT en unilateral)
     * @param dni DNI (solo usado para RESPONDENT en unilateral)
     * @param participantPhoneNumber Teléfono del participante que se está cargando
     * @param nationality Nacionalidad
     * @param occupation Ocupación / Profesión
     * @param email Correo electrónico
     * @param birthDate Fecha de nacimiento (formato YYYY-MM-DD, opcional)
     * @param address Domicilio Real
     */
    void execute(String phoneNumber, 
                 CaseRole role, 
                 String fullName,
                 String dni,
                 String participantPhoneNumber,
                 String nationality, 
                 String occupation, 
                 String email,
                 String birthDate, 
                 AddressVO address);
}

package com.pe.swcotoschero.prospectos.Entity.enums;

/**
 * Registro de quien atendio la llamada.
 * Permite medir "contactabilidad real" (contactos con el TITULAR / total llamadas).
 */
public enum QuienContesto {

    /** El prospecto titular de los datos. */
    TITULAR,

    /** Un familiar, conocido u otra persona (no el titular). */
    TERCERO,

    /** El numero pertenece a alguien que no conoce al prospecto. */
    EQUIVOCADO
}

package com.pe.swcotoschero.prospectos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar un usuario existente.
 * El campo 'usuario' (username) NO es editable por seguridad.
 * El campo 'password' es opcional - solo se actualiza si se proporciona.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUsuarioRequestDTO {

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "Los apellidos son requeridos")
    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    private String apellidos;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    private String email;

    /**
     * Password es opcional. Si se proporciona, debe tener al menos 6 caracteres.
     * Si viene null o vacío, no se actualiza el password del usuario.
     */
    @Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotNull(message = "El rol es requerido")
    private Long rolId;

    @NotNull(message = "El estado es requerido")
    private Boolean estado;
}

package com.pe.swcotoschero.prospectos.dto;

public class UsuarioDTO {
    private Long id;
    private String nombre;
    private String apellidos;
    private String usuario;
    private String email;
    private Boolean estado;
    private String nombreCompleto;
    
    // Información del rol
    private Long rolId;
    private String rolNombre;

    // Constructor vacío
    public UsuarioDTO() {}

    // Constructor completo
    public UsuarioDTO(Long id, String nombre, String apellidos, String usuario, 
                     String email, Boolean estado, Long rolId, String rolNombre) {
        this.id = id;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.usuario = usuario;
        this.email = email;
        this.estado = estado;
        this.rolId = rolId;
        this.rolNombre = rolNombre;
        
        // Crear nombre completo
        if (nombre != null && apellidos != null) {
            this.nombreCompleto = nombre + " " + apellidos;
        }
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
        updateNombreCompleto();
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
        updateNombreCompleto();
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public Long getRolId() {
        return rolId;
    }

    public void setRolId(Long rolId) {
        this.rolId = rolId;
    }

    public String getRolNombre() {
        return rolNombre;
    }

    public void setRolNombre(String rolNombre) {
        this.rolNombre = rolNombre;
    }

    private void updateNombreCompleto() {
        if (nombre != null && apellidos != null) {
            this.nombreCompleto = nombre + " " + apellidos;
        }
    }
}
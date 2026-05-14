package agenda.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import agenda.dto.CrearUsuarioRequest;
import agenda.dto.ActualizarUsuarioRequest;
import agenda.dto.UsuarioResponseDTO;
import agenda.enums.Rol;
import agenda.service.UsuarioService;

class UsuarioControllerTest {

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final UsuarioController usuarioController = new UsuarioController(usuarioService);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(usuarioController).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listarUsuarios_debeResponder200ConLista() throws Exception {
        UsuarioResponseDTO response = crearResponse();
        when(usuarioService.obtenerTodos()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("alumno@iesjandula.es"))
                .andExpect(jsonPath("$[0].nombre").value("Alumno Demo"));
    }

    @Test
    void obtenerPorId_debeResponder200ConElemento() throws Exception {
        when(usuarioService.obtenerPorId(1L)).thenReturn(crearResponse());

        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("alumno@iesjandula.es"));
    }

    @Test
    void crearUsuario_debeResponder201() throws Exception {
        CrearUsuarioRequest request = crearRequest();
        when(usuarioService.crearUsuario(any(CrearUsuarioRequest.class))).thenReturn(crearResponse());

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Alumno Demo"))
                .andExpect(jsonPath("$.rol").value("PROFESORADO"));

        verify(usuarioService).crearUsuario(any(CrearUsuarioRequest.class));
    }

    @Test
        void actualizarUsuario_debeResponder200() throws Exception {
        ActualizarUsuarioRequest request = new ActualizarUsuarioRequest();
        request.setEmail("alumno@iesjandula.es");
        request.setNombre("Alumno Demo");
        request.setRol(Rol.PROFESORADO);

        when(usuarioService.actualizarUsuario(org.mockito.ArgumentMatchers.eq(1L), any(ActualizarUsuarioRequest.class)))
            .thenReturn(crearResponse());

        mockMvc.perform(put("/api/usuarios/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("alumno@iesjandula.es"));
        }

    @Test
    void eliminarUsuario_debeResponder204() throws Exception {
        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(usuarioService).eliminarUsuario(1L);
    }

    private CrearUsuarioRequest crearRequest() {
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setEmail("alumno@iesjandula.es");
        request.setNombre("Alumno Demo");
        request.setPassword("Password123");
        request.setRol(Rol.PROFESORADO);
        return request;
    }

    private UsuarioResponseDTO crearResponse() {
        UsuarioResponseDTO response = new UsuarioResponseDTO();
        response.setId(1L);
        response.setEmail("alumno@iesjandula.es");
        response.setNombre("Alumno Demo");
        response.setRol(Rol.PROFESORADO);
        response.setFechaAlta(LocalDateTime.of(2026, 5, 14, 10, 30));
        return response;
    }
}
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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import agenda.dto.CrearTipoEventoRequest;
import agenda.dto.TipoEventoResponseDTO;
import agenda.service.TipoEventoService;

class TipoEventoControllerTest {

    private final TipoEventoService tipoEventoService = mock(TipoEventoService.class);
    private final TipoEventoController tipoEventoController = new TipoEventoController(tipoEventoService);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(tipoEventoController).build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listarTodos_debeResponder200ConLista() throws Exception {
        TipoEventoResponseDTO response = crearResponse();
        when(tipoEventoService.obtenerTiposPublicos()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/tipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Reunión General"))
                .andExpect(jsonPath("$[0].color").value("#1F8EF1"));
    }

    @Test
    void listarAdmin_debeResponder200ConListaCompleta() throws Exception {
        TipoEventoResponseDTO response = crearResponse();
        when(tipoEventoService.obtenerTiposAdmin()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/tipos/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Reunión General"));
    }

    @Test
    void obtenerPorId_debeResponder200ConElemento() throws Exception {
        when(tipoEventoService.obtenerPorId(1L)).thenReturn(crearResponse());

        mockMvc.perform(get("/api/tipos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Reunión General"));
    }

    @Test
    void crearTipo_debeResponder201() throws Exception {
        CrearTipoEventoRequest request = crearRequest();
        when(tipoEventoService.crearTipoEvento(any(CrearTipoEventoRequest.class))).thenReturn(crearResponse());

        mockMvc.perform(post("/api/tipos/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Reunión General"))
                .andExpect(jsonPath("$.activo").value(true));

        verify(tipoEventoService).crearTipoEvento(any(CrearTipoEventoRequest.class));
    }

    @Test
    void actualizarTipo_debeResponder200() throws Exception {
        CrearTipoEventoRequest request = crearRequest();
        when(tipoEventoService.actualizarTipoEvento(org.mockito.ArgumentMatchers.eq(1L), any(CrearTipoEventoRequest.class)))
                .thenReturn(crearResponse());

        mockMvc.perform(put("/api/tipos/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Reunión General"));
    }

    @Test
    void eliminarTipo_debeResponder204() throws Exception {
        mockMvc.perform(delete("/api/tipos/admin/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(tipoEventoService).eliminarTipoEvento(1L);
    }

    private CrearTipoEventoRequest crearRequest() {
        CrearTipoEventoRequest request = new CrearTipoEventoRequest();
        request.setNombre("Reunión General");
        request.setColor("#1F8EF1");
        request.setIcono("meeting_room");
        request.setPrioridad(2);
        request.setActivo(true);
        return request;
    }

    private TipoEventoResponseDTO crearResponse() {
        TipoEventoResponseDTO response = new TipoEventoResponseDTO();
        response.setId(1L);
        response.setNombre("Reunión General");
        response.setColor("#1F8EF1");
        response.setIcono("meeting_room");
        response.setPrioridad(2);
        response.setActivo(true);
        response.setProtegido(false);
        return response;
    }
}
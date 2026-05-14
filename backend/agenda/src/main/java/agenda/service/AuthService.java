package agenda.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import agenda.dto.auth.AuthResponse;
import agenda.dto.auth.LoginRequest;
import agenda.dto.auth.RegisterRequest;
import agenda.enums.Rol;
import agenda.exception.DuplicateResourceException;
import agenda.model.Usuario;
import agenda.repository.UsuarioRepository;
import agenda.security.JwtService;
import agenda.security.UsuarioDetailsService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public AuthResponse register(RegisterRequest request) {
        usuarioRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new DuplicateResourceException("Email ya registrado: " + request.getEmail());
        });

        Usuario usuario = Usuario.builder()
                .email(request.getEmail())
                .nombre(request.getNombre())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(request.getRol() == null ? Rol.PROFESORADO : request.getRol())
                .build();

        Usuario saved = usuarioRepository.save(usuario);

        UserDetails userDetails = User.builder()
                .username(saved.getEmail())
                .password(saved.getPassword())
                .roles(saved.getRol().name())
                .build();

        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .tipoToken("Bearer")
                .email(saved.getEmail())
                .nombre(saved.getNombre())
                .rol(saved.getRol())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        // load user details
        UserDetails userDetails = usuarioDetailsService.loadUserByUsername(request.getEmail());

        String token = jwtService.generateToken(userDetails);

        // fetch usuario to get nombre and rol
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail()).orElseThrow();

        return AuthResponse.builder()
                .token(token)
                .tipoToken("Bearer")
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .rol(usuario.getRol())
                .build();
    }
}

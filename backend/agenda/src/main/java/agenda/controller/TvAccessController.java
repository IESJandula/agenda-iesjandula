package agenda.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/tv")
public class TvAccessController {

    @Value("${app.tv.token:}")
    private String tvToken;

    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(
            @RequestParam(value = "tvToken", required = false) String queryToken,
            @RequestHeader(value = "X-TV-Token", required = false) String headerToken) {
        String providedToken = StringUtils.hasText(headerToken) ? headerToken : queryToken;
        String expectedToken = tvToken == null ? "" : tvToken.trim();

        if (!StringUtils.hasText(expectedToken)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "El token TV no está configurado");
        }

        if (!StringUtils.hasText(providedToken) || !expectedToken.equals(providedToken.trim())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Token TV inválido");
        }

        return ResponseEntity.noContent().build();
    }
}

package uk.gov.moj.cpp.access.demo.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class EchoController {

    @GetMapping("/hello")
    public Map<String, String> hello(@RequestParam(defaultValue = "world") final String name) {
        return Map.of("message", "hello " + name);
    }

    @PostMapping("/echo")
    public ResponseEntity<?> echo(@RequestBody final Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("received", body));
    }

    @GetMapping("/error")
    public ResponseEntity<?> error() {
        return ResponseEntity.status(418).body(Map.of("error", "teapot"));
    }
}

package hr.fer.letsdosports.user;

import hr.fer.letsdosports.auth.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;
    public UserController(UserService service){this.service=service;}

    @GetMapping
    public List<UserResponse> getAll(
            @RequestParam(required = false) Short cityId,
            @RequestParam(required = false) Short sportId,
            @RequestParam(required = false) Short skillLevelId,
            @RequestParam(required = false) Short ageMin,
            @RequestParam(required = false) Short ageMax
    ){
        if (ageMin != null && (ageMin < 0 || ageMin > 130)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ageMin");
        }

        if (ageMax != null && (ageMax < 0 || ageMax > 130)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ageMax");
        }

        if (ageMin != null && ageMax != null && ageMin > ageMax) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ageMin cannot be greater than ageMax"
            );
        }
        return service.search(cityId,sportId,skillLevelId,ageMin,ageMax);
    }

    @GetMapping("/{id}")
    public UserResponse getOne(@PathVariable Long id){
        return service.getById(id);
    }

    @GetMapping("/me")
    public UserResponse getMe(@CurrentUserId Long userId){
        return service.getMine(userId);
    }

    @PatchMapping("/me")
    public UserResponse update(@CurrentUserId Long userId, @Valid @RequestBody UpdateUserRequest req){

        service.update(userId, req);
        return service.getById(userId);
    }

}

package hr.fer.letsdosports.user;

import hr.fer.letsdosports.auth.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
        return service.search(cityId,sportId,skillLevelId,ageMin,ageMax);
    }

    @GetMapping("/{id}")
    public UserResponse getOne(@PathVariable Long id){
        return service.getById(id);
    }

    @GetMapping("/me")
    private UserResponse getMe(@CurrentUserId Long userId){
        return service.getMine(userId);
    }

    @PostMapping("/me")
    private ResponseEntity<UserResponse> newUser(@Valid @RequestBody CreateUserRequest req){
        User u = service.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(u.getId()).toUri();
        return ResponseEntity.created(location).body(service.getById(u.getId()));
    }

    @PatchMapping("/me")
    private UserResponse update(@CurrentUserId Long userId, @Valid @RequestBody UpdateUserRequest req){

        service.update(userId, req);
        return service.getById(userId);
    }

    @PutMapping("/users/me/photo")
    private void updatePicture(){

    }

}

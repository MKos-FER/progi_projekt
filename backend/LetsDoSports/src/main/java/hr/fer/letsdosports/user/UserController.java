package hr.fer.letsdosports.user;

import hr.fer.letsdosports.auth.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private void newUser(){

    }

    @PatchMapping("/me")
    private void update(@CurrentUserId Long userId){

    }

    @PutMapping("/users/me/photo")
    private void updatePicture(){

    }

}

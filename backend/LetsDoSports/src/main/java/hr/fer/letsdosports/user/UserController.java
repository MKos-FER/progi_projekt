package hr.fer.letsdosports.user;

import hr.fer.letsdosports.auth.CurrentUserId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("users")
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
    public /*ResponseEntity<UserResponse>*/ void getOne(@PathVariable Long id){

    }

    @GetMapping("me")
    private /*User*/ void getMe(@CurrentUserId Long userId){

    }

    @PatchMapping("me")
    private /*UserResponse*/ void update(@CurrentUserId Long userId){

    }

}

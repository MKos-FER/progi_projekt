package hr.fer.letsdosports.user;

import hr.fer.letsdosports.sport.Sport;
import hr.fer.letsdosports.sport.SportDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record UserResponse(
        Long userId,
        String name,
        String username,
        String profilePhotoUrl,
        String gender,
        LocalDate dateOfBirth,
        Integer age,
        Short cityId,
        String appRoleId,
        List<SportDto> sports,
        OffsetDateTime createdAt
) {}

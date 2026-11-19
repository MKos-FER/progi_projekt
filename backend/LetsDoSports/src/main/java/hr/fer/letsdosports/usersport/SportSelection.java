package hr.fer.letsdosports.usersport;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Jedan odabrani sport s razinom iskustva, kako ga šalju registracija i PATCH /users/me
public record SportSelection(
        @NotNull(message = "Sport je obavezan.") @Positive Short sportId,
        @NotNull(message = "Razina je obavezna.") @Positive Short skillLevelId) {}

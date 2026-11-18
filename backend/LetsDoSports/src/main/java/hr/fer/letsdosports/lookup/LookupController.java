package hr.fer.letsdosports.lookup;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Šifrarnici za obrasce profila i filtre (api-endpoints §4); sve rute su javne
@RestController
public class LookupController {
    private final LookupRepository lookups;

    public LookupController(LookupRepository lookups) { this.lookups = lookups; }

    @GetMapping("/sports")
    public List<LookupItem> sports() { return lookups.sports(); }

    @GetMapping("/cities")
    public List<LookupItem> cities() { return lookups.cities(); }

    @GetMapping("/skill-levels")
    public List<SkillLevelItem> skillLevels() { return lookups.skillLevels(); }
}

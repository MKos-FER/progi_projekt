package hr.fer.letsdosports.lookup;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Treba pokrenutu bazu s migracijama (V2 puni šifrarnike). Testovi samo čitaju.
@SpringBootTest
@AutoConfigureMockMvc
class LookupControllerTest {

    @Autowired MockMvc mvc;

    private String getAnonymous(String url) throws Exception {
        return mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void sports_publicAndSortedByName() throws Exception {
        mvc.perform(get("/api/v1/sports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].description").doesNotExist());

        List<String> names = JsonPath.read(getAnonymous("/api/v1/sports"), "$[*].name");
        assertThat(names).containsSubsequence("Košarka", "Nogomet", "Odbojka");
    }

    @Test
    void cities_publicAndSortedByName() throws Exception {
        mvc.perform(get("/api/v1/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").isString());

        List<String> names = JsonPath.read(getAnonymous("/api/v1/cities"), "$[*].name");
        assertThat(names).containsSubsequence("Osijek", "Rijeka", "Split", "Zagreb");
    }

    @Test
    void skillLevels_publicAndSortedByRank() throws Exception {
        mvc.perform(get("/api/v1/skill-levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Amateur"))
                .andExpect(jsonPath("$[0].rank").value(1));

        List<Integer> ranks = JsonPath.read(getAnonymous("/api/v1/skill-levels"), "$[*].rank");
        assertThat(ranks).isNotEmpty().isSorted();
    }
}

package hr.fer.letsdosports.profile;

import hr.fer.letsdosports.auth.CurrentUserArgumentResolver;
import hr.fer.letsdosports.auth.CurrentUserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfilePhotoController.class)
class ProfilePhotoControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProfilePhotoService service;
    @MockitoBean PhotoStorage storage;
    @MockitoBean CurrentUserArgumentResolver currentUser;

    @BeforeEach void identity() throws Exception {
        when(currentUser.supportsParameter(any())).thenAnswer(inv ->
                ((MethodParameter) inv.getArgument(0)).hasParameterAnnotation(CurrentUserId.class));
        when(currentUser.resolveArgument(any(), any(), any(), any())).thenReturn(7L);
    }

    @Test void anonymousCannotReadProfile() throws Exception {
        mvc.perform(get("/api/v1/users/me/photo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser void optionalPhotoReturnsNullAndCsrfToken() throws Exception {
        mvc.perform(get("/api/v1/users/me/photo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilePhotoUrl").isEmpty())
                .andExpect(jsonPath("$.csrfToken").isNotEmpty())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test @WithMockUser void multipartPutUsesCurrentIdentity() throws Exception {
        String url = ProfilePhotoService.PREFIX + "photo.png";
        when(service.replace(eq(7L), any())).thenReturn(url);
        mvc.perform(multipart("/api/v1/users/me/photo").file(PhotoStorageTest.photo("png"))
                        .with(req -> { req.setMethod("PUT"); return req; }).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profilePhotoUrl").value(url));
        verify(service).replace(eq(7L), any());
    }

    @Test @WithMockUser void csrfAndFileAreRequired() throws Exception {
        mvc.perform(multipart("/api/v1/users/me/photo").file(PhotoStorageTest.photo("png"))
                        .with(req -> { req.setMethod("PUT"); return req; }))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/users/me/photo")
                        .with(req -> { req.setMethod("PUT"); return req; }).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser void imageHasExplicitTypeAndSafeHeaders() throws Exception {
        byte[] png = PhotoStorageTest.photo("png").getBytes();
        when(storage.read("photo.png")).thenReturn(png);
        mvc.perform(get("/api/v1/profile-photos/photo.png"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}

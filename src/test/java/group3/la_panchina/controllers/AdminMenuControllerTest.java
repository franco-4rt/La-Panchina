package group3.la_panchina.controllers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import group3.la_panchina.models.MenuItemModel;
import group3.la_panchina.models.MenuItemModel.MenuType;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.MenuItemRepository;
import group3.la_panchina.repositories.UserRepository;

@WebMvcTest(AdminMenuController.class)
class AdminMenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuItemRepository repo;

    @MockitoBean
    private AuthController authController;

    @MockitoBean
    private UserRepository userRepository;

    private MockHttpSession sessionFor(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", new User("Test", "t@test.com", "pw", role));
        return session;
    }

    private MockHttpServletRequestBuilder validAdd(String url) {
        return post(url)
            .session(sessionFor("ADMIN"))
            .param("menuType", "DINNER")
            .param("category", "Pizzas")
            .param("name", "Margherita")
            .param("description", "Tomate y mozzarella")
            .param("price", "200");
    }

    private RequestPostProcessor override(String name, String value) {
        return request -> {
            request.setParameter(name, value);
            return request;
        };
    }

    @Test
    void listRedirectsToLoginWhenLoggedOut() throws Exception {
        mockMvc.perform(get("/admin/menu"))
            .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(repo);
    }

    @Test
    void listRedirectsToLoginForACustomer() throws Exception {
        mockMvc.perform(get("/admin/menu").session(sessionFor("CUSTOMER")))
            .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(repo);
    }

    @Test
    void addFormRedirectsToLoginWhenLoggedOut() throws Exception {
        mockMvc.perform(get("/admin/menu/add"))
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void postAddIsBlockedForACustomerAndSavesNothing() throws Exception {
        mockMvc.perform(validAdd("/admin/menu/add").session(sessionFor("CUSTOMER")))
            .andExpect(redirectedUrl("/login"));
        verify(repo, never()).save(any());
    }

    @Test
    void postAddIsBlockedWhenLoggedOut() throws Exception {
        mockMvc.perform(post("/admin/menu/add")
                .param("menuType", "DINNER").param("category", "Pizzas")
                .param("name", "X").param("price", "10"))
            .andExpect(redirectedUrl("/login"));
        verify(repo, never()).save(any());
    }

    @Test
    void deleteIsBlockedForACustomer() throws Exception {
        mockMvc.perform(post("/admin/menu/delete/5").session(sessionFor("CUSTOMER")))
            .andExpect(redirectedUrl("/login"));
        verify(repo, never()).deleteById(any());
    }

    @Test
    void roleCheckIgnoresCase() throws Exception {
        when(repo.findAll(any(Sort.class))).thenReturn(List.of());
        mockMvc.perform(get("/admin/menu").session(sessionFor("admin")))
            .andExpect(status().isOk());
    }

    @Test
    void listShowsAdminMenuViewForAnAdmin() throws Exception {
        when(repo.findAll(any(Sort.class))).thenReturn(
            List.of(new MenuItemModel("Bebidas", "", "Espresso", 60, MenuType.BREAKFAST)));

        mockMvc.perform(get("/admin/menu").session(sessionFor("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("adminMenu"))
            .andExpect(model().attributeExists("items"));
    }

    @Test
    void addFormShowsAnEmptyFormForAnAdmin() throws Exception {
        mockMvc.perform(get("/admin/menu/add").session(sessionFor("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("menuAdd"))
            .andExpect(model().attributeExists("item", "types"))
            .andExpect(model().attribute("formAction", "/admin/menu/add"));
    }

    @Test
    void validAddSavesTheDishAndRedirects() throws Exception {
        mockMvc.perform(validAdd("/admin/menu/add"))
            .andExpect(redirectedUrl("/admin/menu"));

        ArgumentCaptor<MenuItemModel> captor = ArgumentCaptor.forClass(MenuItemModel.class);
        verify(repo).save(captor.capture());
        MenuItemModel saved = captor.getValue();
        assertEquals("Margherita", saved.getName());
        assertEquals("Pizzas", saved.getCategory());
        assertEquals(200, saved.getPrice());
        assertEquals(MenuType.DINNER, saved.getMenuType());
    }

    @Test
    void addIgnoresAnIdSentByTheClient() throws Exception {
        mockMvc.perform(validAdd("/admin/menu/add").param("id", "99"))
            .andExpect(redirectedUrl("/admin/menu"));

        ArgumentCaptor<MenuItemModel> captor = ArgumentCaptor.forClass(MenuItemModel.class);
        verify(repo).save(captor.capture());
        assertNull(captor.getValue().getId());
    }

    @Test
    void addWithBlankNameReShowsTheFormWithAnErrorAndSavesNothing() throws Exception {
        mockMvc.perform(validAdd("/admin/menu/add").with(override("name", "")))
            .andExpect(status().isOk())
            .andExpect(view().name("menuAdd"))
            .andExpect(model().attributeHasFieldErrors("item", "name"));
        verify(repo, never()).save(any());
    }

    @Test
    void addWithNegativePriceReShowsTheFormWithAnError() throws Exception {
        mockMvc.perform(validAdd("/admin/menu/add").with(override("price", "-5")))
            .andExpect(view().name("menuAdd"))
            .andExpect(model().attributeHasFieldErrors("item", "price"));
        verify(repo, never()).save(any());
    }

    @Test
    void addWithoutAMenuTypeReShowsTheFormWithAnError() throws Exception {
        mockMvc.perform(post("/admin/menu/add").session(sessionFor("ADMIN"))
                .param("category", "Pizzas").param("name", "X").param("price", "10"))
            .andExpect(view().name("menuAdd"))
            .andExpect(model().attributeHasFieldErrors("item", "menuType"));
        verify(repo, never()).save(any());
    }

    @Test
    void editFormLoadsAnExistingDish() throws Exception {
        MenuItemModel existing = new MenuItemModel("Bebidas", "", "Espresso", 60, MenuType.BREAKFAST);
        when(repo.findById(5L)).thenReturn(Optional.of(existing));

        mockMvc.perform(get("/admin/menu/edit/5").session(sessionFor("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("menuAdd"))
            .andExpect(model().attribute("formAction", "/admin/menu/edit/5"));
    }

    @Test
    void editFormForAMissingDishRedirectsToTheList() throws Exception {
        when(repo.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/menu/edit/404").session(sessionFor("ADMIN")))
            .andExpect(redirectedUrl("/admin/menu"));
    }

    @Test
    void validEditSavesUnderTheIdInTheUrl() throws Exception {
        mockMvc.perform(validAdd("/admin/menu/edit/5").param("id", "99"))
            .andExpect(redirectedUrl("/admin/menu"));

        ArgumentCaptor<MenuItemModel> captor = ArgumentCaptor.forClass(MenuItemModel.class);
        verify(repo).save(captor.capture());
        assertEquals(5L, captor.getValue().getId());
    }

    @Test
    void invalidEditReShowsTheFormAndSavesNothing() throws Exception {
        mockMvc.perform(validAdd("/admin/menu/edit/5").with(override("name", "")))
            .andExpect(view().name("menuAdd"))
            .andExpect(model().attributeHasFieldErrors("item", "name"))
            .andExpect(model().attribute("formAction", "/admin/menu/edit/5"));
        verify(repo, never()).save(any());
    }

    @Test
    void deleteRemovesTheDishAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/menu/delete/5").session(sessionFor("ADMIN")))
            .andExpect(redirectedUrl("/admin/menu"));
        verify(repo).deleteById(5L);
    }
}
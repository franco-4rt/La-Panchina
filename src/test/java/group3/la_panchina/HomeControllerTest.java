package group3.la_panchina;

import java.util.List;

import static org.hamcrest.Matchers.hasKey;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import group3.la_panchina.controllers.HomeController;
import group3.la_panchina.models.MenuItemModel;
import group3.la_panchina.models.MenuItemModel.MenuType;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.MenuItemRepository;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuItemRepository repo;

    @Test
    void testHomePageReturns200AndRendersIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

        @Test
    void menuDefaultsToBreakfast() throws Exception {
        when(repo.findByMenuType(eq(MenuType.BREAKFAST), any(Sort.class))).thenReturn(List.of());

        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(view().name("menu"))
                .andExpect(model().attribute("type", MenuType.BREAKFAST));

        verify(repo).findByMenuType(eq(MenuType.BREAKFAST), any(Sort.class));
    }

    @Test
    void menuWithDinnerTypeLoadsDinner() throws Exception {
        when(repo.findByMenuType(eq(MenuType.DINNER), any(Sort.class))).thenReturn(List.of());

        mockMvc.perform(get("/menu").param("type", "DINNER"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("type", MenuType.DINNER));

        verify(repo).findByMenuType(eq(MenuType.DINNER), any(Sort.class));
    }

    @Test
    void menuWithInvalidTypeReturns400() throws Exception {
        mockMvc.perform(get("/menu").param("type", "BRUNCH"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void menuGroupsItemsByCategory() throws Exception {
        MenuItemModel a = new MenuItemModel("Pizzas", "Tomate", "Margherita", 20, MenuType.DINNER);
        MenuItemModel b = new MenuItemModel("Bebidas", "", "Espresso", 4, MenuType.DINNER);
        when(repo.findByMenuType(eq(MenuType.DINNER), any(Sort.class))).thenReturn(List.of(a, b));

        mockMvc.perform(get("/menu").param("type", "DINNER"))
                .andExpect(model().attribute("grouped", hasKey("Pizzas")))
                .andExpect(model().attribute("grouped", hasKey("Bebidas")));
    }

    @Test
    void itemWithNullCategoryGoesUnderOtros() throws Exception {
        MenuItemModel item = new MenuItemModel(null, "x", "Mystery", 5, MenuType.BREAKFAST);
        when(repo.findByMenuType(eq(MenuType.BREAKFAST), any(Sort.class))).thenReturn(List.of(item));

        mockMvc.perform(get("/menu"))
                .andExpect(model().attribute("grouped", hasKey("Otros")));
    }

    @Test
    void isAdminIsFalseWithoutSession() throws Exception {
        when(repo.findByMenuType(any(MenuType.class), any(Sort.class))).thenReturn(List.of());

        mockMvc.perform(get("/menu"))
                .andExpect(model().attribute("isAdmin", false));
    }

    @Test
    void isAdminIsTrueForAdminUser() throws Exception {
        when(repo.findByMenuType(any(MenuType.class), any(Sort.class))).thenReturn(List.of());

        User admin = new User();
        admin.setRole("ADMIN");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", admin);

        mockMvc.perform(get("/menu").session(session))
                .andExpect(model().attribute("isAdmin", true));
    }

    @Test
    void isAdminIsFalseForCustomerUser() throws Exception {
        when(repo.findByMenuType(any(MenuType.class), any(Sort.class))).thenReturn(List.of());

        User customer = new User();
        customer.setRole("CUSTOMER");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", customer);

        mockMvc.perform(get("/menu").session(session))
                .andExpect(model().attribute("isAdmin", false));
    }
}


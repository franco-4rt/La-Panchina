package group3.la_panchina.controllers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import group3.la_panchina.models.MenuItemModel;
import group3.la_panchina.models.MenuItemModel.MenuType;
import group3.la_panchina.repositories.MenuItemRepository;

@WebMvcTest(HomeController.class)
class MenuPageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuItemRepository repo;

    private MenuItemModel item(String category, String name, MenuType type, String icons) {
        MenuItemModel m = new MenuItemModel(category, "desc", name, 10, type);
        m.setIcons(icons);
        return m;
    }

    private void stub(MenuType type, MenuItemModel... items) {
        when(repo.findByMenuType(eq(type), any(Sort.class))).thenReturn(List.of(items));
    }

    private String page(String url) throws Exception {
        return mockMvc.perform(get(url))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private int count(String html, String needle) {
        int n = 0;
        int i = 0;
        while ((i = html.indexOf(needle, i)) != -1) {
            n++;
            i += needle.length();
        }
        return n;
    }

    @Test
    void indexBarListsEveryCategoryAsAnAnchorLink() throws Exception {
        stub(MenuType.BREAKFAST,
            item("Bebidas", "Cafe", MenuType.BREAKFAST, null),
            item("Lo dulce", "Panque", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertTrue(html.contains("class=\"menu-index\""));
        assertTrue(html.contains("aria-label=\"Menu categories\""));
        assertTrue(html.contains("href=\"#cat-0\""));
        assertTrue(html.contains(">Bebidas</a>"));
        assertTrue(html.contains("href=\"#cat-1\""));
        assertTrue(html.contains(">Lo dulce</a>"));
    }

    @Test
    void indexBarFollowsTheSelectedMenu() throws Exception {
        stub(MenuType.BREAKFAST, item("Bebidas", "Cafe", MenuType.BREAKFAST, null));
        stub(MenuType.DINNER, item("Pastas", "Calabrese", MenuType.DINNER, null));

        String html = page("/menu?type=DINNER");

        assertTrue(html.contains(">Pastas</a>"));
        assertFalse(html.contains(">Bebidas</a>"));
    }

    @Test
    void indexBarHasNoLinksWhenTheMenuIsEmpty() throws Exception {
        stub(MenuType.BREAKFAST);

        String html = page("/menu");

        assertFalse(html.contains("href=\"#cat-0\""));
    }

    @Test
    void nullCategoryAppearsInTheIndexAsOtros() throws Exception {
        stub(MenuType.BREAKFAST, item(null, "Mystery", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertTrue(html.contains(">Otros</a>"));
    }

    @Test
    void categorySectionsHaveIdsMatchingTheIndexLinks() throws Exception {
        stub(MenuType.BREAKFAST,
            item("Bebidas", "Cafe", MenuType.BREAKFAST, null),
            item("Lo dulce", "Panque", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertTrue(html.contains("id=\"cat-0\""));
        assertTrue(html.contains("id=\"cat-1\""));
    }

    @Test
    void backToTopButtonIsRendered() throws Exception {
        stub(MenuType.BREAKFAST, item("Bebidas", "Cafe", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertTrue(html.contains("class=\"back-to-top\""));
        assertTrue(html.contains("href=\"#\""));
        assertTrue(html.contains("aria-label=\"Back to top\""));
    }

    @Test
    void backToTopButtonAppearsOnlyOnceEvenWithManyCategories() throws Exception {
        stub(MenuType.BREAKFAST,
            item("A", "a", MenuType.BREAKFAST, null),
            item("B", "b", MenuType.BREAKFAST, null),
            item("C", "c", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertEquals(1, count(html, "class=\"back-to-top\""));
    }

    @Test
    void backToTopButtonIsRenderedOnTheDinnerMenuToo() throws Exception {
        stub(MenuType.DINNER, item("Pastas", "Calabrese", MenuType.DINNER, null));

        String html = page("/menu?type=DINNER");

        assertTrue(html.contains("class=\"back-to-top\""));
    }

    @Test
    void spicyItemShowsTheSpicyIcon() throws Exception {
        stub(MenuType.DINNER, item("Pastas", "Calabrese", MenuType.DINNER, "SPICY"));

        String html = page("/menu?type=DINNER");

        assertTrue(html.contains("title=\"Spicy\""));
        assertFalse(html.contains("title=\"Vegetarian\""));
    }

    @Test
    void vegetarianItemShowsTheVegetarianIcon() throws Exception {
        stub(MenuType.DINNER, item("Pizzas", "Margherita", MenuType.DINNER, "VEGETARIAN"));

        String html = page("/menu?type=DINNER");

        assertTrue(html.contains("title=\"Vegetarian\""));
        assertFalse(html.contains("title=\"Spicy\""));
    }

    @Test
    void itemWithBothIconsShowsBoth() throws Exception {
        stub(MenuType.DINNER, item("Pastas", "Aglio e olio", MenuType.DINNER, "SPICY,VEGETARIAN"));

        String html = page("/menu?type=DINNER");

        assertTrue(html.contains("title=\"Spicy\""));
        assertTrue(html.contains("title=\"Vegetarian\""));
    }

    @Test
    void itemWithoutIconsShowsNone() throws Exception {
        stub(MenuType.BREAKFAST, item("Bebidas", "Cafe", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertFalse(html.contains("title=\"Spicy\""));
        assertFalse(html.contains("title=\"Vegetarian\""));
    }

    @Test
    void itemWithEmptyIconsShowsNone() throws Exception {
        stub(MenuType.BREAKFAST, item("Bebidas", "Cafe", MenuType.BREAKFAST, ""));

        String html = page("/menu");

        assertFalse(html.contains("title=\"Spicy\""));
        assertFalse(html.contains("title=\"Vegetarian\""));
    }

    @Test
    void iconsAppearOnlyOnTheTaggedItems() throws Exception {
        stub(MenuType.DINNER,
            item("Pastas", "Calabrese", MenuType.DINNER, "SPICY"),
            item("Pastas", "Carbonara", MenuType.DINNER, null),
            item("Pastas", "Pomodoro", MenuType.DINNER, "VEGETARIAN"));

        String html = page("/menu?type=DINNER");

        assertEquals(1, count(html, "title=\"Spicy\""));
        assertEquals(1, count(html, "title=\"Vegetarian\""));
    }

    @Test
    void photoPathUsesTheMenuTypeAndCategoryPosition() throws Exception {
        stub(MenuType.BREAKFAST,
            item("Bebidas", "Cafe", MenuType.BREAKFAST, null),
            item("Lo dulce", "Panque", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertTrue(html.contains("/images/menu/breakfast-0.jpg"));
        assertTrue(html.contains("/images/menu/breakfast-1.jpg"));
    }

    @Test
    void dinnerPhotoPathUsesTheDinnerPrefix() throws Exception {
        stub(MenuType.DINNER, item("Pastas", "Calabrese", MenuType.DINNER, null));

        String html = page("/menu?type=DINNER");

        assertTrue(html.contains("/images/menu/dinner-0.jpg"));
    }

    @Test
    void photoAltTextIsTheCategoryName() throws Exception {
        stub(MenuType.BREAKFAST, item("Bebidas", "Cafe", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertTrue(html.contains("alt=\"Bebidas\""));
    }

    @Test
    void placeholderPointsToTheLogo() throws Exception {
        stub(MenuType.BREAKFAST, item("Bebidas", "Cafe", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertTrue(html.contains("data-placeholder=\"/images/logo.png\""));
    }

    @Test
    void layoutAlternatesSidesWithEachCategory() throws Exception {
        stub(MenuType.BREAKFAST,
            item("A", "a", MenuType.BREAKFAST, null),
            item("B", "b", MenuType.BREAKFAST, null),
            item("C", "c", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertEquals(2, count(html, "cat-layout img-right"));
        assertEquals(1, count(html, "cat-layout img-left"));
    }

    @Test
    void firstCategoryPutsThePhotoOnTheRight() throws Exception {
        stub(MenuType.BREAKFAST, item("A", "a", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertTrue(html.contains("cat-layout img-right"));
        assertFalse(html.contains("cat-layout img-left"));
    }

    @Test
    void captionDefaultsToTheFirstDishOfTheCategory() throws Exception {
        stub(MenuType.DINNER,
            item("Pizzas", "Margherita", MenuType.DINNER, null),
            item("Pizzas", "4 Formaggi", MenuType.DINNER, null));

        String html = page("/menu?type=DINNER");

        assertTrue(html.contains(">Margherita</figcaption>"));
        assertFalse(html.contains(">4 Formaggi</figcaption>"));
    }

    @Test
    void breakfastEntradasCaptionIsBowlDeFruta() throws Exception {
        stub(MenuType.BREAKFAST,
            item("Bebidas", "Cafe", MenuType.BREAKFAST, null),
            item("Entradas / Bowl", "Bowl energetico", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertTrue(html.contains(">Bowl de fruta</figcaption>"));
        assertFalse(html.contains(">Bowl energetico</figcaption>"));
    }

    @Test
    void breakfastLoSaladoCaptionIsChilaquiles() throws Exception {
        stub(MenuType.BREAKFAST,
            item("c0", "n0", MenuType.BREAKFAST, null),
            item("c1", "n1", MenuType.BREAKFAST, null),
            item("c2", "n2", MenuType.BREAKFAST, null),
            item("c3", "n3", MenuType.BREAKFAST, null),
            item("Lo salado", "Huevos al gusto", MenuType.BREAKFAST, null));

        String html = page("/menu");

        assertTrue(html.contains(">Chilaquiles</figcaption>"));
        assertFalse(html.contains(">Huevos al gusto</figcaption>"));
    }

    @Test
    void captionOverrideDoesNotAffectTheDinnerMenu() throws Exception {
        stub(MenuType.DINNER,
            item("c0", "n0", MenuType.DINNER, null),
            item("c1", "n1", MenuType.DINNER, null),
            item("c2", "n2", MenuType.DINNER, null),
            item("c3", "n3", MenuType.DINNER, null),
            item("c4", "Extra dish", MenuType.DINNER, null));

        String html = page("/menu?type=DINNER");

        assertTrue(html.contains(">n1</figcaption>"));
        assertTrue(html.contains(">Extra dish</figcaption>"));
        assertFalse(html.contains(">Chilaquiles</figcaption>"));
        assertFalse(html.contains(">Bowl de fruta</figcaption>"));
    }
}
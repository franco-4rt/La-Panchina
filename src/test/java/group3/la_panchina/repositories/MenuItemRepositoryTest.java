package group3.la_panchina.repositories;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Sort;

import group3.la_panchina.models.MenuItemModel;
import group3.la_panchina.models.MenuItemModel.MenuType;

@DataJpaTest
class MenuItemRepositoryTest {

    @Autowired
    private MenuItemRepository repo;

    private MenuItemModel dish(String category, String name, MenuType type) {
        return new MenuItemModel(category, "test row", name, 10, type);
    }

    @Test
    void saveAssignsAnIdAndIncreasesTheCountByOne() {
        long before = repo.count();
        MenuItemModel saved = repo.saveAndFlush(dish("ZZ-Test", "ZZ dish one", MenuType.DINNER));

        assertNotNull(saved.getId());
        assertEquals(before + 1, repo.count());
    }

    @Test
    void findByMenuTypeReturnsOnlyThatType() {
        repo.saveAndFlush(dish("ZZ-Test", "ZZ breakfast dish", MenuType.BREAKFAST));
        repo.saveAndFlush(dish("ZZ-Test", "ZZ dinner dish", MenuType.DINNER));

        List<MenuItemModel> dinners = repo.findByMenuType(MenuType.DINNER, Sort.by("id"));

        assertEquals(1, dinners.size());
        assertTrue(dinners.stream().allMatch(i -> i.getMenuType() == MenuType.DINNER));
        assertEquals("ZZ dinner dish", dinners.get(0).getName());
    }

    @Test
    void findByMenuTypeSortsById() {
        MenuItemModel first = repo.saveAndFlush(dish("ZZ-Test", "ZZ first", MenuType.DINNER));
        MenuItemModel second = repo.saveAndFlush(dish("ZZ-Test", "ZZ second", MenuType.DINNER));

        List<MenuItemModel> result = repo.findByMenuType(MenuType.DINNER, Sort.by("id"));

        assertEquals(first.getId(), result.get(0).getId());
        assertEquals(second.getId(), result.get(1).getId());
    }

    @Test
    void findByCategoryReturnsOnlyMatchingItems() {
        repo.saveAndFlush(dish("ZZ-Pizzas", "ZZ pizza", MenuType.DINNER));
        repo.saveAndFlush(dish("ZZ-Drinks", "ZZ drink", MenuType.DINNER));

        List<MenuItemModel> pizzas = repo.findByCategory("ZZ-Pizzas");

        assertEquals(1, pizzas.size());
        assertEquals("ZZ pizza", pizzas.get(0).getName());
    }

    @Test
    void updatingADishChangesItsStoredValues() {
        MenuItemModel saved = repo.saveAndFlush(dish("ZZ-Test", "ZZ old name", MenuType.DINNER));

        saved.setName("ZZ new name");
        saved.setPrice(25);
        repo.saveAndFlush(saved);

        MenuItemModel reloaded = repo.findById(saved.getId()).orElseThrow();
        assertEquals("ZZ new name", reloaded.getName());
        assertEquals(25, reloaded.getPrice(), 0.001);
    }

    @Test
    void deleteByIdRemovesTheDish() {
        MenuItemModel saved = repo.saveAndFlush(dish("ZZ-Test", "ZZ to delete", MenuType.DINNER));

        repo.deleteById(saved.getId());
        repo.flush();

        assertTrue(repo.findById(saved.getId()).isEmpty());
    }
}
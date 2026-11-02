package group3.la_panchina.models;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import group3.la_panchina.models.MenuItemModel.MenuType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class MenuItemModelValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private MenuItemModel valid() {
        return new MenuItemModel("Bebidas", "", "Espresso", 60, MenuType.BREAKFAST);
    }

    private boolean violatesField(MenuItemModel item, String field) {
        Set<ConstraintViolation<MenuItemModel>> violations = validator.validate(item);
        return violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Test
    void constructorPutsEachArgumentInTheRightField() {
        MenuItemModel item = new MenuItemModel("Pizzas", "Tomate", "Margherita", 200, MenuType.DINNER);
        assertEquals("Pizzas", item.getCategory());
        assertEquals("Tomate", item.getDescription());
        assertEquals("Margherita", item.getName());
        assertEquals(200, item.getPrice());
        assertEquals(MenuType.DINNER, item.getMenuType());
    }

    @Test
    void validItemHasNoViolations() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void blankNameIsRejected() {
        MenuItemModel item = valid();
        item.setName("  ");
        assertTrue(violatesField(item, "name"));
    }

    @Test
    void blankCategoryIsRejected() {
        MenuItemModel item = valid();
        item.setCategory("");
        assertTrue(violatesField(item, "category"));
    }

    @Test
    void missingMenuTypeIsRejected() {
        MenuItemModel item = valid();
        item.setMenuType(null);
        assertTrue(violatesField(item, "menuType"));
    }

    @Test
    void negativePriceIsRejected() {
        MenuItemModel item = valid();
        item.setPrice(-1);
        assertTrue(violatesField(item, "price"));
    }

    @Test
    void zeroPriceIsAllowed() {
        MenuItemModel item = valid();
        item.setPrice(0);
        assertFalse(violatesField(item, "price"));
    }

    @Test
    void emptyDescriptionIsAllowed() {
        MenuItemModel item = valid();
        item.setDescription("");
        assertFalse(violatesField(item, "description"));
    }
}
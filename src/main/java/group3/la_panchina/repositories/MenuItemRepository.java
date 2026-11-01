package group3.la_panchina.repositories;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import group3.la_panchina.models.MenuItemModel;
import group3.la_panchina.models.MenuItemModel.MenuType;

public interface MenuItemRepository extends JpaRepository<MenuItemModel, Long> {
    List<MenuItemModel> findByCategory(String category);
    List<MenuItemModel> findByMenuType(MenuType menuType, Sort sort);
}

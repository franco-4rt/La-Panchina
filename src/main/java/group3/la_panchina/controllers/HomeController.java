package group3.la_panchina.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import group3.la_panchina.models.MenuItemModel;
import group3.la_panchina.models.MenuItemModel.MenuType;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.MenuItemRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private final MenuItemRepository repo;

    public HomeController(MenuItemRepository repository) {
        this.repo = repository;
    }
    
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/menu")
    public String menu(@RequestParam(defaultValue = "BREAKFAST") MenuType type,
                    HttpSession session, Model model) {
        Map<String, List<MenuItemModel>> grouped = repo.findByMenuType(type, Sort.by("id")).stream()
            .collect(Collectors.groupingBy(
                item -> item.getCategory() == null ? "Otros" : item.getCategory(),
                LinkedHashMap::new,
                Collectors.toList()));

        model.addAttribute("grouped", grouped);
        model.addAttribute("type", type);
        model.addAttribute("isAdmin",
            session.getAttribute("session_user") instanceof User u && "ADMIN".equals(u.getRole()));
        return "menu";
    }
}
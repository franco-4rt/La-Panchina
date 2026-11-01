package group3.la_panchina.controllers;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import group3.la_panchina.models.MenuItemModel;
import group3.la_panchina.repositories.MenuItemRepository;



@Controller
public class HomeController {

    private final MenuItemRepository repo;

    public HomeController(MenuItemRepository repository){
        this.repo = repository;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }    

@GetMapping("/menu")
    public String menu(Model model) {
        Map<String, List<MenuItemModel>> grouped = repo.findAll().stream()
            .collect(Collectors.groupingBy(MenuItemModel::getCategory, TreeMap::new, Collectors.toList()));
        model.addAttribute("grouped", grouped);
        return "menu";
    }

}


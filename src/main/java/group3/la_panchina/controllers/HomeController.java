package group3.la_panchina.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
        model.addAttribute("items", repo.findAll());
        return "menu";
    }
    
}


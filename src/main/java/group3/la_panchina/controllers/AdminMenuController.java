package group3.la_panchina.controllers;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import group3.la_panchina.models.MenuItemModel;
import group3.la_panchina.models.MenuItemModel.MenuType;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.MenuItemRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;






@Controller 
@RequestMapping ("/admin/menu")
public class AdminMenuController {
    
    private final AuthController authController;
    private final MenuItemRepository repo;

    public AdminMenuController(MenuItemRepository repo, AuthController authController){
        this.repo = repo;
        this.authController = authController;
    }

    private boolean isAdmin(HttpSession session){
        return session.getAttribute("session_user") instanceof User u && "ADMIN".equals(u.getRole());
    }

    private String deny(HttpSession session){
        return session.getAttribute("session_user") == null ? "redirect:/login" : "redirect:/";
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        if(!isAdmin(session)) return deny(session);
        model.addAttribute("items", repo.findAll(Sort.by("menuType", "id")));
        return "admin/menu-list";
    }
    
    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        if (!isAdmin(session)) return deny(session);
        model.addAttribute("item", new MenuItemModel());
        return form(model, "/admin/menu", "New dish");
    }
    
    @PostMapping
    public String create(@Valid @ModelAttribute("item") MenuItemModel item,
                        BindingResult result, HttpSession session, Model model) {
        if (!isAdmin(session)) return deny(session);
        if (result.hasErrors()) return form(model, "/admin/menu", "New dish");
        item.setId(null);
        repo.save(item);
        return "redirect:/admin/menu";
    }
    
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        if(!isAdmin(session)) return deny(session);
        MenuItemModel item = repo.findById(id).orElse(null);
        if(item == null) return "redirect:/admin/menu";
        model.addAttribute("item", item);
        return form(model, "/admin/menu/" + id, "Edit dish");
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                        @Valid  @ModelAttribute("item") MenuItemModel item,
                        BindingResult result, HttpSession session, Model model) {
        if(!isAdmin(session)) return deny(session);
        if(result.hasErrors()) return form(model, "/admin/menu" + id, "Edit dish");
        item.setId(id);
        repo.save(item);
        return "redirect:/admin/menu";
    }
    
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return deny(session);
        repo.deleteById(id);
        return "redirec:/admin/menu";
    }
    

    private String form(Model model, String action, String title) {
        model.addAttribute("types", MenuType.values());
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
    return "admin/menu-form";
    }
}

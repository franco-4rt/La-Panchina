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
    
    private final MenuItemRepository repo;

    public AdminMenuController(MenuItemRepository repo, AuthController authController){
        this.repo = repo;
    }

    private boolean notAdmin(HttpSession session) {
        User user = (User) session.getAttribute("session_user");
        return user == null || !"ADMIN".equalsIgnoreCase(user.getRole());
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        if (notAdmin(session)) return "redirect:/login";
        model.addAttribute("items", repo.findAll(Sort.by("menuType", "id")));
        return "adminMenu";
    }
    
    @GetMapping("/add")
    public String addForm(HttpSession session, Model model) {
        if (notAdmin(session)) return "redirect:/login";
        model.addAttribute("item", new MenuItemModel());
        return form(model, "/admin/menu/add", "New dish");
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("item") MenuItemModel item,
                        BindingResult result, HttpSession session, Model model) {
        if (notAdmin(session)) return "redirect:/login";
        if (result.hasErrors()) return form(model, "/admin/menu/add", "New dish");
        item.setId(null);
        repo.save(item);
        return "redirect:/admin/menu";
    }
    
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        if (notAdmin(session)) return "redirect:/login";
        MenuItemModel item = repo.findById(id).orElse(null);
        if(item == null) return "redirect:/admin/menu";
        model.addAttribute("item", item);
        return form(model, "/admin/menu/edit/" + id, "Edit dish");
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                        @Valid @ModelAttribute("item") MenuItemModel item,
                        BindingResult result, HttpSession session, Model model) {
        if (notAdmin(session)) return "redirect:/login";
        if (result.hasErrors()) return form(model, "/admin/menu/edit/" + id, "Edit dish");
        item.setId(id);
        repo.save(item);
        return "redirect:/admin/menu";
    }
    
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        if (notAdmin(session)) return "redirect:/login";
        repo.deleteById(id);
        return "redirec:/admin/menu";
    }
    

    private String form(Model model, String action, String title) {
        model.addAttribute("types", MenuType.values());
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
    return "menuAdd";
    }
}

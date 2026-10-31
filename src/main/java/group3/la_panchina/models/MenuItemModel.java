package group3.la_panchina.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity 
@Table (name = "menu_items")
public class MenuItemModel {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    Long id;

    @NotBlank
    private  String name;

    @NotBlank 
    private  String category;

    @NotBlank 
    private  String description;

    @NotNull 
    private  double price;

    protected MenuItemModel(){}

    public MenuItemModel(String category, String description, String name, double price) {
        this.category = category;
        this.description = description;
        this.name = name;
        this.price = price;
    }

    public Long getId() {return id;}

    public void setId(Long id) {this.id = id;}

    public String getName() {return name;}

    public String getCategory() {return category;}

    public String getDescription() {return description;}

    public double getPrice() {return price;}

    
}

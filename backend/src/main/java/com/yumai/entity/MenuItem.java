package com.yumai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** SRS 5.1.4 - sellable menu item, linked to inventory via MenuIngredient. */
@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long menuItemId;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Double price;

    /**
     * Unit cost of producing the item. Not in the original class diagram -
     * added to make profit/loss reports (FR-06.3) computable. See DEVIATIONS.md D3.
     */
    @Column(nullable = false)
    private Double costPrice = 0.0;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MenuIngredient> ingredients = new ArrayList<>();

    public MenuItem(String name, String description, Double price, Double costPrice, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.costPrice = costPrice;
        this.category = category;
    }
}

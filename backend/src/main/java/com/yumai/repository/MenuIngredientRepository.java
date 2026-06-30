package com.yumai.repository;

import com.yumai.entity.MenuIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuIngredientRepository extends JpaRepository<MenuIngredient, Long> {
    List<MenuIngredient> findByMenuItemMenuItemId(Long menuItemId);
}

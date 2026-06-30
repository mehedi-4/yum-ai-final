package com.yumai.config;

import com.yumai.entity.*;
import com.yumai.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Seeds demo users, menu, inventory (SRS 2.5: "menu items and ingredient lists
 * will be pre-seeded by the Admin") plus 60 days of synthetic order history so
 * dashboards and reports are demonstrable out of the box.
 * Disable with yumai.seed.enabled=false. Runs only on an empty database.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final OrderRepository orderRepository;
    private final BillRepository billRepository;
    private final WasteLogRepository wasteLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${yumai.seed.enabled:false}")
    private boolean enabled;

    @Value("${yumai.billing.tax-rate}")
    private double taxRate;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || userRepository.count() > 0) {
            return;
        }
        log.info("Seeding YumAI demo data...");

        User admin = userRepository.save(new User("Admin", "admin@yumai.com",
                passwordEncoder.encode("Admin@123"), Role.ADMIN));
        User manager = userRepository.save(new User("Manager", "manager@yumai.com",
                passwordEncoder.encode("Manager@123"), Role.MANAGER));
        User staff = userRepository.save(new User("Staff", "staff@yumai.com",
                passwordEncoder.encode("Staff@123"), Role.STAFF));
        userRepository.save(new User("Viewer", "viewer@yumai.com",
                passwordEncoder.encode("Viewer@123"), Role.VIEWER));

        InventoryItem chicken = inv("Chicken", 50, "kg", "Meat", 10);
        InventoryItem beef = inv("Beef", 30, "kg", "Meat", 8);
        InventoryItem rice = inv("Rice", 100, "kg", "Grains", 20);
        InventoryItem flour = inv("Flour", 60, "kg", "Grains", 15);
        InventoryItem cheese = inv("Cheese", 25, "kg", "Dairy", 5);
        InventoryItem milk = inv("Milk", 40, "L", "Dairy", 10);
        InventoryItem tomato = inv("Tomatoes", 35, "kg", "Vegetables", 8);
        InventoryItem lettuce = inv("Lettuce", 6, "kg", "Vegetables", 8); // intentionally low for the alert demo
        InventoryItem oil = inv("Cooking Oil", 45, "L", "Pantry", 10);
        InventoryItem cola = inv("Cola Syrup", 20, "L", "Beverages", 5);
        InventoryItem coffee = inv("Coffee Beans", 12, "kg", "Beverages", 3);

        MenuItem burger = menu("Classic Beef Burger", "Beef patty, cheese, lettuce, tomato", 8.99, 3.50, "Burgers",
                List.of(ing(beef, 0.15), ing(cheese, 0.03), ing(lettuce, 0.02), ing(tomato, 0.03), ing(flour, 0.08)));
        MenuItem chickenBurger = menu("Crispy Chicken Burger", "Fried chicken, lettuce, mayo", 7.99, 3.00, "Burgers",
                List.of(ing(chicken, 0.15), ing(lettuce, 0.02), ing(flour, 0.08), ing(oil, 0.05)));
        MenuItem pizza = menu("Margherita Pizza", "Tomato, mozzarella, basil", 11.99, 4.20, "Pizza",
                List.of(ing(flour, 0.25), ing(cheese, 0.12), ing(tomato, 0.10)));
        MenuItem biryani = menu("Chicken Biryani", "Fragrant rice with spiced chicken", 9.99, 3.80, "Rice Dishes",
                List.of(ing(chicken, 0.20), ing(rice, 0.25), ing(oil, 0.03)));
        MenuItem friedRice = menu("Vegetable Fried Rice", "Wok-fried rice with vegetables", 6.99, 2.20, "Rice Dishes",
                List.of(ing(rice, 0.25), ing(tomato, 0.05), ing(oil, 0.03)));
        MenuItem steak = menu("Grilled Beef Steak", "200g sirloin with sides", 15.99, 7.00, "Mains",
                List.of(ing(beef, 0.20), ing(oil, 0.02), ing(tomato, 0.05)));
        MenuItem colaDrink = menu("Cola", "Chilled fountain cola", 1.99, 0.40, "Beverages",
                List.of(ing(cola, 0.05)));
        MenuItem latte = menu("Cafe Latte", "Espresso with steamed milk", 3.49, 0.90, "Beverages",
                List.of(ing(coffee, 0.02), ing(milk, 0.20)));

        // 60 days of synthetic completed orders, weighted toward lunch/dinner peaks
        Random random = new Random(42);
        MenuItem[] items = {burger, chickenBurger, pizza, biryani, friedRice, steak, colaDrink, latte};
        double[] popularity = {0.20, 0.15, 0.16, 0.18, 0.08, 0.06, 0.10, 0.07};
        int[] hours = {12, 13, 13, 14, 19, 20, 20, 21, 11, 18, 15, 22};
        User[] creators = {staff, staff, manager, admin};

        for (int day = 60; day >= 1; day--) {
            LocalDate date = LocalDate.now().minusDays(day);
            boolean weekend = date.getDayOfWeek().getValue() >= 5;
            int orderCount = (weekend ? 12 : 7) + random.nextInt(7);
            for (int i = 0; i < orderCount; i++) {
                Order order = new Order();
                order.setTableNumber("T" + (1 + random.nextInt(15)));
                order.setCreatedBy(creators[random.nextInt(creators.length)]);
                order.setCreatedAt(date.atTime(hours[random.nextInt(hours.length)], random.nextInt(60)));
                int lines = 1 + random.nextInt(3);
                for (int l = 0; l < lines; l++) {
                    MenuItem pick = pickWeighted(items, popularity, random);
                    boolean dup = order.getItems().stream()
                            .anyMatch(oi -> oi.getMenuItem().getMenuItemId().equals(pick.getMenuItemId()));
                    if (!dup) {
                        order.getItems().add(new OrderItem(order, pick, 1 + random.nextInt(3)));
                    }
                }
                double subtotal = order.subtotal();
                double discount = random.nextInt(10) == 0 ? Math.round(subtotal * 10.0) / 100.0 : 0.0;
                double tax = Math.round((subtotal - discount) * taxRate * 100.0) / 100.0;
                order.setDiscountAmount(discount);
                order.setTaxAmount(tax);
                order.setTotalAmount(Math.round((subtotal - discount + tax) * 100.0) / 100.0);
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);

                Bill bill = new Bill(order);
                bill.setGeneratedAt(order.getCreatedAt());
                bill.setPaymentStatus(PaymentStatus.PAID);
                billRepository.save(bill);
            }
        }

        // a few waste log entries for reports
        String[] reasons = {"Spoiled", "Over-prepared", "Dropped during service", "Past expiry date"};
        InventoryItem[] wasteItems = {lettuce, tomato, milk, chicken};
        for (int day = 20; day >= 1; day -= 3) {
            InventoryItem item = wasteItems[(day / 3) % wasteItems.length];
            WasteLog wasteLog = new WasteLog(item, 0.5 + random.nextInt(3) * 0.5,
                    reasons[random.nextInt(reasons.length)], staff);
            wasteLog.setLoggedAt(LocalDate.now().minusDays(day).atTime(22, 0));
            wasteLogRepository.save(wasteLog);
        }

        log.info("Seeding complete: {} orders, {} bills. Demo logins: admin@yumai.com / Admin@123, "
                        + "manager@yumai.com / Manager@123, staff@yumai.com / Staff@123, viewer@yumai.com / Viewer@123",
                orderRepository.count(), billRepository.count());
    }

    private InventoryItem inv(String name, double qty, String unit, String category, double threshold) {
        return inventoryItemRepository.save(new InventoryItem(name, qty, unit, category, threshold));
    }

    private record Ing(InventoryItem item, double qty) {
    }

    private static Ing ing(InventoryItem item, double qty) {
        return new Ing(item, qty);
    }

    private MenuItem menu(String name, String description, double price, double cost, String category,
                          List<Ing> ingredients) {
        MenuItem item = new MenuItem(name, description, price, cost, category);
        for (Ing ing : ingredients) {
            item.getIngredients().add(new MenuIngredient(item, ing.item(), ing.qty()));
        }
        return menuItemRepository.save(item);
    }

    private static MenuItem pickWeighted(MenuItem[] items, double[] weights, Random random) {
        double r = random.nextDouble();
        double acc = 0;
        for (int i = 0; i < items.length; i++) {
            acc += weights[i];
            if (r <= acc) {
                return items[i];
            }
        }
        return items[items.length - 1];
    }
}

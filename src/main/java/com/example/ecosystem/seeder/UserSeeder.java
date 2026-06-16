package com.example.ecosystem.seeder;

import com.example.ecosystem.Entity.Role;
import com.example.ecosystem.Entity.User;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class UserSeeder {

    private final Faker faker = new Faker(new Locale("en"));

    
    public List<User> generateUsers(int count) {
        List<User> users = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            User user = new User();

            String baseName = faker.name().firstName().toLowerCase() + "." + faker.name().lastName().toLowerCase();
            String username = baseName + "_" + i;
            
            user.setUsername(username);
            user.setEmail(username + "@ecosystem.local");
            user.setPassword(faker.internet().password());
            user.setRole(Role.customer);

            users.add(user);

            if (i % 50 == 0) {
                System.out.println("Generated " + i + " users so far...");
            }
        }
        System.out.println("Successfully generated " + count + " users.");
        return users;
    }
}
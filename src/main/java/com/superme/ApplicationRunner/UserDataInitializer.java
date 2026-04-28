package com.superme.ApplicationRunner;

import com.superme.admin.model.Admin;
import com.superme.admin.model.AdminPassword;
import com.superme.admin.repository.AdminRepository;
import com.superme.enums.Department;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import com.superme.model.User;
import com.superme.model.UserPassword;
import com.superme.repository.UserPasswordRepository;
import com.superme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class UserDataInitializer {

    private final UserRepository userRepository;
    private final UserPasswordRepository userPasswordRepository;
    @Autowired
    private AdminRepository adminRepository;
    // ✅ Your given bcrypt password
    private static final String DEFAULT_PASSWORD =
            "$2a$10$0vWLU97zWltDrcS0/r8XhergQ6GJeI3/qH.so0Zalu2rJUoWcUxZu";

    @Bean
    public ApplicationRunner initUser() {
        return args -> {

            long userCount = userRepository.count();

            if (userCount > 0) {
                System.out.println("USER already exist. Skipping admin creation.");
                return;
            }

            User user = new User();
            user.setName("user5ktech.com");
            user.setEmail("user@5ktech.com");
            user.setPhone("123456789");
            user.setDateOfBirth(LocalDate.now());
            user.setGender("MALE");
            user.setRelationship(Relationship.SELF);
            user.setRole( Role.USER);
            User saved =  userRepository.save(user);
            UserPassword password = new UserPassword();
            password.setPassword("$2a$10$0vWLU97zWltDrcS0/r8XhergQ6GJeI3/qH.so0Zalu2rJUoWcUxZu");
            password.setUser(saved);
            userPasswordRepository.save(password);

            System.out.println("✅ Default  USER created successfully! with email = user@5ktech.com and password = Admin@2025");
        };
    }

//    @Bean
//    public ApplicationRunner initSuperAdmin() {
//        return args -> {
//
//
//
//            long userCount = adminRepository.count();
////
////            // ✅ Only if DB is empty
//            if (userCount > 0) {
//                System.out.println("ADMIN already exist. Skipping admin creation.");
//                return;
//            }
//
//
//
//
//            Admin admin = new Admin();
//            admin.setFullName("admin@5ktech");
//            admin.setEmail("superAdmin@5ktech");
//            admin.setPhone("123456789");
//            admin.setDepartment(Department.HR);
//            admin.setDesignation("HR");
//            admin.setRole( Role.SUPER_ADMIN);
//            admin.setExpiryDate(LocalDateTime.now().plusYears(1));
//
//            AdminPassword password = new AdminPassword();
//            password.setHashedPassword("$2a$10$0vWLU97zWltDrcS0/r8XhergQ6GJeI3/qH.so0Zalu2rJUoWcUxZu");
//            password.setCreatedAt(LocalDateTime.now());
//            password.setActive(true);
//
//            admin.setPassword(password);
//            adminRepository.save(admin);
//
//            System.out.println("✅ Default ADMIN user created successfully! with email = superAdmin@5ktech and password = Admin@2025" );
//        };
//    }
//
//    @Bean
//    public ApplicationRunner initAdmin() {
//        return args -> {
//
//            long userCount = adminRepository.count();
////            // ✅ Only if DB is empty
//            if (userCount > 0) {
//                System.out.println("ADMIN already exist. Skipping admin creation.");
//                return;
//            }
//
//            Admin admin = new Admin();
//            admin.setFullName("admin@5ktech");
//            admin.setEmail("Admin@5ktech");
//            admin.setPhone("123456789");
//            admin.setDepartment(Department.HR);
//            admin.setDesignation("HR");
//            admin.setRole( Role.ADMIN);
//            admin.setExpiryDate(LocalDateTime.now().plusYears(1));
//
//            AdminPassword password = new AdminPassword();
//            password.setHashedPassword("$2a$10$0vWLU97zWltDrcS0/r8XhergQ6GJeI3/qH.so0Zalu2rJUoWcUxZu");
//            password.setCreatedAt(LocalDateTime.now());
//            password.setActive(true);
//
//            admin.setPassword(password);
//            adminRepository.save(admin);
//
//            System.out.println("✅ Default ADMIN user created successfully! with email = admin@5ktech and password = Admin@2025" );
//        };
//    }


@Bean
public ApplicationRunner initSuperAdmin() {
    return args -> {

        boolean superAdminExists = adminRepository.existsByRole(Role.SUPER_ADMIN);

        if (superAdminExists) {
            System.out.println("SUPER_ADMIN already exists. Skipping...");
            return;
        }

        Admin admin = new Admin();
        admin.setFullName("superadmin@5ktech");
        admin.setEmail("superAdmin@5ktech.com");
        admin.setPhone("123456789");
        admin.setDepartment(Department.HR);
        admin.setDesignation("HR");
        admin.setRole(Role.SUPER_ADMIN);
        admin.setExpiryDate(LocalDateTime.now().plusYears(1));

        AdminPassword password = new AdminPassword();
        password.setHashedPassword(DEFAULT_PASSWORD);
        password.setCreatedAt(LocalDateTime.now());
        password.setActive(true);

        admin.setPassword(password);
        adminRepository.save(admin);

        System.out.println("✅  Default SUPER ADMIN  created successfully! with email = superAdmin@5ktech.com and password = Admin@2025!");
    };
}

    @Bean
    public ApplicationRunner initAdmin() {
        return args -> {

            boolean adminExists = adminRepository.existsByRole(Role.ADMIN);

            if (adminExists) {
                System.out.println("ADMIN already exists. Skipping...");
                return;
            }

            Admin admin = new Admin();
            admin.setFullName("admin@5ktech");
            admin.setEmail("admin@5ktech.com");
            admin.setPhone("123456789");
            admin.setDepartment(Department.HR);
            admin.setDesignation("HR");
            admin.setRole(Role.ADMIN);
            admin.setExpiryDate(LocalDateTime.now().plusYears(1));

            AdminPassword password = new AdminPassword();
            password.setHashedPassword(DEFAULT_PASSWORD);
            password.setCreatedAt(LocalDateTime.now());
            password.setActive(true);

            admin.setPassword(password);
            adminRepository.save(admin);

            System.out.println("✅ Default ADMIN  created successfully! with email = admin@5ktech and password = Admin@2025!");
        };
    }

}
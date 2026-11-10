package qltv.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * Mật khẩu được lưu và so sánh dạng chữ thường (không mã hóa) để trang quản lý
     * hiển thị được mật khẩu cụ thể. Chỉ phù hợp cho bản demo / đồ án.
     * Muốn quay lại mã hóa: đổi thành  return new BCryptPasswordEncoder();
     */
    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/forgot-password", "/style.css").permitAll()
                .requestMatchers("/accounts/**").hasRole("ADMIN")
                .requestMatchers("/readers/**", "/statistics/**").hasAnyRole("ADMIN", "LIBRARIAN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        return http.build();
    }
}

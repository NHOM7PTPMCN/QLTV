
package qltv.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * Mật khẩu được lưu và so sánh dạng chữ thường (không mã hóa) để trang quản lý
     * hiển thị được mật khẩu cụ thể. Chỉ phù hợp cho bản demo / đồ án.
     * Muốn quay lại mã hóa: đổi thành return new BCryptPasswordEncoder();
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
                // Các trang không cần đăng nhập
                .requestMatchers(
                    "/login",
                    "/register",
                    "/forgot-password",
                    "/style.css"
                ).permitAll()

                // Quản lý tài khoản: chỉ ADMIN
                .requestMatchers("/accounts/**")
                    .hasRole("ADMIN")

                // Quản lý độc giả và thống kê: ADMIN, LIBRARIAN
                .requestMatchers("/readers/**", "/statistics/**")
                    .hasAnyRole("ADMIN", "LIBRARIAN")

                // Xem và tìm kiếm sách: tất cả người dùng đã đăng nhập
                .requestMatchers(HttpMethod.GET, "/books", "/books/**")
                    .authenticated()

                // Thêm, sửa, xóa sách: ADMIN và LIBRARIAN
                .requestMatchers(HttpMethod.POST, "/books/**")
                    .hasAnyRole("ADMIN", "LIBRARIAN")

                // Các trang và chức năng còn lại cần đăng nhập
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
            );

        return http.build();
    }
}

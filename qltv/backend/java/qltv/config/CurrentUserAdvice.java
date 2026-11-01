package qltv.config;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import qltv.account.Role;

/** Đưa giá trị vai trò (0, 1, 2) của người đang đăng nhập vào mọi trang: ${roleCode}. */
@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute("roleCode")
    public Integer roleCode(Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return null;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            for (Role r : Role.values()) {
                if (ga.getAuthority().equals("ROLE_" + r.name())) {
                    return r.getCode();
                }
            }
        }
        return null;
    }
}

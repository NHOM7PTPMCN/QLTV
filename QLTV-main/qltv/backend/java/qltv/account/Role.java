package qltv.account;

/**
 * Giá trị vai trò (lưu vào CSDL và dùng để ẩn/hiện chức năng):
 *   0 = Quản trị viên, 1 = Thủ thư, 2 = Độc giả.
 * Số càng nhỏ thì quyền càng lớn.
 */
public enum Role {
    ADMIN(0, "Quản trị viên"),
    LIBRARIAN(1, "Thủ thư"),
    READER(2, "Độc giả");

    private final int code;
    private final String label;

    Role(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() { return code; }
    public String getLabel() { return label; }

    public static Role fromCode(int code) {
        for (Role r : values()) {
            if (r.code == code) return r;
        }
        throw new IllegalArgumentException("Vai trò không hợp lệ: " + code);
    }
}

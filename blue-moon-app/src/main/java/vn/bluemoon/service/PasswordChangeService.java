package vn.bluemoon.service;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.repository.UserRepository;

import java.time.LocalDate;

/**
 * Service để quản lý yêu cầu đổi mật khẩu
 */
public class PasswordChangeService {
    private final UserRepository userRepository = new UserRepository();

    /**
     * Yêu cầu user đổi mật khẩu ngay lập tức
     * @param userId User ID
     * @throws DbException if database error occurs
     */
    public void requirePasswordChangeNow(Integer userId) throws DbException {
        User user = userRepository.findById(userId);
        if (user != null) {
            user.setMustChangePassword(true);
            user.setPasswordChangeRequiredDate(LocalDate.now());
            userRepository.update(user);
        }
    }

    /**
     * Thiết lập yêu cầu đổi mật khẩu tại một ngày cụ thể
     * @param userId User ID
     * @param requiredDate Ngày yêu cầu đổi mật khẩu
     * @throws DbException if database error occurs
     */
    public void requirePasswordChangeOnDate(Integer userId, LocalDate requiredDate) throws DbException {
        User user = userRepository.findById(userId);
        if (user != null) {
            user.setMustChangePassword(true);
            user.setPasswordChangeRequiredDate(requiredDate);
            userRepository.update(user);
        }
    }

    /**
     * Thiết lập định kỳ đổi mật khẩu (số ngày)
     * @param userId User ID
     * @param periodDays Số ngày định kỳ phải đổi mật khẩu (ví dụ: 30, 60, 90)
     * @throws DbException if database error occurs
     */
    public void setPasswordChangePeriod(Integer userId, Integer periodDays) throws DbException {
        User user = userRepository.findById(userId);
        if (user != null) {
            user.setPasswordChangePeriodDays(periodDays);
            // Nếu chưa có last_password_change_date, set bằng ngày hiện tại
            if (user.getLastPasswordChangeDate() == null) {
                user.setLastPasswordChangeDate(LocalDate.now());
            }
            userRepository.update(user);
        }
    }

    /**
     * Hủy yêu cầu đổi mật khẩu
     * @param userId User ID
     * @throws DbException if database error occurs
     */
    public void cancelPasswordChangeRequirement(Integer userId) throws DbException {
        User user = userRepository.findById(userId);
        if (user != null) {
            user.setMustChangePassword(false);
            user.setPasswordChangeRequiredDate(null);
            user.setPasswordChangePeriodDays(null);
            userRepository.update(user);
        }
    }

    /**
     * Kiểm tra xem user có cần đổi mật khẩu không
     * @param user User
     * @return true nếu cần đổi mật khẩu
     */
    public boolean needsPasswordChange(User user) {
        if (user == null) {
            return false;
        }

        // Kiểm tra yêu cầu đổi mật khẩu ngay lập tức
        if (Boolean.TRUE.equals(user.getMustChangePassword())) {
            // Nếu có ngày yêu cầu cụ thể, kiểm tra xem đã đến ngày chưa
            if (user.getPasswordChangeRequiredDate() != null) {
                LocalDate today = LocalDate.now();
                if (today.isAfter(user.getPasswordChangeRequiredDate()) || 
                    today.isEqual(user.getPasswordChangeRequiredDate())) {
                    return true;
                }
            } else {
                // Không có ngày cụ thể, yêu cầu đổi ngay
                return true;
            }
        }

        // Kiểm tra định kỳ đổi mật khẩu
        if (user.getPasswordChangePeriodDays() != null && user.getLastPasswordChangeDate() != null) {
            LocalDate today = LocalDate.now();
            LocalDate nextChangeDate = user.getLastPasswordChangeDate()
                .plusDays(user.getPasswordChangePeriodDays());
            if (today.isAfter(nextChangeDate) || today.isEqual(nextChangeDate)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Cập nhật ngày đổi mật khẩu lần cuối
     * @param userId User ID
     * @throws DbException if database error occurs
     */
    public void updateLastPasswordChangeDate(Integer userId) throws DbException {
        User user = userRepository.findById(userId);
        if (user != null) {
            user.setLastPasswordChangeDate(LocalDate.now());
            // Nếu đã đổi mật khẩu, hủy yêu cầu đổi mật khẩu ngay lập tức
            if (user.getPasswordChangeRequiredDate() != null) {
                LocalDate today = LocalDate.now();
                if (today.isAfter(user.getPasswordChangeRequiredDate()) || 
                    today.isEqual(user.getPasswordChangeRequiredDate())) {
                    user.setMustChangePassword(false);
                    user.setPasswordChangeRequiredDate(null);
                }
            }
            userRepository.update(user);
        }
    }
}


package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import com.jubayer.cholobodlai.Service.AccountRecoveryService;
import com.jubayer.cholobodlai.Service.EmailDeliveryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AccountRecoveryWebController {
    private final AccountRecoveryService recovery;
    private final EmailDeliveryService mail;
    public AccountRecoveryWebController(AccountRecoveryService recovery, EmailDeliveryService mail) {
        this.recovery = recovery; this.mail = mail;
    }
    @GetMapping("/forgot-password")
    public String forgotForm() { return "auth/forgot-password"; }
    @PostMapping("/forgot-password")
    public String requestReset(@RequestParam String email) {
        recovery.requestPasswordReset(email);
        return "redirect:/forgot-password?sent";
    }
    @GetMapping("/resend-verification")
    public String resendForm() { return "auth/resend-verification"; }
    @PostMapping("/resend-verification")
    public String resend(@RequestParam String email) {
        try {
            recovery.resendVerification(email);
            return "redirect:/resend-verification?sent";
        } catch (IllegalStateException unavailable) {
            return "redirect:/resend-verification?mailUnavailable";
        }
    }
    @GetMapping("/verify-email")
    public String verify(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("verified", recovery.verify(token));
        return "auth/verification-result";
    }
    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        boolean valid = recovery.validResetToken(token);
        model.addAttribute("valid", valid);
        model.addAttribute("token", valid ? token : "");
        return "auth/reset-password";
    }
    @PostMapping("/reset-password")
    public String reset(@RequestParam String token, @RequestParam String newPassword,
                        @RequestParam String confirmPassword) {
        recovery.resetPassword(token, newPassword, confirmPassword);
        return "redirect:/login?resetSuccess";
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/mail-preview")
    public String preview(Model model) {
        if (!mail.previewEnabled()) throw new BusinessRuleException("Local mail preview is disabled");
        model.addAttribute("emails", mail.latestPreviews());
        return "admin/mail-preview";
    }
}

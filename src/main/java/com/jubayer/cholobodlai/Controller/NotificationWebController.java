package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.Repository.NotificationRepository;
import com.jubayer.cholobodlai.Service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notifications")
public class NotificationWebController {
    private final NotificationRepository repository;
    private final CurrentUserService current;
    public NotificationWebController(NotificationRepository repository, CurrentUserService current) {
        this.repository = repository; this.current = current;
    }
    @GetMapping
    public String page(Model model) {
        model.addAttribute("items", repository.findTop100ByUserIdOrderByCreatedAtDesc(current.currentUser().getId()));
        return "notifications";
    }
    @PostMapping("/{id}/read")
    public String read(@PathVariable String id) {
        var item = repository.findById(id).orElseThrow();
        if (!item.getUserId().equals(current.currentUser().getId()))
            throw new AccessDeniedException("You cannot modify another user's notification");
        item.setRead(true); repository.save(item);
        return "redirect:/notifications";
    }
}

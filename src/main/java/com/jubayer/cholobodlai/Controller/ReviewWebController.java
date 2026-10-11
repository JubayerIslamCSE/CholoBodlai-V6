package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.Service.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ReviewWebController {
    private final ReviewService reviews;
    public ReviewWebController(ReviewService reviews) { this.reviews = reviews; }
    @PostMapping("/trips/{id}/review")
    public String submit(@PathVariable String id, @RequestParam int stars,
                         @RequestParam(required = false) String comment) {
        reviews.submit(id, stars, comment);
        return "redirect:/trips/" + id + "?reviewed#delivery-reviews";
    }
}

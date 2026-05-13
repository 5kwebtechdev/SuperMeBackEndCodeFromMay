package com.superme.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountDeletionPageController {

    // This will serve the page at https://api.supermeapp.com/delete-account
    @GetMapping("/delete-account")
    public String showDeleteAccountPage() {
        // Returns the name of the HTML template file (without .html)
        return "delete-account";
    }
}
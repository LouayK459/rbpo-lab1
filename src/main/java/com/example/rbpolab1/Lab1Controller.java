package com.example.rbpolab1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Lab1Controller {

    @GetMapping("/hello")
    public String hello() {
        return "Hello!";
    }

    @GetMapping("/number")
    public int number() {
        return 42;
    }
}
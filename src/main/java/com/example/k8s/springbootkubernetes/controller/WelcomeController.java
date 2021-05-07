package com.example.k8s.springbootkubernetes.controller;

import com.example.k8s.springbootkubernetes.util.DateManager;
import org.springframework.web.bind.annotation.*;

@RestController
public class WelcomeController {
	@RequestMapping("/greeting")
	public String greeting() {
		System.out.println("In WelcomeController.greeting");
		return "Hello! Current time is: " + new DateManager().getCurrentDate();
	}
}

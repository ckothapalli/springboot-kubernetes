package com.example.k8s.springbootkubernetes.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("hostpath")
public class HostpathWelcomeController extends  AbstractWelcomeController{
	public String getGreetingsFolder(){
		return "/tmp/host";
	}
}
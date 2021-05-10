package com.example.k8s.springbootkubernetes.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("emptydir")
public class EmptydirWelcomeController extends  AbstractWelcomeController{

	//private String hostPath = "/host/tmp/greetingsFile.txt";

	public String getGreetingsFolder(){
		return "/tmp/pod_temp";
	}
}
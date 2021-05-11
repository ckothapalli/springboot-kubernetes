package com.example.k8s.springbootkubernetes.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample controller for PersistentVolume sample
 */
@RestController
@RequestMapping("pv")
public class PersistentVolumeWelcomeController extends  AbstractWelcomeController{
	public String getGreetingsFolder(){
		return "/tmp/pv";
	}
}
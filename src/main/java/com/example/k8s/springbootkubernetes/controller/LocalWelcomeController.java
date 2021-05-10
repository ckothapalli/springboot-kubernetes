package com.example.k8s.springbootkubernetes.controller;

import com.example.k8s.springbootkubernetes.util.DateManager;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("local")
public class LocalWelcomeController extends  AbstractWelcomeController{

	//private String hostPath = "/host/tmp/greetingsFile.txt";

	public String getGreetingsFolder(){
		return "/tmp";
	}
}
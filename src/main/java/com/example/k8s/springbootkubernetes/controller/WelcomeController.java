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
public class WelcomeController {

	private String filePath = "/pv/tmp/greetingsFile.txt";
	
	@RequestMapping("/greeting")
	public String greeting() {
		System.out.println("In WelcomeController.greeting");
		return "Hello! Current time is: " + new DateManager().getCurrentDate();
	}

	@GetMapping("/all_greetings")
	List<String> all_greetings() {
		List<String> allGreetings = new ArrayList<String>();
		try (BufferedReader br =
					 new BufferedReader(new FileReader(filePath))) {
			String line = "";
			while( (line = br.readLine()) != null){
				allGreetings.add(line);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return allGreetings;
	}
	// end::get-aggregate-root[]

	@PostMapping("/all_greetings")
	String newGreeting(@RequestBody String greeting) {
		try (BufferedWriter bw =
					 new BufferedWriter(new FileWriter(filePath, true))) {
			bw.write(greeting + " in host:" + getHostName());
			bw.newLine();
		} catch (Exception e){
			e.printStackTrace();
		}
		return greeting + " saved successfully";
	}

	private String getHostName(){
		String hostName = "noHost";
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (Exception e){
			e.printStackTrace();
		}
		return hostName;
	}
}

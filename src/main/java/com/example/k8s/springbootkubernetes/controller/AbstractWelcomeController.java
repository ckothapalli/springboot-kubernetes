package com.example.k8s.springbootkubernetes.controller;

import com.example.k8s.springbootkubernetes.util.DateManager;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWelcomeController {

    //private String hostPath = "/host/tmp/greetingsFile.txt";

    protected abstract String getGreetingsFolder();

    private String getFilePath(){
        return getGreetingsFolder() + "/greetingsFile.txt";
    }

    @RequestMapping("/greeting")
    public String greeting() {
        System.out.println("In WelcomeController.greeting");
        return "Hello! Current time is: " + new DateManager().getCurrentDate();
    }

    @GetMapping("/get_all_greetings")
    List<String> all_greetings() {
        List<String> allGreetings = new ArrayList<String>();
        try (BufferedReader br =
                     new BufferedReader(new FileReader(getFilePath() )) ) {
            String line = "";
            while( (line = br.readLine()) != null){
                allGreetings.add(line);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return allGreetings;
    }

    @PostMapping("/post_greetings")
    String postGreeting(@RequestBody String greeting) {
        //Create the directory where the greetings file is stored if it does not exist
        File directory = new File(getGreetingsFolder());
        if (! directory.exists()){
            directory.mkdirs();
        }

        //Write to the greetings file
        try (BufferedWriter bw =
                     new BufferedWriter(new FileWriter(getFilePath(), true))) {
            bw.write(greeting + " stored in host:" + getHostName() + " on "
                    + new DateManager().getCurrentDate() + " in file " + getFilePath());
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
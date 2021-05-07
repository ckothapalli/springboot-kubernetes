package com.example.k8s.springbootkubernetes.util;

import java.util.Date;

public class DateManager {
    public String getCurrentDate(){
        return new Date().toString();
    }
}

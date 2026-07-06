package com.example.graduationevaluationsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.graduationevaluationsystem.mapper")
public class GraduationEvaluationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraduationEvaluationSystemApplication.class, args);
    }

}

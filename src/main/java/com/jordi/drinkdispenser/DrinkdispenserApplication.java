package com.jordi.drinkdispenser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class DrinkdispenserApplication {

	public static void main(String[] args) {
		SpringApplication.run(DrinkdispenserApplication.class, args);
	}

}

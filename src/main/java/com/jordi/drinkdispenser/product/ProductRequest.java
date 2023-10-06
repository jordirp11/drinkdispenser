package com.jordi.drinkdispenser.product;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ProductRequest (String name, Integer price, Integer quantity, @JsonFormat(pattern="yyyy-MM-dd") LocalDate expirationDate) { }

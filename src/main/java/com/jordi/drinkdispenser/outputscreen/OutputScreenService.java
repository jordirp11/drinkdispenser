package com.jordi.drinkdispenser.outputscreen;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

@Service
public class OutputScreenService {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> future = null;

    private String display = null;

    public void display(final String message, final Object ... params) {        
        future = executor.submit(() -> { 
            display = String.format(message, params);    
        });
    }

    public String getLatestDisplay() throws InterruptedException, ExecutionException {
        future.get();        
        return display;
    }
    
}

package com.jordi.drinkdispenser.purchase.timeout;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jordi.drinkdispenser.purchase.PurchaseResult;
import com.jordi.drinkdispenser.purchase.PurchaseService;

@Aspect
@Component
public class PurchaseTimeoutAspect {
    public static Integer TEST_TIME_LIMIT = null;

    @Autowired
    public PurchaseService purchaseService;

    @Around("@annotation(purchaseTimeout)")
    public Object purchaseTimeout(ProceedingJoinPoint joinPoint, PurchaseTimeout purchaseTimeout) throws Throwable {        
        long startTime = System.currentTimeMillis();
        
        Object result = joinPoint.proceed();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        int timeLimit = TEST_TIME_LIMIT == null ? purchaseTimeout.value() : TEST_TIME_LIMIT;
        
        if (executionTime > timeLimit) {
            if (result instanceof PurchaseResult) {
                return purchaseService.purchaseTimeout((PurchaseResult)result);                
            } else {
                throw new IllegalStateException("Annotation purchaseTimeout is only valid for purchaseResult methods");
            }
        }
        
        return result;
    }

}

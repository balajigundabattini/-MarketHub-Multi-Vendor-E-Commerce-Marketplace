package com.markethub.payments.application;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
public final class PaymentAmounts {
    private PaymentAmounts() { }
    public static long minorUnits(BigDecimal amount,String currency) {
        int digits=Currency.getInstance(currency).getDefaultFractionDigits();
        if(digits<0) throw new IllegalArgumentException("Currency does not define minor units");
        return amount.movePointRight(digits).setScale(0,RoundingMode.HALF_UP).longValueExact();
    }
}

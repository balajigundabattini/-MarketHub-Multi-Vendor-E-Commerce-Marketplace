package com.markethub.payments.application;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
class PaymentAmountsTest {
    @Test void convertsTwoDecimalCurrenciesToMinorUnits(){assertEquals(12999,PaymentAmounts.minorUnits(new BigDecimal("129.99"),"INR"));}
    @Test void supportsZeroDecimalCurrencies(){assertEquals(130,PaymentAmounts.minorUnits(new BigDecimal("129.5"),"JPY"));}
    @Test void supportsThreeDecimalCurrencies(){assertEquals(129987,PaymentAmounts.minorUnits(new BigDecimal("129.987"),"KWD"));}
}

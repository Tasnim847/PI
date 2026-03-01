//package org.example.projet_pi.Service;
//
//import com.stripe.Stripe;
//import com.stripe.exception.StripeException;
//import com.stripe.model.PaymentIntent;
//import com.stripe.param.PaymentIntentCreateParams;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import jakarta.annotation.PostConstruct;
//import java.math.BigDecimal;
//
//@Service
//public class StripeRepaymentService {
//
//    @Value("${stripe.api.key}")
//    private String stripeApiKey;
//
//    @PostConstruct
//    public void init() {
//        Stripe.apiKey = stripeApiKey;
//    }
//
//    public PaymentIntent createPaymentIntent(Long creditId, BigDecimal amount, String currency)
//            throws StripeException {
//
//        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
//                .setAmount(amount.multiply(new BigDecimal("100")).longValue()) // En centimes
//                .setCurrency(currency)
//                .putMetadata("creditId", creditId.toString())
//                .build();
//
//        return PaymentIntent.create(params);
//    }
//
//    public PaymentIntent confirmPaymentIntent(String paymentIntentId) throws StripeException {
//        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
//        return paymentIntent.confirm();
//    }
//}
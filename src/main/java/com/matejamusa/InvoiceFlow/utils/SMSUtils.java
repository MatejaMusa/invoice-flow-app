package com.matejamusa.InvoiceFlow.utils;

import com.matejamusa.InvoiceFlow.exception.ApiException;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class SMSUtils {
    public static void sendSMS(SnsClient snsClient, String message, String phoneNumber) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    PublishRequest request = PublishRequest.builder()
                            .message(message)
                            .phoneNumber(phoneNumber)
                            .build();

                    PublishResponse result = snsClient.publish(request);
                    System.out.println(result.messageId() + " Message sent. Status was " + result.sdkHttpResponse().statusCode());
                } catch (SnsException e) {
                    System.err.println(e.awsErrorDetails().errorMessage());
                } catch (Exception e) {
                    throw new ApiException("Unable to send email");
                }
            }
        });
    }
}

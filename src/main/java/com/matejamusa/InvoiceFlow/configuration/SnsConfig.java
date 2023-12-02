package com.matejamusa.InvoiceFlow.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
@RequiredArgsConstructor
public class SnsConfig {

    @Value("${aws.access.key}")
    private String AWS_ACCESS_KEY;

    @Value("${aws.secret.key}")
    private String AWS_SECRET_KEY;

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of("eu-central-1"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY)))
                .build();

    }
}

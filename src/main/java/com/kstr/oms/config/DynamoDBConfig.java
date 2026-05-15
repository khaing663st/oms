package com.kstr.oms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

/**
 * Configuration class for AWS DynamoDB client setup.
 **/
@Configuration
public class DynamoDBConfig {

    @Value("${env}")
    private String env;

    @Value("${aws.dynamodb.endpoint}")
    private String dynamoDBEndpoint;

    @Value("${aws.dynamodb.region}")
    private String region;

    @Value("${aws.access.key.id}")
    private String awsAccessKeyId;

    @Value("${aws.secret.access.key}")
    private String awsSecretAccessKey;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region));

        if ("local".equalsIgnoreCase(env)) {
            builder.endpointOverride(URI.create(dynamoDBEndpoint));
        }

        if (StringUtils.hasText(awsAccessKeyId) && StringUtils.hasText(awsSecretAccessKey)) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey)
            ));
        }

        return builder.build();
    }
}


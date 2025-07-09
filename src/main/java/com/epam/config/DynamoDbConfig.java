package com.epam.config;

import com.epam.edai.run8.team15.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

import static software.amazon.awssdk.enhanced.dynamodb.TableSchema.fromBean;

@Configuration
public class DynamoDbConfig {

    @Value("${aws.accessKeyId}")
    private String accessKeyId;

    @Value("${aws.secretAccessKey}")
    private String secretAccessKey;

    @Value("${aws.sessionToken}")
    private String sessionToken;

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${dynamodb.tables.user}")
    private String userTableName;

    @Value("${aws.roleArn}")
    private String roleArn;

    @Value("${dynamodb.tables.waiter}")
    private String waiterTableName;

    @Value("${dynamodb.tables.dish}")
    private String dishTableName;

    @Value("${dynamodb.tables.location}")
    private String locationTableName;

    @Value("${dynamodb.tables.feedback}")
    private String feedbackTableName;

    @Value("${dynamodb.tables.restable}")
    private String resTableName;

    @Value("${dynamodb.tables.report}")
    private String reportTableName;

    @Value("${dynamodb.tables.reservation}")
    private String reservationTableName;


    @Bean
    public DynamoDbClient dynamoDbClient() {
        // Step 1: Use initial temporary session credentials
        AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                accessKeyId,
                secretAccessKey,
                sessionToken
        );

        // Step 2: Create STS client using the initial session credentials
        StsClient stsClient = StsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(sessionCredentials))
                .build();

        // Step 3: Assume Role credentials provider
        StsAssumeRoleCredentialsProvider assumeRoleCredentialsProvider = StsAssumeRoleCredentialsProvider.builder()
                .refreshRequest(r -> r.roleArn(roleArn).roleSessionName("assume-role-session"))
                .stsClient(stsClient)
                .build();

        // Step 4: Create DynamoDB client using assumed credentials
        return DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(assumeRoleCredentialsProvider)
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public DynamoDbTable<User> userTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(userTableName, fromBean(User.class));
    }

    @Bean
    public DynamoDbTable<Waiter> waiterTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(waiterTableName, fromBean(Waiter.class));
    }

    @Bean
    public DynamoDbTable<Dish> dishTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(dishTableName, fromBean(Dish.class));
    }

    @Bean
    public DynamoDbTable<Location> locationTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(locationTableName, fromBean(Location.class));
    }

    @Bean
    public DynamoDbTable<Reservation> reservationTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(reservationTableName, fromBean(Reservation.class));
    }

    @Bean
    public DynamoDbTable<Feedback> feedbackTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(feedbackTableName, fromBean(Feedback.class));
    }

    @Bean
    public DynamoDbTable<ResTable> resTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(resTableName, fromBean(ResTable.class));
    }

    @Bean
    public DynamoDbTable<Report> reportTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(reportTableName, fromBean(Report.class));
    }
}

package com.fraudwatch.test;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

public final class InfrastructureContainers {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16");
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");
    private static final DockerImageName RABBIT_IMAGE = DockerImageName.parse("rabbitmq:3.13-management");

    private InfrastructureContainers() {
    }

    public static PostgreSQLContainer<?> postgres(String databaseName) {
        return new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName(databaseName)
            .withUsername("fraudwatch")
            .withPassword("fraudwatch");
    }

    public static RabbitMQContainer rabbitMq() {
        return new RabbitMQContainer(RABBIT_IMAGE)
            .withUser("fraudwatch", "fraudwatch");
    }

    public static GenericContainer<?> redis() {
        return new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379);
    }
}

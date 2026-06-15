import com.github.gradle.node.npm.task.NpmTask

plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("com.github.node-gradle.node") version "7.0.2"
}

node {
    version.set("22.14.0")
    npmVersion.set("10.9.2")
    download.set(true)
}

group = "com.guessmelody"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Database (H2 для разработки, PostgreSQL для продакшена)
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Spotify Web API Java wrapper
    implementation("se.michaelthelin.spotify:spotify-web-api-java:8.3.6")

    // Загрузка .env файла в Spring Environment
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // Lombok — убирает бойлерплейт
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Тесты
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:2.1.5")
}

tasks.named<NpmTask>("npmInstall") {
    workingDir.set(file("frontend"))
}

tasks.register<NpmTask>("npmBuild") {
    dependsOn("npmInstall")
    group = "build"
    description = "Builds the React frontend"
    workingDir.set(file("frontend"))
    npmCommand.set(listOf("run", "build"))
}

tasks.processResources {
    dependsOn("npmBuild")
    from("frontend/dist") {
        into("static")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Test> {
    useJUnitPlatform()
}

plugins {
	java
	id("org.springframework.boot") version "3.1.3"
	id("io.spring.dependency-management") version "1.1.3"
}

group = "com.rhododendra"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
	implementation("org.apache.lucene:lucene-core:9.7.0")
	implementation("org.apache.lucene:lucene-queryparser:9.7.0")
	implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
	implementation("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("io.zonky.test:embedded-postgres:2.1.0")
	testImplementation(enforcedPlatform("io.zonky.test.postgres:embedded-postgres-binaries-bom:16.6.0"))
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty(
		"lucene.index.base",
		layout.buildDirectory.dir("test-lucene").get().asFile.absolutePath
	)
}

tasks.register<JavaExec>("migrateAndIndex") {
	group = "application"
	description =
		"Migrates JSON from the hirsutum scraper output directory into PostgreSQL, then rebuilds Lucene indexes under ./index"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.rhododendra.cli.MigrateAndIndexApplication")
	workingDir = project.layout.projectDirectory.asFile

	val dataJsonDir =
		(project.findProperty("dataJsonDir") as String?)
			?: "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/"
	val domain =
		(project.findProperty("domain") as String?)
			?: "https://rhododendra.com"
	val springDatasourceUrl =
		(project.findProperty("springDatasourceUrl") as String?)
			?: System.getenv("SPRING_DATASOURCE_URL") ?: "jdbc:postgresql://localhost:5432/rhododendra"
	val springDatasourceUsername =
		(project.findProperty("springDatasourceUsername") as String?)
			?: System.getenv("SPRING_DATASOURCE_USERNAME") ?: "rhododendra"
	val springDatasourcePassword =
		(project.findProperty("springDatasourcePassword") as String?)
			?: System.getenv("SPRING_DATASOURCE_PASSWORD") ?: "rhododendra"

	systemProperty("data.jsonDir", dataJsonDir)
	systemProperty("spring.datasource.url", springDatasourceUrl)
	systemProperty("spring.datasource.username", springDatasourceUsername)
	systemProperty("spring.datasource.password", springDatasourcePassword)
	systemProperty("domain", domain)
}

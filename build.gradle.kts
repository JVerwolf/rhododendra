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
	implementation("org.xerial:sqlite-jdbc:3.46.0.0")


	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<Test> {
	useJUnitPlatform()
	// Absolute paths so IDE runs still isolate SQLite + Lucene under build/ (matches test application.properties intent).
	// @TestPropertySource on a test class overrides these defaults when it sets db.path.
	systemProperty(
		"db.path",
		layout.buildDirectory.file("test-rhododendra.sqlite").get().asFile.absolutePath
	)
	systemProperty(
		"lucene.index.base",
		layout.buildDirectory.dir("test-lucene").get().asFile.absolutePath
	)
}

tasks.register<JavaExec>("migrateAndIndex") {
	group = "application"
	description =
		"Migrates JSON from the hirsutum scraper output directory into SQLite, then rebuilds Lucene indexes under ./index"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.rhododendra.cli.MigrateAndIndexApplication")
	workingDir = project.layout.projectDirectory.asFile

	val dataJsonDir =
		(project.findProperty("dataJsonDir") as String?)
			?: "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/"
	val dbPath =
		(project.findProperty("dbPath") as String?)
			?: "${project.layout.projectDirectory.asFile}/data/rhododendra.sqlite"
	val domain =
		(project.findProperty("domain") as String?)
			?: "https://rhododendra.com"

	systemProperty("data.jsonDir", dataJsonDir)
	systemProperty("db.path", dbPath)
	systemProperty("domain", domain)
}

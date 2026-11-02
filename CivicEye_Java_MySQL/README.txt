# CivicEye - Java Console + MySQL Mini Project

## Requirements
- JDK 17 or newer recommended
- MySQL Server
- MySQL Connector/J JAR
- VS Code (or any Java IDE)

## 1. Create the database
Open MySQL Workbench and run the entire `schema.sql` file.

## 2. Configure the database password
Open `DBConnection.java` and replace `YOUR_MYSQL_PASSWORD` with your MySQL root password.
If your MySQL username is not `root`, change `USER` too.

## 3. Add MySQL Connector/J
Download the MySQL Connector/J JAR from the official MySQL website.
In VS Code, easiest option:
- Install the "Extension Pack for Java".
- Create/open this project folder.
- Create a `lib` folder and put the connector JAR inside it.
- In VS Code settings, configure Java project referenced libraries to include `lib/**/*.jar`, or use the classpath command below.

## 4. Compile and run (Windows Command Prompt / terminal)
Replace `mysql-connector-j-<version>.jar` with your actual JAR filename.

Windows:
    javac -cp ".;lib\mysql-connector-j-<version>.jar" DBConnection.java Main.java
    java -cp ".;lib\mysql-connector-j-<version>.jar" Main

macOS/Linux:
    javac -cp ".:lib/mysql-connector-j-<version>.jar" DBConnection.java Main.java
    java -cp ".:lib/mysql-connector-j-<version>.jar" Main

## Demo admin
Username: admin
Password: Admin@123

This admin credential is hard-coded for a classroom prototype. Do not use this design in a real public system.

## Photo evidence
The console version stores the optional photo path in MySQL; it does not upload a file to a server or display an image in the console. Keep the image on the same computer and enter its full path.

## Important
This is a college prototype. It does not automatically submit reports to a government department.

@echo off
chcp 65001 >nul
mvn test
if errorlevel 1 exit /b 1
mvn package
if errorlevel 1 exit /b 1
java -jar target\cinemalog-tdd-1.0.0.jar

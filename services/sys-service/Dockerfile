# ----- Giai đoạn 1: Build ứng dụng với Maven và JDK 21 -----
# Sử dụng một image chuẩn có chứa Maven (phiên bản 3.x) và JDK 21
FROM maven:3-eclipse-temurin-21 AS build

WORKDIR /app

# Copy file pom.xml để tải các dependency
COPY pom.xml .
RUN mvn dependency:go-offline

# Thiết lập môi trường UTF-8 để tránh lỗi encoding
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# Đảm bảo Maven build theo UTF-8
ENV MAVEN_OPTS="-Dfile.encoding=UTF-8"
# Copy toàn bộ source code và build
COPY src ./src
RUN mvn clean package -DskipTests

# ----- Giai đoạn 2: Tạo image để chạy với JRE 21 -----
# Sử dụng một image Java 21 JRE mỏng nhẹ (slim)
FROM openjdk:21-slim

WORKDIR /app

# Copy file .jar đã được build từ giai đoạn 1
COPY --from=build /app/target/*.jar app.jar

# Lệnh để chạy ứng dụng của bạn khi container khởi động
ENTRYPOINT ["java", "-jar", "app.jar"]
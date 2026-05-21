# Sử dụng ảnh chính thức OpenJDK làm ảnh cha
FROM eclipse-temurin:17-jdk

# Đặt thư mục làm việc bên trong container
WORKDIR /app

# Sao chép Maven wrapper và pom.xml
COPY mvnw* pom.xml ./
COPY .mvn .mvn

# Cấp quyền thực thi cho mvnw (sửa lỗi permission denied)
RUN chmod +x mvnw

# Sao chép mã nguồn
COPY src ./src
COPY src/main/resources ./src/main/resources

# Đóng gói ứng dụng (bỏ qua kiểm thử để build nhanh hơn, có thể bỏ nếu muốn chạy kiểm thử)
RUN ./mvnw clean package -DskipTests

# Sao chép file jar đã build vào container
RUN cp target/*.jar app.jar

# Mở cổng (thay đổi nếu ứng dụng dùng cổng khác)
EXPOSE 8181

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
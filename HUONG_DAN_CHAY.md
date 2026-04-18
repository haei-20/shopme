# Huong dan chay du an GearShop

Tai lieu nay huong dan chay du an tren Windows voi MySQL.

## 1. Yeu cau moi truong

- JDK 17 (khuyen nghi)
- MySQL Server 8.x
- VS Code hoac Command Prompt/PowerShell
- Khong can cai Maven rieng (du an da co `mvnw.cmd`)

## 2. Cau hinh database

1. Mo MySQL client (MySQL Workbench hoac command line).
2. Chay file tao schema:
   - `sanpham.sql`
3. Chay file du lieu mau:
   - `insertsp.sql`

Thu tu bat buoc: `sanpham.sql` -> `insertsp.sql`.

## 3. Cau hinh ket noi CSDL

Mo file `src/main/resources/application.properties` va kiem tra:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

Vi du hien tai:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/gearshop?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=111111
```

Neu mat khau MySQL cua ban khac, hay sua lai gia tri `spring.datasource.password`.

## 3.1 Cau hinh SMTP an toan (khong hardcode mat khau)

Du an da duoc chinh de doc thong tin SMTP tu bien moi truong:

- `SMTP_HOST` (mac dinh: `smtp.gmail.com`)
- `SMTP_PORT` (mac dinh: `587`)
- `SMTP_USERNAME`
- `SMTP_PASSWORD`

### Cmd (tam thoi cho phien hien tai)

```bat
set SMTP_HOST=smtp.gmail.com
set SMTP_PORT=587
set SMTP_USERNAME=your_email@gmail.com
set SMTP_PASSWORD=your_app_password
```

### PowerShell (tam thoi cho phien hien tai)

```powershell
$env:SMTP_HOST="smtp.gmail.com"
$env:SMTP_PORT="587"
$env:SMTP_USERNAME="your_email@gmail.com"
$env:SMTP_PASSWORD="your_app_password"
```

Luu y: khong commit tai khoan hoac app password vao git.

## 3.2 File local khong commit (profile local)

Da tao san file `src/main/resources/application-local.properties` de ban luu SMTP local:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

File nay da duoc them vao `.gitignore`, nen se khong bi commit.

## 3.3 Mau file application.properties (copy dung truc tiep)

Ban co the copy mau ben duoi vao `src/main/resources/application.properties`:

```properties
spring.application.name=gearshop
spring.datasource.url=jdbc:mysql://localhost:3306/gearshop?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_mysql_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hibernate config (JPA)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl

# SMTP (configure with your real mail account/app password)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Forgot password OTP expiration (milliseconds)
app.forgot-password.otp-expiration-ms=300000
```

## 4. Chay ung dung

Mo terminal tai thu muc goc `Source Code`, sau do chay:
cd /d D:\my_git\BTL-TMDT\Source Code

```powershell
.\mvnw.cmd spring-boot:run
```

Neu muon nap `application-local.properties`, chay voi profile local:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Khi thay dong log ung dung da start thanh cong, truy cap:

- `http://localhost:8080`

## 5. Build file jar (tuy chon)

```powershell
.\mvnw.cmd clean package
java -jar target\gearshop-0.0.1-SNAPSHOT.jar
```

## 6. Luu y ve JDK

Du an khai bao Java 17 trong `pom.xml`.

Neu may dang dung JDK 25, du an van co the chay nhung co kha nang gap loi tuong thich. Khuyen nghi dat `JAVA_HOME` tro den JDK 17 truoc khi chay.

## 7. Loi thuong gap

### Loi Data too long for column 'modelMain'

Neu gap loi nay khi import `insertsp.sql`, chay lenh sau trong MySQL:

```sql
ALTER TABLE sanphammainboard MODIFY COLUMN modelMain NVARCHAR(100);
```

Sau do chay lai doan insert cua bang `sanphammainboard`.

### Loi khong ket noi duoc MySQL

- Kiem tra MySQL service da chay chua
- Kiem tra lai user/password trong `application.properties`
- Kiem tra port 3306 co dang duoc dung boi MySQL khong

### Loi port 8080 da duoc su dung

Chay ung dung voi port khac:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## 8. Dang nhap nhanh (du lieu mau)

Ban co the dung tai khoan da co trong `insertsp.sql` de test nhanh.

Vi du:

- Username: `dangch2003`
- Password: `admin`

Neu khong dang nhap duoc, kiem tra lai du lieu da import day du chua.

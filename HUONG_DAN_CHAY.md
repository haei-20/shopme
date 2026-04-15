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

## 4. Chay ung dung

Mo terminal tai thu muc goc `Source Code`, sau do chay:
cd /d D:\my_git\BTL-TMDT\Source Code

```powershell
.\mvnw.cmd spring-boot:run
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

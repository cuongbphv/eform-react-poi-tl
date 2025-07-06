# Hướng dẫn cài đặt và sử dụng Hệ thống E-Form

## Tổng quan

Hệ thống E-Form cho phép:
- Upload template Word (.docx) với các biến dynamic
- Tự động phát hiện và cấu hình các biến trong template
- Tạo form điền thông tin dựa trên template
- Load dữ liệu từ API backend
- Sử dụng POI-TL để xử lý template Word
- Xuất PDF từ form đã điền

## Cài đặt Backend (Spring Boot)

### 1. Tạo project Spring Boot

```bash
# Tạo thư mục project
mkdir eform-backend
cd eform-backend

# Copy các file đã tạo vào thư mục src/main/java/com/example/eform/
```

### 2. Cấu trúc thư mục

```
eform-backend/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── eform/
│       │               ├── EFormApplication.java
│       │               ├── controller/
│       │               │   └── EFormController.java
│       │               ├── dto/
│       │               │   └── (các DTO classes)
│       │               ├── model/
│       │               │   └── (các Entity classes)
│       │               ├── repository/
│       │               │   └── (các Repository interfaces)
│       │               └── service/
│       │                   └── EFormService.java
│       └── resources/
│           └── application.properties
├── uploads/          # Thư mục lưu template
├── outputs/          # Thư mục tạm cho PDF
└── pom.xml
```

### 3. Chạy backend

```bash
# Cài đặt dependencies
mvn clean install

# Chạy ứng dụng
mvn spring-boot:run
```

Backend sẽ chạy trên `http://localhost:8080`

## Cài đặt Frontend (React)

### 1. Tạo project React

```bash
# Tạo project React
npx create-react-app eform-frontend
cd eform-frontend

# Cài đặt dependencies
npm install lucide-react axios
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

### 2. Cấu hình Tailwind CSS

Copy nội dung từ file `tailwind.config.js` đã tạo

### 3. Thay thế file App.js

Copy nội dung từ React component đã tạo vào `src/App.js`

### 4. Cấu hình CSS

Copy nội dung CSS vào `src/index.css`

### 5. Chạy frontend

```bash
npm start
```

Frontend sẽ chạy trên `http://localhost:3000`

## Cách sử dụng

### 1. Tạo Template Word

Tạo file Word (.docx) với các biến theo format:
- `{{tenBien}}` cho text đơn giản
- `{{nguoiNhan}}` cho tên người nhận
- `{{ngayThang}}` cho ngày tháng
- `{{diaChi}}` cho địa chỉ

Ví dụ template:
```
CÔNG TY {{tenCongTy}}
Địa chỉ: {{diaChi}}
Điện thoại: {{dienThoai}}

Kính gửi: {{nguoiNhan}}
Ngày: {{ngayThang}}

Nội dung: {{noiDung}}

Trân trọng,
{{nguoiKy}}
```

### 2. Upload Template

1. Mở ứng dụng web
2. Chọn tab "Templates"
3. Kéo thả hoặc click để chọn file Word
4. Nhập tên template
5. Hệ thống sẽ tự động phát hiện các biến

### 3. Tạo Form

1. Trong danh sách templates, click "Tạo Form"
2. Nhập tên form
3. Điền thông tin cho các biến
4. Click "Lưu Form"

### 4. Xuất PDF

1. Chọn tab "Forms"
2. Tìm form cần xuất
3. Click "PDF" để tải xuống file PDF

## API Endpoints

### Templates
- `POST /api/v1/templates/upload` - Upload template
- `GET /api/v1/templates` - Lấy danh sách templates
- `GET /api/v1/templates/{id}` - Lấy chi tiết template

### Forms
- `POST /api/v1/forms` - Lưu form
- `GET /api/v1/forms` - Lấy danh sách forms
- `GET /api/v1/forms/{id}` - Lấy chi tiết form
- `POST /api/v1/forms/generate-pdf` - Tạo PDF từ dữ liệu
- `POST /api/v1/forms/{id}/generate-pdf` - Tạo PDF từ form đã lưu


## Database Schema

### Templates Table
```sql
CREATE TABLE templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    variables TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Forms Table
```sql
CREATE TABLE forms (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    form_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES templates(id)
);
```

## Tính năng nâng cao

### 1. Template với Table động

Tạo template với bảng có thể lặp:
```
{{#employees}}
STT | Họ tên | Chức vụ | Lương
{{index}} | {{name}} | {{position}} | {{salary}}
{{/employees}}
```

Xử lý trong Spring Boot:
```java
public class EmployeeData {
    private List<Employee> employees;
    
    public static class Employee {
        private String name;
        private String position;
        private String salary;
        // getters/setters
    }
}
```

### 2. Conditional rendering

Template với điều kiện:
```
{{#isManager}}
Phần dành cho quản lý: {{managerContent}}
{{/isManager}}

{{^isManager}}
Phần dành cho nhân viên: {{employeeContent}}
{{/isManager}}
```

### 3. Image handling

Thêm hình ảnh vào template:
```java
@Service
public class ImageService {
    public PictureRenderData createPicture(String imagePath) {
        return Pictures.ofLocal(imagePath)
                .size(100, 100)
                .create();
    }
}
```

### 4. Advanced form validation

```javascript
const validateForm = (formData, template) => {
    const errors = {};
    
    template.variables.forEach(variable => {
        if (variable.required && !formData[variable.name]) {
            errors[variable.name] = 'Trường này là bắt buộc';
        }
        
        if (variable.type === 'email' && formData[variable.name]) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(formData[variable.name])) {
                errors[variable.name] = 'Email không hợp lệ';
            }
        }
    });
    
    return errors;
};
```
# E-Commerce Platform - Business Logic & API Specifications

Tài liệu này mô tả chi tiết toàn bộ nghiệp vụ cốt lõi của hệ thống thương mại điện tử (E-commerce) và danh sách các API mà bạn cần implement (hoặc hoàn thiện) trong từng Microservice.

---

## 1. Tổng quan Nghiệp vụ (Business Flows)

Hệ thống được thiết kế theo kiến trúc Microservices, bao gồm 4 luồng nghiệp vụ chính:

### 1.1. Luồng Người dùng (User Flow - `user-service`)
- **Khách hàng mới** truy cập hệ thống và thực hiện Đăng ký tài khoản (Register) với email và mật khẩu.
- Hệ thống gửi email chào mừng (thông qua `notification-service`).
- **Khách hàng** Đăng nhập (Login) để lấy Access Token (sống ngắn hạn, 15 phút) và Refresh Token (sống dài hạn, 7 ngày, lưu ở HttpOnly Cookie).
- Dùng Access Token để xem và cập nhật thông tin cá nhân (Profile).
- Khi Access Token hết hạn, client tự động gọi API Refresh để lấy token mới mà không bắt user đăng nhập lại.

### 1.2. Luồng Sản phẩm (Product Flow - `product-service`)
- **Admin** tạo, sửa, xóa sản phẩm và danh mục sản phẩm. Các dữ liệu này được lưu xuống DB.
- **Khách hàng** lên trang chủ, gọi API lấy danh sách danh mục và danh sách sản phẩm.
- Khách hàng có thể tìm kiếm, lọc theo giá, phân trang.
- Để tăng tốc độ, danh sách sản phẩm và chi tiết sản phẩm được cache trên **Redis + Caffeine (L1/L2 Cache)**. Nếu Admin cập nhật thông tin sản phẩm, Cache sẽ bị xóa (Evicted) để đảm bảo tính nhất quán.

### 1.3. Luồng Đơn hàng (Order Flow - `order-service`)
- **Khách hàng** chọn các sản phẩm vào giỏ hàng (Giỏ hàng có thể quản lý ở FE hoặc một service riêng, ở đây ta gộp vào bước tạo đơn).
- Khách hàng bấm **Đặt hàng** (Checkout). `order-service` tiếp nhận request:
  1. Dùng Resilience4j (Circuit Breaker & Retry) gọi sang `product-service` để kiểm tra thông tin sản phẩm, giá cả và số lượng tồn kho (Inventory).
  2. Nếu hợp lệ, tính tổng tiền và lưu đơn hàng vào DB (`orders_db`) với trạng thái `PENDING`.
  3. Cập nhật (trừ) tồn kho bên `product-service` (gọi API trừ tồn kho).
  4. Lưu một event "Tạo đơn hàng thành công" vào bảng `outbox_events` (Transactional Outbox Pattern).
- Một worker/scheduler sẽ đọc bảng outbox và bắn event `order.created` vào **Kafka**.
- Đơn hàng chuyển sang trạng thái `CONFIRMED` hoặc `PROCESSING`.

### 1.4. Luồng Thông báo (Notification Flow - `notification-service`)
- Service này lắng nghe (Consume) các topic từ **Kafka** (`user.registered`, `order.created`).
- Khi có tin nhắn, nó sẽ gửi Email hoặc SMS cho khách hàng.
- Nếu gửi lỗi, nó sẽ tự động thử lại (Retry) và nếu thất bại hoàn toàn sẽ đẩy vào `Dead Letter Topic (DLT)` để xử lý thủ công.

---

## 2. Danh sách API Cần Implement

Dưới đây là các API chi tiết mà bạn cần code trong từng project. Dữ liệu trả về (Response) có thể được gói gọn trong class `ApiResponse<T>`.

### 2.1. API Gateway (`gateway`)
Gateway không chứa business logic, chỉ đóng vai trò làm proxy và filter.
- **JWT Authentication Filter**: Chặn các request yêu cầu xác thực, kiểm tra Access Token trên Header.
- **Rate Limiting**: Giới hạn số lượng request (Ví dụ: 100 req/s cho mỗi IP).

---

### 2.2. User Service (`user-service`)

> [!NOTE]
> Các API quản lý Auth đã có sẵn một phần ở README gốc, bạn cần hoàn thiện logic Token Rotation và bảo mật Cookie.

| Phương thức | Endpoint | Phân quyền | Mô tả |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Tạo user mới. Pass phải được mã hóa (BCrypt). Bắn Kafka event `user.registered`. |
| `POST` | `/api/v1/auth/login` | Public | Trả về AccessToken (Body) + RefreshToken (Cookie). |
| `POST` | `/api/v1/auth/refresh` | Public | Đọc RefreshToken từ Cookie, cấp lại AccessToken và RefreshToken mới (Rotation). |
| `POST` | `/api/v1/auth/logout` | Public | Xóa RefreshToken trong DB và clear Cookie ở trình duyệt. |
| `GET` | `/api/v1/users/me` | Authenticated | Lấy profile của chính mình. Phải sử dụng `@Cacheable`. |
| `PUT` | `/api/v1/users/me` | Authenticated | Cập nhật thông tin (Họ tên). Phải sử dụng `@CachePut`. |

---

### 2.3. Product Service (`product-service`)

> [!IMPORTANT]
> Toàn bộ API GET phải hỗ trợ Caching (Redis/Caffeine). API biến đổi dữ liệu (POST, PUT, DELETE) phải thực hiện xóa hoặc cập nhật lại Cache.

**Danh mục (Category)**

| Phương thức | Endpoint | Phân quyền | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/categories` | Public | Lấy cây danh mục (hỗ trợ nested/phân cấp). Cần Cache. |
| `POST` | `/api/v1/categories` | Admin | Tạo danh mục mới. |

**Sản phẩm (Product)**

| Phương thức | Endpoint | Phân quyền | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/products` | Public | Lấy danh sách sản phẩm. Param: `page, size, categoryId, search, minPrice, maxPrice`. Cần Cache. |
| `GET` | `/api/v1/products/{id}` | Public | Lấy chi tiết sản phẩm. Cần Cache. |
| `POST` | `/api/v1/products` | Admin | Thêm sản phẩm mới. Payload: `name, price, stock, categoryId, images`. |
| `PUT` | `/api/v1/products/{id}` | Admin | Sửa thông tin sản phẩm. Xóa cache của sản phẩm này. |
| `DELETE` | `/api/v1/products/{id}` | Admin | Xóa mềm (Soft delete) sản phẩm. |
| `POST` | `/api/v1/products/{id}/deduct-stock` | Internal (Order) | API nội bộ cho `order-service` gọi để trừ tồn kho. Kiểm tra nếu `stock < quantity` thì báo lỗi. |

---

### 2.4. Order Service (`order-service`)

> [!WARNING]
> Order Service là service phức tạp nhất vì nó giao tiếp đa service. Bắt buộc phải dùng `@CircuitBreaker` khi gọi sang Product Service.

| Phương thức | Endpoint | Phân quyền | Mô tả |
|---|---|---|---|
| `POST` | `/api/v1/orders` | Authenticated | **Tạo đơn hàng.**<br>Payload: List danh sách sản phẩm (productId, quantity).<br>- Gọi sang Product Service lấy giá hiện tại, kiểm tra số lượng.<br>- Lưu DB.<br>- Gọi sang Product trừ tồn kho.<br>- Lưu event vào Outbox. |
| `GET` | `/api/v1/orders` | Authenticated | Danh sách lịch sử đơn hàng của user đang đăng nhập (Pagination). |
| `GET` | `/api/v1/orders/{id}` | Authenticated | Xem chi tiết đơn hàng (các mặt hàng, tổng tiền, trạng thái). |
| `PATCH` | `/api/v1/orders/{id}/cancel` | Authenticated | Hủy đơn hàng (Chỉ hủy được khi trạng thái là PENDING hoặc CONFIRMED). Phải có logic cộng lại tồn kho bên Product. |

---

### 2.5. Notification Service (`notification-service`)
Service này phần lớn không có HTTP API (hoặc chỉ có API Health Check) mà hoạt động dựa trên Event-Driven.

**Kafka Consumers cần implement:**
1. **Lắng nghe `user.registered`**:
   - Nhận payload: `{ "userId": "...", "email": "..." }`.
   - Gửi Email: "Chào mừng bạn gia nhập nền tảng E-commerce của chúng tôi!".
2. **Lắng nghe `order.created`**:
   - Nhận payload: `{ "orderId": "...", "totalAmount": 1500000, "userEmail": "..." }`.
   - Gửi Email: "Xác nhận đơn hàng #... thành công".

---

## 3. Các Checklist Kỹ Thuật Khi Implement

1. **Transaction Management (`@Transactional`)**: Bắt buộc phải có ở những hàm ghi dữ liệu phức tạp (như tạo đơn hàng).
2. **Exception Handling**: Mọi lỗi cần bắn ra một `BaseException` (hoặc custom exception) và được bắt lại bởi `@ControllerAdvice` để trả về JSON chuẩn hóa (có field `code`, `message`, `timestamp`).
3. **Resilience4j**: Ở hàm tạo đơn hàng trong Order Service, nếu gọi sang Product Service bị lỗi Timeout hoặc 500, cần cấu hình Retry 3 lần, nếu vẫn thất bại -> Kích hoạt Fallback method (Hủy luồng, trả lỗi "Hệ thống đang bận, vui lòng thử lại sau" thay vì Exception trắng).
4. **Outbox Pattern**:
   - Để tránh việc lưu DB thành công nhưng push lên Kafka thất bại, hãy tạo table `outbox_events`. Khi lưu Order thành công, lưu chung 1 record vào `outbox_events` (chung 1 transaction). Có 1 cron-job (dùng `@Scheduled`) cứ 5 giây quét bảng outbox một lần để bắn lên Kafka.


# E-Commerce Feature Roadmap (Những tính năng cần làm tiếp)

Đây là tài liệu lộ trình (Roadmap) tổng hợp các tính năng (features) từ cơ bản đến nâng cao cho một hệ thống E-commerce thực tế. Bạn hãy dựa vào danh sách này để tiếp tục code và nâng cấp dự án của mình.

---

## 1. Mở rộng Product Service (Quản lý Sản phẩm & Tồn kho)

### 1.1. Hệ thống Đánh giá & Bình luận (Review & Rating)
- **API cần làm**:
  - `POST /api/v1/products/{id}/reviews`: User gửi đánh giá (1-5 sao) kèm nội dung.
  - `GET /api/v1/products/{id}/reviews`: Lấy danh sách đánh giá của sản phẩm (có phân trang).
- **Nghiệp vụ**: 
  - Chỉ cho phép user đã mua hàng (Order status = DELIVERED) mới được đánh giá.
  - Sau khi thêm review, cần update lại điểm rating trung bình (Average Rating) của Product.

### 1.2. Quản lý Thuộc tính Sản phẩm (Product Variants)
- **Nghiệp vụ**: Một sản phẩm (ví dụ: Áo thun) có nhiều biến thể (Màu sắc: Xanh, Đỏ; Size: S, M, L). Mỗi biến thể có một giá và số lượng tồn kho (stock) riêng.
- **API cần làm**:
  - Update các CRUD API hiện tại để hỗ trợ mảng `variants`.

### 1.3. Khuyến mãi & Mã giảm giá (Discount & Coupon)
- **API cần làm**:
  - `POST /api/v1/coupons`: Tạo mã giảm giá (Admin).
  - `POST /api/v1/coupons/apply`: Kiểm tra tính hợp lệ của coupon (Dùng lúc User ở màn hình Checkout).
- **Nghiệp vụ**:
  - Có 2 loại: Giảm % (Percent) và Giảm tiền mặt (Fixed Amount).
  - Check điều kiện: Giá trị đơn hàng tối thiểu, Ngày hết hạn, Số lượng mã tối đa.

---

## 2. Mở rộng Order Service (Giỏ hàng & Thanh toán)

### 2.1. Quản lý Giỏ hàng (Cart)
- **Nghiệp vụ**: Khách hàng chọn sản phẩm vào giỏ hàng trước khi checkout. Giỏ hàng nên được lưu bằng **Redis** (dạng Hash hoặc List) để tốc độ siêu nhanh và tự động hết hạn, không cần lưu vào Database cứng.
- **API cần làm**:
  - `POST /api/v1/cart/items`: Thêm sản phẩm vào giỏ.
  - `GET /api/v1/cart`: Lấy thông tin giỏ hàng hiện tại.
  - `DELETE /api/v1/cart/items/{productId}`: Xóa sản phẩm khỏi giỏ.

### 2.2. Tích hợp Thanh toán (Payment Integration)
- **Nghiệp vụ**: Hỗ trợ thanh toán qua cổng điện tử (VNPay, Momo, Stripe).
- **API cần làm**:
  - `POST /api/v1/orders/{id}/pay`: Tạo Payment URL (trả về link để user click vào thanh toán).
  - `GET /api/v1/payments/callback`: Nhận Webhook/Callback từ cổng thanh toán báo về (thành công/thất bại).
  - Nếu thành công -> Đổi status đơn hàng sang `PROCESSING`.
  - Nếu thất bại -> Đổi sang `PAYMENT_FAILED` và rollback tồn kho (trả lại stock cho Product Service qua Kafka hoặc gọi API bù trừ).

---

## 3. Thêm User Service & Authentication Nâng cao

### 3.1. Đăng nhập qua Mạng Xã Hội (Social Login - OAuth2)
- **Nghiệp vụ**: Đăng nhập bằng Google / Facebook.
- **API cần làm**:
  - `GET /api/v1/auth/google/url`: Lấy URL redirect sang Google.
  - `GET /api/v1/auth/google/callback`: Nhận Auth Code, gọi lên Google lấy thông tin user, sau đó tạo User trong DB (nếu chưa có) và trả về AccessToken/RefreshToken.

### 3.2. Quản lý Địa chỉ Giao hàng (User Addresses)
- **API cần làm**:
  - `POST /api/v1/users/me/addresses`: Thêm địa chỉ mới.
  - `GET /api/v1/users/me/addresses`: Lấy danh sách địa chỉ.
  - `PUT /api/v1/users/me/addresses/{id}/default`: Đặt làm địa chỉ mặc định.

---

## 4. Các Service Mới Có Thể Xây Dựng (Advanced)

### 4.1. Inventory Service (Tách riêng từ Product)
- Thay vì Product Service giữ biến `stockQuantity`, hãy tách hẳn ra một Microservice chuyên quản lý Kho hàng.
- Áp dụng pattern **Saga** (Choreography hoặc Orchestration) để xử lý Distributed Transaction giữa Order Service và Inventory Service.

### 4.2. Search Service (ElasticSearch)
- Khi dữ liệu hàng triệu sản phẩm, tìm kiếm qua `%LIKE%` PostgreSQL sẽ rất chậm.
- Tạo `search-service` dùng Elasticsearch.
- Lắng nghe Kafka event `product.created`, `product.updated` để đồng bộ dữ liệu từ PostgreSQL sang Elasticsearch.

### 4.3. Recommendation Service (Gợi ý Sản phẩm)
- Đề xuất sản phẩm "Có thể bạn cũng thích" dựa trên lịch sử xem hàng hoặc lịch sử mua của User.
- Dùng Neo4j (Graph DB) hoặc các thuật toán phân tích hành vi đơn giản trên SQL/Redis.

---

## 5. Cải thiện System & DevOps (Non-Functional)

1. **API Gateway Security**: Thêm cấu hình CORS, Rate Limiting (Bucket4j) chống DDoS.
2. **Monitoring & Tracing**: 
   - Tích hợp Prometheus & Grafana để vẽ biểu đồ theo dõi sức khỏe các API.
   - Thêm Spring Cloud Sleuth/Micrometer Tracing (Zipkin/Jaeger) để có `traceId` chạy xuyên suốt từ Gateway -> Order -> Product -> Notification (dễ debug).
3. **Caching Strategy**: Bổ sung cơ chế Cache Stampede protection (chống nghẽn lúc cache hết hạn hàng loạt).
4. **CI/CD**: Viết GitHub Actions pipeline tự động chạy Unit Test, build Docker Image khi có code mới push lên.

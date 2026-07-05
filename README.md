<div align="center">

# 🍕 PizzaTime

### Ứng dụng đặt pizza & quản lý giao hàng đa vai trò trên Android

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Cloud%20Firestore](https://img.shields.io/badge/Cloud%20Firestore-FF6F00?style=for-the-badge&logo=firebase&logoColor=white)
![XML UI](https://img.shields.io/badge/XML%20UI-005FAD?style=for-the-badge&logo=androidstudio&logoColor=white)
![OpenStreetMap](https://img.shields.io/badge/OpenStreetMap-7EBC6F?style=for-the-badge&logo=openstreetmap&logoColor=white)

**PizzaTime** mô phỏng đầy đủ một hệ thống đặt pizza: khách hàng đặt món, nhân viên xác nhận đơn, bếp chế biến, shipper giao hàng và admin quản lý toàn bộ cửa hàng.

</div>

---

## 📌 Tổng quan dự án

**PizzaTime** là ứng dụng Android native được xây dựng bằng **Kotlin + XML + Firebase**. Ứng dụng hướng đến mô hình cửa hàng pizza có nhiều vai trò vận hành, từ khách hàng đến nhân viên nội bộ.

Ứng dụng không chỉ dừng ở màn đặt món cơ bản, mà có đầy đủ quy trình:

```text
Khách đặt pizza
→ Staff xác nhận đơn
→ Kitchen tiếp nhận và chế biến
→ Shipper giao hàng, thu tiền mặt
→ Đơn hoàn tất
→ Admin theo dõi và quản lý cửa hàng
```

Mục tiêu của project là tạo một sản phẩm demo Android có tính thực tế, có phân quyền rõ ràng, dữ liệu realtime và workflow gần giống một app đặt đồ ăn thật.

---

## 🎯 PizzaTime dùng để làm gì?

PizzaTime dùng để:

- Cho khách hàng xem menu pizza, chọn món, tùy chỉnh size/đế/topping và đặt hàng.
- Cho nhân viên tiếp nhận và xác nhận đơn hàng mới.
- Cho bếp theo dõi các đơn cần chế biến và cập nhật trạng thái món.
- Cho shipper nhận đơn giao, xem địa chỉ lấy/giao hàng, thu tiền và hoàn tất đơn.
- Cho admin quản lý cửa hàng, sản phẩm, nhân sự, mã giảm giá, phí giao hàng và báo cáo.

---

## 👥 Vai trò người dùng

| Vai trò | Mô tả | Chức năng chính |
|---|---|---|
| **Customer** | Khách hàng đặt pizza | Xem menu, thêm giỏ hàng, checkout, theo dõi đơn, quản lý địa chỉ, yêu thích |
| **Staff** | Nhân viên tiếp tân/xác nhận đơn | Xem đơn mới, xác nhận đơn, chuyển đơn sang bếp |
| **Kitchen** | Nhân viên bếp | Tiếp nhận đơn đã xác nhận, chế biến, đánh dấu hoàn thành món |
| **Shipper** | Nhân viên giao hàng | Nhận đơn từ bếp, giao hàng, thu tiền, hoàn tất đơn |
| **Admin** | Quản lý cửa hàng | Quản lý menu, nhân sự, mã giảm giá, cài đặt cửa hàng, báo cáo |

---

## ✨ Chức năng nổi bật

### 🛒 Customer Flow

- Đăng nhập/đăng ký bằng Firebase Authentication.
- Xem danh sách pizza và sản phẩm bán kèm.
- Tìm kiếm sản phẩm.
- Xem chi tiết pizza.
- Tùy chỉnh pizza theo:
  - Size
  - Crust/đế bánh
  - Topping
- Thêm sản phẩm vào giỏ hàng.
- Áp dụng mã giảm giá.
- Chọn địa chỉ giao hàng.
- Chọn vị trí giao hàng trên bản đồ.
- Tính khoảng cách giao hàng.
- Tính phí ship theo cấu hình cửa hàng.
- Tạo đơn hàng với mã đơn dễ đọc dạng `#ab-1234`.
- Theo dõi trạng thái đơn realtime.
- Xem lịch sử đơn hàng.
- Quản lý sản phẩm yêu thích.
- Xem thông tin tài khoản.

### 🧾 Staff Flow

- Xem danh sách đơn đang chờ xử lý.
- Mở chi tiết đơn hàng.
- Xác nhận đơn hợp lệ.
- Chuyển đơn sang bếp.
- Theo dõi trạng thái đơn sau khi xử lý.

### 👨‍🍳 Kitchen Flow

- Xem board đơn hàng cần chế biến.
- Xem chi tiết món trong từng đơn.
- Cập nhật trạng thái đang nấu.
- Đánh dấu đơn đã hoàn thành chế biến.
- Chuyển đơn sang trạng thái sẵn sàng giao hàng.

### 🛵 Shipper Flow

- Xem danh sách đơn cần giao.
- Xem địa chỉ cửa hàng và địa chỉ khách hàng.
- Mở vị trí lấy hàng/giao hàng bằng app bản đồ ngoài.
- Bắt đầu giao hàng.
- Xác nhận đã giao và đã thu tiền mặt.
- Cập nhật đơn thành `DELIVERED`.
- Lưu trạng thái thanh toán:
  - `paymentMethod = CASH_ON_DELIVERY`
  - `paymentStatus = PAID`
  - `cashCollected = true`

### 🛠️ Admin Flow

- Dashboard quản trị tổng quan.
- Quản lý menu sản phẩm:
  - Thêm sản phẩm
  - Sửa sản phẩm
  - Xóa sản phẩm
  - Bật/tắt trạng thái bán
  - Cập nhật ảnh sản phẩm
  - Cấu hình size, đế bánh, topping
- Upload ảnh sản phẩm qua Cloudinary.
- Quản lý mã giảm giá.
- Quản lý nhân sự.
- Xem báo cáo đơn hàng/doanh thu.
- Cấu hình cửa hàng:
  - Tên cửa hàng
  - Địa chỉ lấy hàng
  - Vị trí cửa hàng trên bản đồ
  - Số điện thoại cửa hàng
  - Giờ mở cửa
  - Trạng thái nhận đơn
  - Phí ship cơ bản
  - Phí ship theo km
  - Mốc miễn phí giao hàng
- Profile dùng chung theo role.
- Bottom navigation tự đổi theo role.

---

## 🧭 Các màn hình tiêu biểu

### Customer

| Màn hình | Mục đích |
|---|---|
| Splash Screen | Kiểm tra session và điều hướng theo role |
| Welcome Screen | Màn hình giới thiệu app |
| Login Screen | Đăng nhập tài khoản |
| Register Screen | Đăng ký tài khoản khách hàng |
| Customer Home | Trang chủ khách hàng |
| Pizza Menu/List | Danh sách sản phẩm |
| Pizza Detail | Chi tiết pizza và tùy chọn size/đế/topping |
| Cart | Giỏ hàng |
| Checkout | Thanh toán, địa chỉ, phí ship, mã giảm giá |
| Map Picker | Chọn vị trí giao hàng |
| Order Tracking | Theo dõi trạng thái đơn realtime |
| Order History | Lịch sử đơn hàng |
| Favorites | Sản phẩm yêu thích |
| Customer Account/Profile | Hồ sơ khách hàng |

### Staff / Kitchen / Shipper

| Màn hình | Mục đích |
|---|---|
| Staff Dashboard | Danh sách đơn cần xác nhận |
| Staff Order Detail | Chi tiết đơn cho staff xử lý |
| Kitchen Board | Board đơn đang chờ bếp chế biến |
| Kitchen Order Detail | Chi tiết đơn trong bếp |
| Shipper Dashboard | Danh sách đơn cần giao/đang giao |
| Shipper Delivery Detail | Chi tiết giao hàng, địa chỉ, thu tiền, hoàn tất đơn |
| Shared Profile | Hồ sơ dùng chung cho staff/kitchen/shipper |

### Admin

| Màn hình | Mục đích |
|---|---|
| Admin Dashboard | Tổng quan quản trị |
| Store Settings | Cấu hình thông tin cửa hàng, vị trí, phí ship |
| Manage Menu | Quản lý sản phẩm |
| Add/Edit Product | Thêm/sửa sản phẩm |
| Manage Promo Codes | Quản lý mã giảm giá |
| Manage Staff | Quản lý nhân sự |
| Reports | Báo cáo doanh thu/đơn hàng |
| Admin Profile | Thông tin tài khoản admin |

---

### Ảnh minh họa giao diện

<p align="center">
  <img src="docs/screenshots/customer-home.png" width="220" alt="Customer Home" />
  <img src="docs/screenshots/pizza-detail.png" width="220" alt="Pizza Detail" />
  <img src="docs/screenshots/checkout.png" width="220" alt="Checkout" />
</p>

<p align="center">
  <img src="docs/screenshots/admin-dashboard.png" width="220" alt="Admin Dashboard" />
  <img src="docs/screenshots/Reports-&-Analytics.png" width="220" alt="Reports & Analytics" />
  <img src="docs/screenshots/shipper-detail.png" width="220" alt="Shipper Detail" />
</p>

> Lưu ý: các ảnh phía trên sẽ chỉ hiện sau khi bạn thêm file ảnh thật vào thư mục `docs/screenshots/` và push lên GitHub.

---

## 🧱 Công nghệ sử dụng

| Nhóm | Công nghệ |
|---|---|
| Ngôn ngữ | Kotlin |
| UI | XML Layouts, ViewBinding |
| Navigation | Fragment-based navigation |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
| Realtime data | Firestore Snapshot Listener |
| Image loading | Glide |
| Image upload | Cloudinary unsigned upload |
| Map picker | osmdroid + OpenStreetMap |
| Location | Android LocationManager |
| Build tool | Gradle |
| Version control | Git + GitHub |

---

## 🔥 Firebase / Firestore Data Model

Các collection chính:

```text
users/{uid}
categories/{categoryId}
products/{productId}
orders/{orderCodeKey}
promoCodes/{promoCodeId}
appConfig/store
```

### users/{uid}

```text
fullName
email
phone
role
active
avatarUrl
deliveryAddress
deliveryLat
deliveryLng
favorites
doughPoints
createdAt
updatedAt
```

### products/{productId}

```text
name
description
basePrice
categoryId
imageUrl
available
sizeOptions
crustOptions
toppingOptions
createdAt
updatedAt
```

### orders/{orderCodeKey}

```text
orderId
orderCodeKey
orderCode
customerId
customerName
customerPhone
items
itemsSubtotal
discountAmount
deliveryFee
finalTotal
total
status
statusHistory
storeName
pickupAddress
pickupLat
pickupLng
deliveryAddress
deliveryLat
deliveryLng
distanceKm
paymentMethod
paymentStatus
cashCollected
createdAt
updatedAt
```

### appConfig/store

```text
storeName
pickupAddress
pickupLat
pickupLng
storePhone
openingHours
acceptingOrders
baseDeliveryFee
deliveryFeePerKm
freeDeliveryMinSubtotal
updatedAt
```

---

## 🧾 Trạng thái đơn hàng

PizzaTime sử dụng workflow trạng thái theo từng vai trò:

```text
PENDING
→ CONFIRMED
→ PREPARING
→ READY_FOR_DELIVERY
→ ASSIGNED_TO_SHIPPER
→ DELIVERING
→ DELIVERED
```

Ngoài ra có trạng thái hủy:

```text
CANCELLED
```

---

## 🔐 Demo Accounts

```text
customer@pizzatime.com / 123456
staff@pizzatime.com / 123456
kitchen@pizzatime.com / 123456
shipper@pizzatime.com / 123456
admin@pizzatime.com / 123456
```

---

## 👨‍💻 Author

**Phuong Nguyen**

GitHub: [@PhuongL1](https://github.com/PhuongL1)

---

<div align="center">

### 🍕 PizzaTime — From order to delivery, all in one Android app.

</div>

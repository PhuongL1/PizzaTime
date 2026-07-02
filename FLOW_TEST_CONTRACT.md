# PizzaTime — Basic App Flow Test Contract

_updated: 2026-07-02_

## 0. Mục tiêu tài liệu

Tài liệu này dùng để đưa cho `fw-coding agent` đọc trước khi test và sửa app. Mục tiêu là làm cho PizzaTime chạy chuẩn **basic navigation flow** trước, chưa cần backend/API thật.

Luồng khách hàng chính bắt buộc chạy được:

```text
Open app
→ Splash Screen
→ Welcome Screen
→ Unified Login Screen
→ Customer Home Screen
→ Pizza List Screen
→ Pizza Detail Screen
→ Cart Screen
→ Checkout Screen
→ Order Success Screen
→ Order Tracking Screen
```

Các phần có data thật, API thật, validate phức tạp, payment thật, Google/Facebook login, search/filter thật có thể để fake data hoặc `Toast coming soon`. Nhưng các nút chuyển màn cơ bản phải hoạt động, không crash, không kẹt back stack.

---

## 1. Nguyên tắc triển khai cho agent

### 1.1. Không redesign UI

Agent không được tự ý đổi layout lớn, không đổi theme, không đổi màu hardcode. Nếu cần sửa UI để gắn click hoặc tránh crash thì sửa tối thiểu.

### 1.2. Dùng navigator chung

Các hàm chuyển màn nên gom vào:

```text
app/src/main/java/com/devpro/pizzatime/feature/staff/navigation/StaffFlowNavigator.kt
```

Không lặp lại `parentFragmentManager.beginTransaction()` rải rác nếu đã có hàm navigator.

Các hàm navigation nên có hoặc cần kiểm tra:

```kotlin
openWelcomeScreen()
openLoginScreen()
openLoginRequiredScreen()
openRegisterScreen()
openForgotPassword()
openCustomerHome()
openPizzaMenuScreen()
openPizzaDetailScreen()
openCartScreen()
openCheckoutScreen()
openOrderSuccess()
openOrderTracking()
openCustomerOrderHistory()
openCustomerOrderDetail()
openCustomerPromoCodes()
openCustomerAccount()
openCustomerMemberQr()
openStaffDashboard()
openStaffOrderDetail()
openKitchenBoard()
openKitchenOrderDetail()
openShipperDeliveryDashboard()
openShipperDeliveryDetail()
openAdminDashboard()
openManageOrders()
openManageMenu()
openAddEditProduct()
openManagePromoCodes()
openManageStaff()
openReports()
```

Nếu hàm đã tồn tại với tên khác thì ưu tiên dùng tên hiện có, nhưng không để nhiều hàm trùng behavior gây rối.

### 1.3. Guest và Logged In

Hiện tại chưa cần auth thật. Có thể dùng fake session hoặc fake role.

Quy ước test:

```text
Guest = chưa login
Logged in = sau khi login fake thành công
```

Các màn cần đăng nhập mà Guest bấm vào thì mở:

```text
Login Required Modal / LoginRequiredFragment
```

### 1.4. Data có thể fake

Các phần sau chưa bắt buộc xử lý thật:

```text
Backend API
Token
Room database
Search thật
Filter thật
Payment thật
Google login
Facebook login
Forgot password API
Register API
Cart persistence thật
Order persistence thật
```

Nhưng fake data phải đủ để test flow.

---

## 2. Screen status theo spec hiện tại

### 2.1. Customer main flow

| # | Official Screen Name | Role | Status |
|---|---|---|---|
| 1 | Splash Screen | Shared | X |
| 2 | Welcome Screen | Guest / Customer | X |
| 3 | Unified Login Screen | Shared | X |
| 4 | Register Screen | Customer | X |
| 9 | Customer Home Screen | Customer | X |
| 11 | Pizza List Screen | Customer / Guest | X |
| 12 | Pizza Detail Screen | Customer / Guest | X |
| 14 | Cart Screen | Customer | X |
| 15 | Checkout Screen | Customer | X |
| 16 | Order Success Screen | Customer | X |
| 17 | Order Tracking Screen | Customer | X |

### 2.2. Staff flow

| # | Official Screen Name | Role | Status |
|---|---|---|---|
| 24 | Staff Dashboard Screen | Staff | X |
| 25 | Staff Order Detail Screen | Staff | X |

### 2.3. Kitchen flow

| # | Official Screen Name | Role | Status |
|---|---|---|---|
| 26 | Kitchen Board Screen | Kitchen | X |
| 27 | Kitchen Order Detail Screen | Kitchen | X |

### 2.4. Shipper flow

| # | Official Screen Name | Role | Status |
|---|---|---|---|
| 28 | Shipper Delivery Dashboard Screen | Shipper | X |
| 29 | Shipper Delivery Detail Screen | Shipper | X |

### 2.5. Admin flow

| # | Official Screen Name | Role | Status |
|---|---|---|---|
| 30 | Admin Dashboard Screen | Admin | X |
| 31 | Manage Orders Screen | Admin | X |
| 32 | Manage Menu Screen | Admin | X / PARTIAL |
| 33 | Add/Edit Product Screen | Admin | X |
| 34 | Manage Promo Codes Screen | Admin | X |
| 35 | Staff Management Screen | Admin | X |
| 36 | Reports Screen | Admin | X |

### 2.6. Shared / Common screens

| # | Official Screen Name | Role | Status |
|---|---|---|---|
| 5 | Forgot Password Screen | Shared | X |
| 6 | Notification Screen | Customer / Staff / Admin | X |
| 7 | Support / FAQ Screen | Customer / Staff | VERIFY |
| 8 | Login Required Modal | Guest | VERIFY |

### 2.7. Customer extended screens

| # | Official Screen Name | Role | Status |
|---|---|---|---|
| 10 | Order Type Screen | Customer | X |
| 13 | Build Your Pizza Screen | Customer | X |
| 18 | Customer Order Detail Screen | Customer | X |
| 19 | Order History Screen | Customer | X |
| 20 | Favorites Screen | Customer | X |
| 21 | Promo Codes Screen | Customer | X |
| 22 | Member QR Screen | Customer | X |
| 23 | Customer Account Screen | Customer | X |

### 2.8. Modal / Dialog

| # | Official Modal Name | Role | Status |
|---|---|---|---|
| 37 | Cancel Order Confirmation Modal | Customer / Staff / Admin | X |
| 38 | Assign Shipper Modal | Staff / Admin | X |
| 39 | Status Update Confirmation Modal | Staff / Kitchen / Shipper / Admin | X |

---

## 3. App start flow

### Expected flow

```text
Open app
→ Splash Screen
→ Welcome Screen
```

### Splash Screen

Required behavior:

| Action | Expected result |
|---|---|
| App launch | Splash hiển thị ngắn |
| Splash timeout | openWelcomeScreen() |

Checklist:

- [ ] Splash không crash.
- [ ] Splash không bị kẹt vô hạn.
- [ ] Splash chuyển sang Welcome.

### Welcome Screen

Required actions:

| UI Action | Expected result |
|---|---|
| btnStartOrdering | openCustomerHome() as Guest |
| btnLogin | openLoginScreen() |

Checklist:

- [ ] Start Ordering mở Customer Home.
- [ ] Login mở Unified Login Screen.
- [ ] Back stack không bị nhân nhiều fragment bất thường.

---

## 4. Auth flow

## 4.1. Unified Login Screen

### Input

User nhập:

```text
Email / Phone / Staff ID
Password
```

### Future real login response

```json
{
  "userId": "u001",
  "name": "Nguyen Van A",
  "email": "a@example.com",
  "phone": "0380000000",
  "role": "CUSTOMER",
  "token": "jwt_token"
}
```

### Required actions

| UI Action | Expected result |
|---|---|
| btnLogin | fake login, sau đó điều hướng theo role |
| btnCreateAccount | openRegisterScreen() |
| btnForgotPassword | openForgotPassword() |
| btnBack | popBackStack() |

### Fake login accounts

Agent nên kiểm tra hoặc tạo fake login theo bảng này:

| Account | Password | Expected screen |
|---|---|---|
| customer@pizzatime.com | 123456 | Customer Home Screen |
| staff@pizzatime.com | 123456 | Staff Dashboard Screen |
| kitchen@pizzatime.com | 123456 | Kitchen Board Screen |
| shipper@pizzatime.com | 123456 | Shipper Delivery Dashboard Screen |
| admin@pizzatime.com | 123456 | Admin Dashboard Screen |

### Role-based navigation

| Role | Expected result |
|---|---|
| CUSTOMER | openCustomerHome() |
| STAFF | openStaffDashboard() |
| KITCHEN | openKitchenBoard() |
| SHIPPER | openShipperDeliveryDashboard() |
| ADMIN | openAdminDashboard() |

Nếu role không hợp lệ:

```text
Show Toast: Your account role is not supported.
Stay on Login Screen or clear fake session.
```

---

## 4.2. Register Screen

### Required actions

| UI Action | Expected result |
|---|---|
| btnBack | openLoginScreen() hoặc popBackStack về Login |
| btnBackToLogin | openLoginScreen() hoặc popBackStack về Login |
| btnCreateAccount | validate cơ bản hoặc Toast fake success |
| btnGoogle | Toast coming soon |
| btnFacebook | Toast coming soon |

### Future register rules

Sau này khi có auth thật, Register cần xử lý:

```text
User nhập name, email, phone, password, confirm password.
Phải có ít nhất email hoặc phone.
Email/phone không được trùng account cũ.
Password và confirm password phải khớp.
Phải tick đồng ý điều khoản.
Role mặc định sau đăng ký = CUSTOMER.
```

Hiện tại chưa cần backend. Không được crash khi bấm Create Account.

---

## 4.3. Forgot Password Screen

### Required actions

| UI Action | Expected result |
|---|---|
| btnBack | openLoginScreen() hoặc popBackStack về Login |
| btnBackToLogin | openLoginScreen() hoặc popBackStack về Login |
| btnSendResetInstructions | Toast fake success / coming soon |

Required:

- [ ] Back về Login đúng.
- [ ] Send reset không crash.
- [ ] Không cần gửi email thật.

---

## 4.4. Login Required Modal / Screen

Dùng khi Guest bấm chức năng cần đăng nhập.

Required actions:

| UI Action | Expected result |
|---|---|
| btnLogin | openLoginScreen() |
| btnClose / Back | dismiss hoặc popBackStack |

Required:

- [ ] Guest checkout mở Login Required.
- [ ] Guest bấm Orders/Profile mở Login Required.
- [ ] Login Required không làm mất flow hiện tại.

---

## 5. Customer main flow

Main customer flow bắt buộc:

```text
Customer Home
→ Pizza List
→ Pizza Detail
→ Add to Cart
→ Cart
→ Checkout
→ Place Order
→ Order Success
→ Order Tracking
```

---

## 5.1. Customer Home Screen

### Required actions

| UI Action | Guest expected result | Logged-in expected result |
|---|---|---|
| btnHomeAvatar | Toast / drawer coming soon | open drawer / account drawer |
| searchBar | Toast coming soon | Toast coming soon |
| promoCard | Toast coming soon | Toast coming soon |
| tvSeeAll | openPizzaMenuScreen() | openPizzaMenuScreen() |
| bottomNav.navMenu | openCustomerHome() | openCustomerHome() |
| bottomNav.navOrders | openLoginRequiredScreen() | openCustomerOrderHistory() |
| bottomNav.navLoyalty | openCustomerPromoCodes() | openCustomerPromoCodes() |
| bottomNav.navProfile | openLoginRequiredScreen() | openCustomerAccount() |

Required checklist:

- [ ] Customer Home mở được từ Welcome với Guest.
- [ ] Customer Home mở được từ Login với CUSTOMER.
- [ ] `tvSeeAll` mở Pizza List.
- [ ] Bottom nav không crash.
- [ ] Guest bấm Orders/Profile bị chặn bằng Login Required.
- [ ] Logged-in bấm Orders/Profile mở đúng màn.

---

## 5.2. Pizza List Screen

### Required actions

| UI Action | Expected result |
|---|---|
| btnOpenDrawer | Toast coming soon |
| btnOpenCart | openCartScreen() |
| searchBar | Toast coming soon |
| btnFilter | Toast coming soon |
| pizza item click | openPizzaDetailScreen() |
| add button | add fake item to cart hoặc Toast added |
| bottomNav.navMenu | openCustomerHome() |
| bottomNav.navOrders | Guest: Login Required / Logged-in: Order History |
| bottomNav.navLoyalty | openCustomerPromoCodes() |
| bottomNav.navProfile | Guest: Login Required / Logged-in: Customer Account |

Required checklist:

- [ ] Pizza List mở được từ Customer Home.
- [ ] Pizza item mở Pizza Detail.
- [ ] Cart icon mở Cart.
- [ ] Search/filter chưa xử lý thì Toast.
- [ ] Không crash khi fake list rỗng hoặc có data.

---

## 5.3. Pizza Detail Screen

### Required actions

| UI Action | Expected result |
|---|---|
| btnBack | popBackStack() |
| btnAddToCart | add fake item hoặc Toast added |
| btnCart | openCartScreen() |
| quantity plus | quantity + 1 |
| quantity minus | quantity - 1, không nhỏ hơn 1 |
| size/crust/topping click | update selected UI hoặc Toast |

Required checklist:

- [ ] Mở được từ Pizza List.
- [ ] Add to Cart không crash.
- [ ] Cart mở Cart Screen.
- [ ] Quantity không âm, không về 0 nếu rule min = 1.

---

## 5.4. Cart Screen

### Required actions

| UI Action | Guest expected result | Logged-in expected result |
|---|---|---|
| btnCheckout | openLoginRequiredScreen() | openCheckoutScreen() |
| btnBack | popBackStack() | popBackStack() |
| plus/minus | update fake quantity | update fake quantity |
| remove | remove fake item | remove fake item |
| promo input/apply | Toast coming soon | Toast coming soon |

Required checklist:

- [ ] Cart mở được từ Pizza List/Pizza Detail.
- [ ] Cart có fake data hoặc empty state.
- [ ] Empty cart không crash.
- [ ] Guest checkout bị chặn bằng Login Required.
- [ ] Logged-in checkout mở Checkout.

---

## 5.5. Checkout Screen

### Required actions

| UI Action | Expected result |
|---|---|
| btnBack | popBackStack() |
| btnPlaceOrder | openOrderSuccess(fakeOrderId) |
| payment method click | update selected UI hoặc Toast |
| address edit | Toast coming soon |
| note input | cho nhập bình thường |

Required checklist:

- [ ] Checkout mở được từ Cart.
- [ ] Place Order mở Order Success.
- [ ] Fake order id dùng được, ví dụ `PT-9823`.
- [ ] Không cần payment thật.

---

## 5.6. Order Success Screen

### Required actions

| UI Action | Expected result |
|---|---|
| btnTrackOrder | openOrderTracking(fakeOrderId) |
| btnBackHome | openCustomerHome() |

Required checklist:

- [ ] Mở được sau Place Order.
- [ ] Track Order mở Order Tracking.
- [ ] Back Home về Customer Home.

---

## 5.7. Order Tracking Screen

### Required actions

| UI Action | Expected result |
|---|---|
| btnBack | popBackStack() |
| btnSupport | openSupportFaq() hoặc Toast coming soon |
| timeline | hiển thị fake status |

Required checklist:

- [ ] Mở được từ Order Success.
- [ ] Timeline fake hiển thị không crash.
- [ ] Support chưa xong thì Toast.

---

## 6. Customer extended screens

### Required basic navigation

| Screen | Required action |
|---|---|
| Order Type Screen | chọn Delivery / Self-Collect / Dine-In không crash |
| Build Your Pizza Screen | Add to Cart hoặc Toast |
| Customer Order Detail Screen | Track Order mở Order Tracking |
| Order History Screen | item click mở Customer Order Detail |
| Favorites Screen | item click mở Pizza Detail |
| Promo Codes Screen | back/nav không crash |
| Member QR Screen | back/nav không crash |
| Customer Account Screen | Logout mở Login hoặc Welcome |

Agent không cần hoàn thiện data thật cho các màn extended, chỉ cần navigation cơ bản và click action không crash.

---

## 7. Staff flow

### Main flow

```text
Unified Login Screen
→ Staff Dashboard Screen
→ Staff Order Detail Screen
→ Confirm / Assign / Update order
```

### Staff Dashboard Screen

Required actions:

| UI Action | Expected result |
|---|---|
| order item click | openStaffOrderDetail() |
| filter status | update fake list hoặc Toast |
| avatar/menu | drawer hoặc Toast, không crash |

### Staff Order Detail Screen

Required actions:

| UI Action | Expected result |
|---|---|
| btnConfirmOrder | fake status update hoặc Status Update Modal |
| btnSendToKitchen | fake status update hoặc Toast |
| btnAssignShipper | openAssignShipperModal() |
| btnCancelOrder | openCancelOrderConfirmationModal() |
| btnBack | popBackStack() |

### Staff status handling

```text
PENDING → CONFIRMED
CONFIRMED → PREPARING / Send to Kitchen
READY → ASSIGNED_TO_SHIPPER
```

Required checklist:

- [ ] Login staff mở Staff Dashboard.
- [ ] Order card mở Staff Order Detail.
- [ ] Confirm/Assign/Cancel không crash.
- [ ] Assign Shipper Modal mở được.
- [ ] Cancel Modal mở được.

---

## 8. Kitchen flow

### Main flow

```text
Unified Login Screen
→ Kitchen Board Screen
→ Kitchen Order Detail Screen
→ Update preparing / baking / ready
```

### Kitchen status

```text
PREPARING → BAKING → READY
```

### Kitchen Board Screen

Required actions:

| UI Action | Expected result |
|---|---|
| order item click | openKitchenOrderDetail() |
| status tabs/filter | update fake list hoặc Toast |

### Kitchen Order Detail Screen

Required actions:

| UI Action | Expected result |
|---|---|
| btnStartPreparing | fake update to PREPARING |
| btnStartBaking | fake update to BAKING |
| btnMarkReady | fake update to READY |
| btnCancelOrder | Cancel modal hoặc Toast |
| btnBack | popBackStack() |

Required checklist:

- [ ] Login kitchen mở Kitchen Board.
- [ ] Order item mở Kitchen Order Detail.
- [ ] Start Preparing/Baking/Ready không crash.

---

## 9. Shipper flow

### Main flow

```text
Unified Login Screen
→ Shipper Delivery Dashboard Screen
→ Shipper Delivery Detail Screen
→ Start Delivery
→ Complete Order
```

### Shipper status

```text
DELIVERING → DELIVERED
```

### Shipper Delivery Dashboard Screen

Required actions:

| UI Action | Expected result |
|---|---|
| delivery item click | openShipperDeliveryDetail() |
| filter assigned/delivering/completed | fake update hoặc Toast |

### Shipper Delivery Detail Screen

Required actions:

| UI Action | Expected result |
|---|---|
| btnStartDelivery | fake update to DELIVERING |
| btnCompleteOrder | fake update to DELIVERED |
| btnCallCustomer | Toast |
| btnBack | popBackStack() |

Required checklist:

- [ ] Login shipper mở Shipper Delivery Dashboard.
- [ ] Delivery item mở detail.
- [ ] Start Delivery/Complete Order không crash.

---

## 10. Admin flow

### Main flow

```text
Unified Login Screen
→ Admin Dashboard Screen
→ Manage Orders / Manage Menu / Add Product / Promo / Staff / Reports
```

### Admin Dashboard quick actions

| UI Action | Expected result |
|---|---|
| Add Product | openManageMenu() hoặc openAddEditProduct() |
| Manage Staff | openManageStaff() |
| View Reports | openReports() |
| Promotions | openManagePromoCodes() |
| Inventory | Toast coming soon |

### Other Admin screens

| Screen | Required action |
|---|---|
| Manage Orders Screen | item click mở detail/modal hoặc Toast |
| Manage Menu Screen | Add Product mở Add/Edit Product |
| Add/Edit Product Screen | Save fake success hoặc popBackStack |
| Manage Promo Codes Screen | Add/Edit fake hoặc Toast |
| Staff Management Screen | Add/Edit fake hoặc Toast |
| Reports Screen | hiển thị fake data, không crash |

Required checklist:

- [ ] Login admin mở Admin Dashboard.
- [ ] Quick actions được wire.
- [ ] Inventory không crash, Toast coming soon.
- [ ] Back từ các màn admin không bị kẹt.

---

## 11. Shared / Common screens

| Screen | Required behavior |
|---|---|
| Notification Screen | item click Toast hoặc open fake detail |
| Support / FAQ Screen | nếu đã có thì mở được, nếu chưa thì Toast coming soon |
| Login Required Modal | login button mở Login Screen, dismiss/back không crash |

---

## 12. Modal / Dialog behavior

| Modal | Required behavior |
|---|---|
| Cancel Order Confirmation Modal | mở được, cancel/dismiss được, confirm trả result hoặc Toast |
| Assign Shipper Modal | mở được, chọn shipper fake, confirm trả result hoặc Toast |
| Status Update Confirmation Modal | mở được, confirm update fake status hoặc Toast |

Required checklist:

- [ ] Không modal nào crash khi mở.
- [ ] Dismiss hoạt động.
- [ ] Confirm trả result hoặc Toast.
- [ ] Không cần backend thật.

---

## 13. Manual test script cho agent

### 13.1. Guest customer flow

```text
Open app
→ Splash
→ Welcome
→ Start Ordering
→ Customer Home as Guest
→ See All
→ Pizza List
→ Pizza item
→ Pizza Detail
→ Add to Cart
→ Cart
→ Checkout
→ Login Required
```

Expected:

```text
Không crash.
Guest bị chặn khi checkout.
Login Required mở được.
```

### 13.2. Logged-in customer flow

```text
Open app
→ Welcome
→ Login
→ customer@pizzatime.com / 123456
→ Customer Home
→ See All
→ Pizza List
→ Pizza Detail
→ Add to Cart
→ Cart
→ Checkout
→ Place Order
→ Order Success
→ Track Order
→ Order Tracking
```

Expected:

```text
Chạy hết flow.
Không crash.
Order id fake hiển thị được.
Timeline fake hiển thị được.
```

### 13.3. Register / Forgot Password

```text
Login
→ Create Account
→ Register
→ Back
→ Login
→ Forgot Password
→ Back
→ Login
```

Expected:

```text
Back đúng.
Không bị kẹt back stack.
Không crash.
```

### 13.4. Staff flow

```text
Login staff@pizzatime.com / 123456
→ Staff Dashboard
→ Click order
→ Staff Order Detail
→ Confirm Order
→ Assign Shipper
→ Cancel Order
→ Back
```

Expected:

```text
Mở được detail.
Mở được modal.
Fake action không crash.
```

### 13.5. Kitchen flow

```text
Login kitchen@pizzatime.com / 123456
→ Kitchen Board
→ Click order
→ Kitchen Order Detail
→ Start Preparing
→ Start Baking
→ Mark Ready
→ Back
```

Expected:

```text
Status fake đổi hoặc Toast.
Không crash.
```

### 13.6. Shipper flow

```text
Login shipper@pizzatime.com / 123456
→ Shipper Delivery Dashboard
→ Click delivery
→ Shipper Delivery Detail
→ Start Delivery
→ Complete Order
→ Back
```

Expected:

```text
Status fake đổi hoặc Toast.
Không crash.
```

### 13.7. Admin flow

```text
Login admin@pizzatime.com / 123456
→ Admin Dashboard
→ Add Product
→ Manage Menu or Add/Edit Product
→ Back
→ Manage Staff
→ Back
→ View Reports
→ Back
→ Promotions
→ Back
→ Inventory
```

Expected:

```text
Quick actions được wire.
Inventory Toast coming soon.
Không crash.
```

---

## 14. Build / QA commands

Agent phải chạy ít nhất:

```powershell
.\gradlew.bat assembleDebug
```

Nếu pass thì chạy thêm:

```powershell
.\gradlew.bat build
```

Nếu `assembleDebug` pass nhưng `build` fail vì lint warning không liên quan flow, không được phá navigation để sửa warning nhỏ. Ưu tiên basic flow trước.

---

## 15. Acceptance Criteria

App đạt basic flow khi:

- [ ] Splash tự chuyển Welcome.
- [ ] Welcome mở được Customer Home guest.
- [ ] Welcome mở được Login.
- [ ] Login mở được Register.
- [ ] Login mở được Forgot Password.
- [ ] Login fake role CUSTOMER mở Customer Home.
- [ ] Login fake role STAFF mở Staff Dashboard.
- [ ] Login fake role KITCHEN mở Kitchen Board.
- [ ] Login fake role SHIPPER mở Shipper Delivery Dashboard.
- [ ] Login fake role ADMIN mở Admin Dashboard.
- [ ] Register back về Login.
- [ ] Forgot Password back về Login.
- [ ] Customer Home `tvSeeAll` mở Pizza List.
- [ ] Pizza List item mở Pizza Detail.
- [ ] Pizza Detail Add to Cart không crash.
- [ ] Pizza Detail/ Pizza List mở được Cart.
- [ ] Cart mở Checkout khi logged in.
- [ ] Cart mở Login Required khi guest checkout.
- [ ] Checkout Place Order mở Order Success.
- [ ] Order Success Track Order mở Order Tracking.
- [ ] Customer bottom nav không crash.
- [ ] Staff Dashboard item mở Staff Order Detail.
- [ ] Kitchen Board item mở Kitchen Order Detail.
- [ ] Shipper Dashboard item mở Shipper Detail.
- [ ] Admin quick actions được wire.
- [ ] 3 modal mở được và dismiss được.
- [ ] `assembleDebug` pass.

---

## 16. Prompt đưa cho fw-coding agent

```text
Read FLOW_TEST_CONTRACT.md first.

Task:
Verify and fix PizzaTime basic navigation flow.

Priority:
1. Do not redesign UI.
2. Do not add backend/API.
3. Do not replace existing architecture.
4. Only wire missing click actions and navigation.
5. Fake data is acceptable.
6. Data-heavy logic can be Toast coming soon.
7. Basic screen-to-screen navigation must work.
8. Use StaffFlowNavigator.kt for shared navigation helpers.
9. Avoid duplicate fragment transactions across screens.
10. Keep assembleDebug passing.

Main customer flow must work:
Open app
→ Splash
→ Welcome
→ Login
→ Customer Home
→ Pizza List
→ Pizza Detail
→ Cart
→ Checkout
→ Order Success
→ Order Tracking

Also verify role flows:
STAFF → Staff Dashboard → Staff Order Detail
KITCHEN → Kitchen Board → Kitchen Order Detail
SHIPPER → Shipper Dashboard → Shipper Detail
ADMIN → Admin Dashboard → Manage screens

After changes, run:
.\gradlew.bat assembleDebug

Then report:
- Files changed
- Click actions wired
- Flows verified
- Remaining TODOs
```

---

## 17. Ghi chú cuối

Đây là contract để ổn định flow trước. Sau khi flow chạy chuẩn mới xử lý tiếp:

```text
SessionManager thật
Room cart thật
Retrofit API
Token auth
Register/Forgot Password thật
Search/filter thật
Payment thật
Notification thật
Map/push realtime
```

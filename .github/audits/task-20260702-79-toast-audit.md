# Task 79 Toast Audit

Baseline: `3b1ee3396951ab014a81bd52e783b058efbf7fbc`

- Direct `Toast.makeText` calls: 110
- Toast imports: 37
- Indirect helper definitions that rendered Toast: 10
- Production Kotlin files affected: 37

Each row records the original call count and grouped use sites. `Screen bottom nav` means the shared renderer
discovers the visible role navigation; `root` means the current Fragment view is used without an anchor.

## Authentication And Customer

| File | Calls and original use sites | Category | Replacement | Anchor | Async/lifecycle |
| --- | --- | --- | --- | --- | --- |
| `feature/splash/SplashFragment.kt` | 1: incompatible session/app routing | unauthorized navigation | app UI message bus, error Snackbar after navigation | activity/current nav | yes |
| `feature/auth/forgot/ForgotPasswordFragment.kt` | 1: reset email result | validation, success/error | field error; Snackbar | root | yes |
| `feature/auth/LoginFragment.kt` | 4: edition mismatch, login success/failure, empty pending cart | success, error, information | Snackbar/app UI message bus | activity/current nav | yes |
| `feature/auth/RegisterFragment.kt` | 5: unavailable providers, registration success/failure, empty pending cart | success, information, error | Snackbar/app UI message bus | activity/current nav | yes |
| `feature/customer/home/CustomerHomeFragment.kt` | 5: featured state, sign-in requirement, invalid product, favorite result | information, unauthorized, success/error | Snackbar; omit normal-state message | screen bottom nav | yes |
| `feature/customer/menu/PizzaMenuFragment.kt` | 1: filter update | information | selected UI state, no transient message | n/a | no |
| `feature/customer/detail/PizzaDetailFragment.kt` | 7: topping changes, add to cart, customize unavailable, sign-in, invalid product, favorite result | information, success, warning, error | Snackbar | screen bottom nav | yes |
| `feature/customer/cart/CartFragment.kt` | 3: promo applied/invalid/ineligible | success, validation | Snackbar; inline promo error | screen bottom nav | yes |
| `feature/customer/checkout/CheckoutFragment.kt` | 2: payment/order/empty-cart helper paths | success, information, error | Snackbar | screen bottom nav | yes |
| `feature/customer/customize/BuildYourPizzaFragment.kt` | 1: custom pizza added | success | Snackbar | screen bottom nav | no |
| `feature/customer/menubottomsheet/CustomerMenuBottomSheetDialog.kt` | 1: unavailable menu destination | unavailable navigation | app UI message bus | activity/current nav | no |
| `feature/customer/notifications/CustomerNotificationsFragment.kt` | 6: unavailable inbox actions | unavailable navigation, error | Snackbar | screen bottom nav | no |
| `feature/customer/account/CustomerAccountFragment.kt` | 1 helper: profile/order/password/menu/logout result paths | validation, success, error, destructive confirmation | inline errors, Snackbar, Material dialog | screen bottom nav | yes |
| `feature/customer/memberqr/CustomerMemberQrFragment.kt` | 1 helper: member QR feedback | information/error | Snackbar | screen bottom nav | no |
| `feature/customer/promos/CustomerPromoCodesFragment.kt` | 1 helper: load/apply/eligibility feedback | success, warning, error | Snackbar; existing empty state | screen bottom nav | yes |
| `feature/customer/order/OrderTypeFragment.kt` | 1: order type selected | information | selected UI state, no transient message | n/a | no |
| `feature/customer/orderhistory/CustomerOrderHistoryFragment.kt` | 1 helper: unavailable order action | unavailable navigation | Snackbar | screen bottom nav | no |
| `feature/customer/orderdetail/CustomerOrderDetailFragment.kt` | 11: load/action/review/cancel branches | success, validation, error, destructive confirmation | inline errors, Snackbar, Material dialog | screen bottom nav | yes |
| `feature/customer/tracking/OrderTrackingFragment.kt` | 3: unavailable order/item/support | information, error | Snackbar/app UI message bus | screen bottom nav | yes |
| `feature/customer/support/SupportFaqFragment.kt` | 1: contact action | information | Snackbar | screen bottom nav | no |
| `shared/location/MapPickerFragment.kt` | 3: permission/location/address paths | validation, warning, error | field error, Snackbar, existing permission UI | root | yes |

Authentication/customer/shared subtotal: 74 direct calls in 21 files.

## Staff

| File | Calls and original use sites | Category | Replacement | Anchor | Async/lifecycle |
| --- | --- | --- | --- | --- | --- |
| `feature/staff/dashboard/StaffDashboardFragment.kt` | 3: queue/action/load feedback | success, error, empty state | Snackbar; existing empty state | screen bottom nav | yes |
| `feature/staff/orderdetail/StaffOrderDetailFragment.kt` | 6: transition/assignment/cancel failures and results | success, warning, error | Snackbar, Material confirmation | screen bottom nav | yes |
| `feature/staff/navigation/StaffFlowNavigator.kt` | 5: unavailable/unauthorized destinations | unavailable navigation | app UI message bus | activity/current nav | no |
| `feature/staff/assignment/AssignShipperDialogFragment.kt` | 1: selection/assignment feedback | validation, success/error | inline error; parent feedback | parent screen nav | yes |

Staff subtotal: 15 direct calls in 4 files.

## Kitchen

| File | Calls and original use sites | Category | Replacement | Anchor | Async/lifecycle |
| --- | --- | --- | --- | --- | --- |
| `feature/kitchen/board/KitchenBoardFragment.kt` | 4: filter/transition/listener feedback | success, information, error | Snackbar for direct actions; no filter/listener message | screen bottom nav | yes |
| `feature/kitchen/orderdetail/KitchenOrderDetailFragment.kt` | 5: order transition results | success, warning, error | Snackbar | screen bottom nav | yes |

Kitchen subtotal: 9 direct calls in 2 files.

## Shipper

| File | Calls and original use sites | Category | Replacement | Anchor | Async/lifecycle |
| --- | --- | --- | --- | --- | --- |
| `feature/shipper/dashboard/ShipperDeliveryDashboardFragment.kt` | 2: accept/list feedback | success, empty state/error | Snackbar; existing empty state | screen bottom nav | yes |
| `feature/shipper/detail/ShipperDeliveryDetailFragment.kt` | 7: delivery transitions and unavailable phone/maps | success, warning, error | Snackbar | screen bottom nav | yes |

Shipper subtotal: 9 direct calls in 2 files.

## Admin

| File | Calls and original use sites | Category | Replacement | Anchor | Async/lifecycle |
| --- | --- | --- | --- | --- | --- |
| `feature/admin/dashboard/AdminDashboardFragment.kt` | 2: dashboard load/quick action feedback | information, error | Snackbar; existing empty state | screen bottom nav | yes |
| `feature/admin/menu/ManageMenuFragment.kt` | 1 helper: product action feedback | validation, success/error, confirmation | inline errors, Snackbar, Material dialog | screen bottom nav | yes |
| `feature/admin/orders/ManageOrdersFragment.kt` | 5: unavailable actions/filter/load feedback | information, warning, error | Snackbar; selected/empty UI state | screen bottom nav | yes |
| `feature/admin/product/AddEditProductFragment.kt` | 1 helper: validation/save/upload feedback | validation, success, error | field errors; Snackbar | screen bottom nav | yes |
| `feature/admin/promos/ManagePromoCodesFragment.kt` | 1 helper: validation and CRUD feedback | validation, success/error, confirmation | field errors, Snackbar, Material dialog | screen bottom nav | yes |
| `feature/admin/staff/ManageStaffFragment.kt` | 4: load/validation/update/remove feedback | validation, success/error, confirmation | field errors, Snackbar, Material dialog | screen bottom nav | yes |
| `feature/admin/settings/StoreSettingsFragment.kt` | 2: load/save feedback | validation, success/error | field errors, Snackbar | screen bottom nav | yes |

Admin subtotal: 16 direct calls in 7 files.

## Core Notification System

| File | Calls and original use sites | Category | Replacement | Anchor | Async/lifecycle |
| --- | --- | --- | --- | --- | --- |
| `core/notification/PizzaTimeNotificationManager.kt` | 1: foreground order notification | foreground domain notification | app UI message bus collected by `MainActivity` | activity/current role nav | yes |

Core subtotal: 1 direct call in 1 file. Background `NotificationManagerCompat` delivery remains a system notification.

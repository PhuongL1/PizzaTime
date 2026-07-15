# Task 79B btnMenu Audit

| Layout | Fragment/include owner | Role | Existing btnMenu action | Badge before 79B | 79B binding |
| --- | --- | --- | --- | --- | --- |
| `layout_customer_home_top_bar` | `CustomerHomeFragment` | Customer/guest browsing | Existing customer menu bottom sheet | None | `bindCustomerTopBar` |
| `layout_customer_top_bar` | `CustomerFavoritesFragment` | Customer | Existing customer menu bottom sheet | None | `bindCustomerTopBar` |
| `layout_customer_top_bar` | `CustomerPromoCodesFragment` | Customer | Existing customer menu bottom sheet | None | `bindCustomerTopBar` |
| `layout_customer_top_bar` | `CustomerOrderHistoryFragment` | Customer | Existing customer menu bottom sheet | None | `bindCustomerTopBar` |
| `layout_customer_top_bar` | `CustomerAccountFragment` | Customer/guest | Existing customer menu bottom sheet | None | `bindCustomerTopBar` |
| `layout_customer_top_bar` | `CustomerMemberQrFragment` | Customer | Existing customer menu bottom sheet | None | `bindCustomerTopBar` |
| `layout_staff_top_bar` | `StaffDashboardFragment` | Staff | Existing role menu action | None | `bindStaffTopBar` |
| `layout_staff_top_bar` | `KitchenBoardFragment` | Kitchen | Existing role menu action | None | `bindStaffTopBar` |
| `layout_staff_top_bar` | `ShipperDeliveryDashboardFragment` | Shipper | Existing role menu action | None | `bindStaffTopBar` |
| `layout_staff_top_bar` | `AdminDashboardFragment` | Admin | Existing admin menu action | None | `bindAdminTopBar` to `bindStaffTopBar` |
| `layout_staff_top_bar` | `ManageMenuFragment` | Admin | Existing back/dashboard action | None | `bindAdminTopBar` to `bindStaffTopBar` |
| `layout_staff_top_bar` | `ManagePromoCodesFragment` | Admin | Existing admin menu action | None | `bindAdminTopBar` to `bindStaffTopBar` |

Back-only detail screens and custom top bars without `btnMenu` are intentionally unchanged. The notification badge is
an overlay inside each of the three real shared layouts and does not replace or wrap any existing click listener.

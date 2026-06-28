# Funnel Analysis Algorithm

## Kiểu thuật toán: Sequential Ordered Funnel (CTE Chaining)

**Nguồn dữ liệu:** GA4 event tables trên BigQuery (`events_*` partitioned theo ngày).

---

## Nguyên tắc cốt lõi

Mỗi user phải hoàn thành các bước **theo đúng thứ tự**, và bước sau phải xảy ra **sau bước trước về mặt timestamp**. User không đi đúng thứ tự sẽ bị loại khỏi step đó.

---

## Cấu trúc SQL (BigQuery CTE Chaining)

```sql
WITH base AS (
  -- 1. Một lần scan duy nhất, chỉ lấy các event liên quan
  SELECT user_pseudo_id, event_name, event_timestamp
  FROM `{project}.{dataset}.events_*`
  WHERE _TABLE_SUFFIX BETWEEN {dateFrom} AND {dateTo}
    AND event_name IN ('step_A', 'step_B', 'step_C', ...)
),

-- 2. Step 0: tất cả user đã fire event đầu tiên
s0 AS (
  SELECT user_pseudo_id, MIN(event_timestamp) AS ts
  FROM base
  WHERE event_name = 'step_A'
  GROUP BY user_pseudo_id
),

-- 3. Step 1: chỉ user đã qua s0, và fire step_B SAU timestamp của s0
s1 AS (
  SELECT b.user_pseudo_id, MIN(b.event_timestamp) AS ts
  FROM base b
  JOIN s0 ON b.user_pseudo_id = s0.user_pseudo_id
  WHERE b.event_name = 'step_B'
    AND b.event_timestamp >= s0.ts   -- điều kiện then chốt: ordered in time
  GROUP BY b.user_pseudo_id
),

-- 4. Tương tự với s2, s3, ... (chain tiếp tục với mỗi bước)
s2 AS (
  SELECT b.user_pseudo_id, MIN(b.event_timestamp) AS ts
  FROM base b
  JOIN s1 ON b.user_pseudo_id = s1.user_pseudo_id
  WHERE b.event_name = 'step_C'
    AND b.event_timestamp >= s1.ts
  GROUP BY b.user_pseudo_id
)

-- 5. Đếm distinct users mỗi step
SELECT 0 AS step_idx, COUNT(*) AS users FROM s0
UNION ALL
SELECT 1 AS step_idx, COUNT(*) AS users FROM s1
UNION ALL
SELECT 2 AS step_idx, COUNT(*) AS users FROM s2
ORDER BY step_idx
```

---

## Tính toán Conversion Rate (phía API / TypeScript)

Sau khi BigQuery trả về mảng `counts[]`:

```typescript
// counts[i] = số distinct users đã hoàn thành step i

const steps = eventNames.map((eventName, i) => ({
  step: i + 1,         // 1-based
  eventName,
  users: counts[i],

  // Tỉ lệ so với step đầu tiên (overall funnel conversion)
  overallConvRate: counts[0] > 0 ? counts[i] / counts[0] : 0,

  // Tỉ lệ so với bước liền trước (step-over-step drop-off)
  stepConvRate: i === 0 ? 1.0 : (counts[i - 1] > 0 ? counts[i] / counts[i - 1] : 0),
}));
```

---

## Data Contract

### Input

```typescript
filter: {
  appKey: string,
  dateFrom: string,  // "YYYY-MM-DD"
  dateTo:   string,  // "YYYY-MM-DD"
}
steps: string[]
// Mảng event names theo đúng thứ tự
// Ví dụ: ["first_open", "screen_view", "add_to_cart", "purchase"]
```

### Output — mỗi phần tử trong `steps[]`

```typescript
{
  step: number,            // 1-based index
  eventName: string,
  users: number,           // distinct users đã hoàn thành step này VÀ tất cả bước trước
  overallConvRate: number, // [0..1] so với step 1
  stepConvRate: number,    // [0..1] so với bước liền trước; luôn = 1.0 cho step 1
}
```

---

## Quy tắc khi tích hợp Event

| Rule | Chi tiết |
|------|----------|
| **Tên event** | Chỉ dùng `[a-zA-Z0-9_]`, tối đa 40 ký tự (GA4 standard) |
| **Deduplication** | Dùng `MIN(event_timestamp)` — chỉ lấy lần đầu tiên user fire event đó trong range |
| **Ordering** | Bước sau PHẢI có `event_timestamp >= timestamp` của bước trước |
| **User identity** | Join theo `user_pseudo_id` (GA4 anonymous ID) |
| **Date partition** | Bắt buộc filter `_TABLE_SUFFIX` để tránh full table scan trên BigQuery |
| **Phạm vi** | Toàn bộ funnel tính trên **cùng một date range** — không phải cohort |
| **Ký tự đặc biệt** | Sanitize trước khi đưa vào SQL: `s.replace(/[^a-zA-Z0-9_]/g, '_').slice(0, 40)` |

---

## Quy ước đặt tên Event

### Format chung

```
{category}_{action}_{object?}
```

- Tất cả **lowercase**, dùng dấu `_` làm separator
- Không dùng dấu cách, gạch ngang, hoặc ký tự đặc biệt
- Tối đa **40 ký tự** (giới hạn của GA4)

---

### Danh mục (Category) và prefix chuẩn

| Category | Prefix | Ví dụ |
|----------|--------|-------|
| **Click** | `click_` | `click_button_buy`, `click_banner_promo` |
| **View** | `view_` | `view_screen_home`, `view_item_detail` |
| **IAP / Purchase** | *(keyword trong tên)* | `in_app_purchase`, `subscription_start`, `buy_coins`, `payment_success` |
| **Ads** | *(keyword trong tên)* | `ad_impression`, `ad_reward_claimed` |
| **Error** | `error_` hoặc `app_exception` | `error_network_timeout`, `app_exception` |
| **System** *(GA4 auto)* | — | `first_open`, `session_start`, `screen_view`, `user_engagement`, `app_update`, `os_update`, `app_remove`, `firebase_campaign`, `notification_receive`, `notification_foreground` |

> ⚠️ Các event **System** được GA4 tự log — **không cần tự fire**, chỉ cần dùng tên đúng khi cấu hình funnel.

---

### Nhận diện category theo tên (logic dashboard)

Dashboard phân loại tự động theo pattern:

```
tên chứa "error"  hoặc  == "app_exception"       → Error
tên chứa "ad_impression" hoặc "ad_reward"        → Ad
tên chứa "sub", "buy", "pay"                     → IAP
bắt đầu bằng "click_"                            → Click
bắt đầu bằng "view_"                             → View
các event System GA4 (danh sách cố định ở trên)  → System
còn lại                                          → Other
```

---

### Ví dụ đặt tên cho một user journey điển hình

```
first_open              ← GA4 auto (System)
screen_view             ← GA4 auto (System)
view_screen_onboarding  ← custom (View)
click_button_start      ← custom (Click)
view_item_premium       ← custom (View)
click_button_buy        ← custom (Click)
in_app_purchase         ← custom / StoreKit (IAP)
```

---

### Những cái cần tránh

| Sai | Đúng |
|-----|------|
| `ButtonTapped` | `click_button_home` |
| `screen-view-home` | `view_screen_home` |
| `buy flow step 1` | `view_screen_checkout` |
| `PurchaseComplete` | `in_app_purchase` |
| `err_net` (quá ngắn, không rõ) | `error_network_timeout` |

---

## Pattern thực tế trong project này

Dựa trên mock data của dashboard, pattern đang dùng là:

```
{category}_{screen_name}_{action_or_object}_{platform}
```

| Event thực tế | Phân tích |
|---|---|
| `view_home_screen_vocalize_aos` | view \| màn home \| tab vocalize \| Android |
| `click_generates_screen_voice_aos` | click \| màn generates \| nút voice \| Android |
| `click_login_screen_with_google_aos` | click \| màn login \| option google \| Android |
| `click_play_audio_screen_download_aos` | click \| màn play audio \| nút download \| Android |
| `ad_impression_android` | ad impression \| Android |
| `server_error_me_aos` | server error \| endpoint /me \| Android |

**Platform suffix:** `_aos` = Android, `_ios` = iOS.

---

## Ví dụ 1 — Tính năng Edit / Customize

Giả sử app có màn chỉnh sửa profile và màn custom voice:

```
# Vào màn
view_edit_profile_screen_aos
view_customize_voice_screen_aos

# Các hành động trong màn edit profile
click_edit_profile_screen_change_avatar_aos
click_edit_profile_screen_change_name_aos
click_edit_profile_screen_save_aos
click_edit_profile_screen_cancel_aos

# Các hành động trong màn customize voice
click_customize_voice_screen_pitch_aos       ← chỉnh pitch
click_customize_voice_screen_speed_aos       ← chỉnh speed
click_customize_voice_screen_preview_aos     ← nghe thử
click_customize_voice_screen_apply_aos       ← áp dụng
click_customize_voice_screen_reset_aos       ← reset về default

# Kết quả
customize_voice_save_success_aos             ← lưu thành công
error_customize_voice_save_aos               ← lỗi khi lưu
```

**Funnel tích hợp ngay được:**
```
steps = [
  "view_customize_voice_screen_aos",
  "click_customize_voice_screen_preview_aos",
  "click_customize_voice_screen_apply_aos",
  "customize_voice_save_success_aos"
]
```

---

## Ví dụ 2 — Tracking Preset / Category / Item động (số lượng lớn)

### Vấn đề với encode vào event_name

```
// ❌ Không scale được khi preset do user tạo hoặc số lượng lớn
click_preset_dark_voice_apply_aos   ← tên preset bị giới hạn 14 ký tự
click_preset_dark_voice_apply_aos   ← không phân biệt được version preset
```

### Giải pháp: Dùng `event_params` + API `param-top-values`

Dashboard **đã hỗ trợ đọc `event_params`** qua endpoint `param-top-values`. Mobile app chỉ cần log event với param, không cần encode vào tên.

#### Cách log trên mobile

```swift
// iOS — Swift / SwiftUI
Analytics.logEvent("click_preset_apply", parameters: [
  "preset_id": "dark_voice_v2",        // ID hoặc slug của preset
  "preset_category": "voice",           // category của preset
  "source_screen": "library"            // user đang ở màn nào khi apply
])

Analytics.logEvent("click_preset_favorite", parameters: [
  "preset_id": "soft_female"
])

Analytics.logEvent("click_tab_category", parameters: [
  "category_name": "voice"
])
```

```kotlin
// Android — Kotlin
firebaseAnalytics.logEvent("click_preset_apply") {
    param("preset_id", "dark_voice_v2")
    param("preset_category", "voice")
    param("source_screen", "library")
}

firebaseAnalytics.logEvent("click_preset_favorite") {
    param("preset_id", "soft_female")
}
```

#### API query trên dashboard

```
GET /api/v1/dashboards/analytics/param-top-values
  ?appKey=my-app
  &eventName=click_preset_apply
  &paramKey=preset_id
  &dateFrom=2026-06-01
  &dateTo=2026-06-13
  &limit=50
```

**Response:**
```json
{
  "eventName": "click_preset_apply",
  "paramKey": "preset_id",
  "rows": [
    { "paramValue": "dark_voice_v2", "eventCount": 1234, "uniqueUsers": 890 },
    { "paramValue": "soft_female",   "eventCount":  987, "uniqueUsers": 712 },
    { "paramValue": "robot",         "eventCount":  543, "uniqueUsers": 401 }
  ]
}
```

#### Map câu hỏi → query

| Câu hỏi | `eventName` | `paramKey` |
|---|---|---|
| Preset nào được apply nhiều nhất? | `click_preset_apply` | `preset_id` |
| Preset nào được favorite nhiều nhất? | `click_preset_favorite` | `preset_id` |
| Category tab nào được chọn nhiều nhất? | `click_tab_category` | `category_name` |
| Màn nào user hay apply preset nhất? | `click_preset_apply` | `source_screen` |
| Item nào được share nhiều nhất? | `click_share_item` | `item_id` |
| Level nào được chơi nhiều nhất? | `level_start` | `level_number` |

---

## Khi nào dùng encode vào `event_name`, khi nào dùng `event_params`?

| Tiêu chí | Encode vào `event_name` | Dùng `event_params` |
|---|---|---|
| Số lượng giá trị | Nhỏ, cố định (< 10) | Lớn hoặc động (preset do user tạo, item ID) |
| Độ dài tên | Phải ≤ 40 ký tự tổng | Param value không giới hạn bởi event_name |
| Dùng Funnel | ✅ Có thể dùng trực tiếp | ❌ Funnel chỉ filter theo event_name |
| Dùng Top Values | ✅ Top Events hiện sẵn | ✅ Dùng `param-top-values` endpoint |
| Ví dụ | `click_tab_voice_aos`, `click_tab_ad_aos` | preset_id, item_id, level_number |

**Quy tắc:** Nếu giá trị là **danh sách cố định ≤ 10 mục**, encode vào event_name. Nếu là **ID động hoặc danh sách lớn**, dùng `event_params`.

---

---

## Ví dụ Funnel thực tế

```
steps = ["first_open", "screen_view", "purchase"]
dateFrom = "2026-01-01"
dateTo   = "2026-01-31"

Kết quả:
  step 1 — first_open : 1000 users | overallConvRate=1.00 | stepConvRate=1.00
  step 2 — screen_view:  820 users | overallConvRate=0.82 | stepConvRate=0.82
  step 3 — purchase   :  123 users | overallConvRate=0.12 | stepConvRate=0.15
```

---

## Nguồn code

| Thành phần | File | Symbol |
|---|---|---|
| Funnel SQL + BQ impl | `apps/api/src/services/analyticsService.ts` | `BigQueryAnalyticsService.getFunnel()` |
| Top Param Values SQL | `apps/api/src/services/analyticsService.ts` | `BigQueryAnalyticsService.getTopParamValues()` |
| Mock impl | `apps/api/src/services/analyticsService.ts` | `MockAnalyticsService.*` |
| Funnel types | `packages/shared-types/src/index.ts` | `FunnelStepResult`, `FunnelAnalysisResponse` |
| Param types | `packages/shared-types/src/index.ts` | `ParamTopValueRow`, `ParamTopValuesResponse` |
| Route — funnel | `apps/api/src/routes/dashboards.ts` | `GET /api/v1/dashboards/analytics/funnel` |
| Route — param top values | `apps/api/src/routes/dashboards.ts` | `GET /api/v1/dashboards/analytics/param-top-values` |
| Route — next events | `apps/api/src/routes/dashboards.ts` | `GET /api/v1/dashboards/analytics/next-events` |
| Category helper (web) | `apps/web/src/pages/EventsPage.tsx` | `eventCategory()` |

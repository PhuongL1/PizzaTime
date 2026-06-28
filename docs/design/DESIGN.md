# PizzaTime Design Guide

## 1. Purpose

This document defines the visual direction, layout rules, UI component system, screen checklist, and implementation-friendly design rules for the PizzaTime Android app.

PizzaTime is an Android pizza ordering and restaurant operation app. It supports customer ordering and internal order processing in one single app.

The app has one shared login screen. After login, the user role decides which workspace the user enters.

Supported roles:

* `GUEST`
* `CUSTOMER`
* `STAFF`
* `KITCHEN`
* `SHIPPER`
* `ADMIN`

This design guide is the source of truth for converting Stitch/Figma/screenshot designs into Android XML layouts.

---

## 2. Design Concept

### Concept Name

```text
Midnight Artisan Pizzeria
```

### Design Direction

PizzaTime should feel like a premium late-night artisan pizza restaurant, not a generic fast-food pizza app.

The UI should be:

* Premium
* Warm
* Minimal
* Modern
* Clean
* Easy to scan
* Android XML friendly
* Consistent across all roles

### Anti-copy Rule

Do not copy or imitate:

* Pizza Hut
* Domino’s
* Papa John’s
* Any existing pizza brand

Avoid:

* Full red-yellow fast-food theme
* Roof-style pizza logos
* Checkerboard restaurant patterns
* Cheap cartoon pizza visuals
* Overloaded promotional UI

---

## 3. Platform & Implementation Target

```text
Platform: Android
UI Technology: XML Layout
Language: Kotlin
Architecture: MVVM
Networking: Retrofit + REST API
Backend: Node.js
Database: MySQL
Design Size: 390 x 844 portrait
Spacing System: 8dp grid
```

The design must be easy to implement with:

* `ConstraintLayout`
* `LinearLayout`
* `RecyclerView`
* `Material Components`
* Reusable drawable backgrounds
* Shared colors and dimens resources

Avoid complex layouts that are hard to convert into Android XML.

---

## 4. Color System

### 4.1 Base Colors

```text
App Background:       #161311
Surface Dark:         #211B17
Surface Soft:         #2A221D
Surface Card:         #FFF4E6
```

Usage:

* Use `App Background` for main dark screens.
* Use `Surface Dark` for dark cards and sections.
* Use `Surface Card` for highlighted cream cards.
* Use `Surface Soft` for secondary surfaces.

### 4.2 Brand Colors

```text
Primary Tomato:       #C2472D
Primary Dark:         #8F2F22
Copper Accent:        #D99A3D
Basil Green:          #3F8F5F
Cream:                #FFF4E6
```

Usage:

* `Primary Tomato`: main CTA buttons, brand highlight.
* `Primary Dark`: pressed/active tomato states.
* `Copper Accent`: price, active timeline step, secondary highlight.
* `Basil Green`: success, ready, delivered.
* `Cream`: cards, forms, readable surfaces.

Do not use tomato red as the full background color. The brand must not look like a common fast-food chain.

### 4.3 Text Colors

```text
Text Primary on Dark:     #F8EFE5
Text Secondary on Dark:   #AFA39A
Text Primary on Card:     #211B17
Text Secondary on Card:   #6F6258
Text Disabled:            #7D746E
```

Rules:

* Text on dark background must be high contrast.
* Text on cream cards must use dark colors.
* Disabled text must be visible but clearly inactive.

---

## 5. Order Status Colors

```text
PENDING:                 #D99A3D
CONFIRMED:               #C2472D
PREPARING:               #D9822B
BAKING:                  #E06A2D
READY:                   #3F8F5F
ASSIGNED_TO_SHIPPER:     #5E7CE2
DELIVERING:              #3C8DBC
DELIVERED:               #2E7D50
CANCELLED:               #8A8A8A
FAILED:                  #B23A3A
```

Rules:

* Every order status must have a consistent badge color.
* The same status color must be used across Customer, Staff, Kitchen, Shipper, and Admin screens.
* The current order status must be visually stronger than completed and future statuses.

---

## 6. Typography

Use a clean sans-serif font.

Recommended hierarchy:

```text
Display:        28sp / Bold
Screen Title:   24sp / SemiBold
Section Title:  18sp / SemiBold
Body Large:     16sp / Regular
Body:           14sp / Regular
Caption:        12sp / Regular
Button:         14sp - 16sp / SemiBold
```

Rules:

* Customer screens can use more visual hierarchy.
* Staff, Kitchen, and Shipper screens need clearer, larger, faster-to-read text.
* Avoid tiny text for operational actions.
* Buttons must use strong readable text.

---

## 7. Spacing & Shape System

### 7.1 Spacing

```text
4dp    Tiny spacing
8dp    Small spacing
12dp   Card inner gap
16dp   Default screen padding
20dp   Medium-large spacing
24dp   Section spacing
32dp   Large section spacing
```

Rules:

* Main screen horizontal padding: `16dp`
* Section spacing: `24dp`
* Card spacing: `12dp - 16dp`
* Minimum button height: `48dp`
* Preferred primary button height: `52dp`

### 7.2 Radius

```text
8dp     Small radius
12dp    Input radius
16dp    Button radius
24dp    Card / modal radius
999dp   Chip / pill radius
```

Usage:

* Input fields: `12dp - 16dp`
* Buttons: `16dp`
* Product/order cards: `20dp - 24dp`
* Chips/status badges: fully rounded

---

## 8. Shared Components

### 8.1 Buttons

Button types:

* Primary Button
* Secondary Button
* Outline Button
* Danger Button
* Icon Button

Rules:

* Each screen should have one main primary action.
* Primary buttons use `Primary Tomato`.
* Secondary highlights may use `Copper Accent`.
* Success actions may use `Basil Green`.
* Dangerous actions such as cancel order must use a danger/warning style.
* Kitchen and Shipper buttons must be large and easy to tap.

### 8.2 Cards

Card types:

* Product Card
* Cart Item Card
* Order Card
* Dashboard Metric Card
* Promo Card
* Profile Card
* Address Card
* Status Card

Rules:

* Product cards prioritize image, name, price, rating, and add action.
* Order cards prioritize order ID, status, time, and next action.
* Metric cards prioritize number first, label second.
* Avoid long paragraphs inside cards.

### 8.3 Inputs

Used in:

* Login
* Register
* Checkout
* Search
* Promo Code
* Admin Forms

Rules:

* Inputs must have clear labels or clear placeholders.
* Error text must be shown below the input.
* Important fields must not blend into the dark background.
* Forms should be vertically aligned and easy to complete.

### 8.4 Status Badges

Used for:

* Order status
* Product availability
* Payment status
* Staff availability
* Shipper availability

Rules:

* Badges must be short and easy to scan.
* Use consistent colors from the status color system.
* Badge text should be uppercase or semi-bold for clarity.

### 8.5 Timeline

Used in Customer Order Tracking.

Timeline steps:

```text
Order placed
Order confirmed
Preparing
Baking
Ready
Delivering
Delivered
```

Rules:

* Completed step: check icon.
* Current step: larger highlighted dot.
* Future step: outline dot.
* Current step can use `Copper Accent` or `Basil Green`.
* For Self-Collect and Dine-In orders, the `Delivering` step can be hidden.

---

## 9. Navigation Rule

PizzaTime has only one shared login screen.

After login, navigation is based on role:

```text
CUSTOMER → Customer Home
STAFF    → Staff Dashboard
KITCHEN  → Kitchen Board
SHIPPER  → Shipper Delivery Dashboard
ADMIN    → Admin Dashboard
```

Do not design or implement:

* Separate Staff Login
* Separate Kitchen Login
* Separate Shipper Login
* Separate Admin Login
* Role Selection Screen

The account role returned by the backend decides the destination screen.

---

## 10. Screen Groups

### 10.1 Shared Screens

Shared screens are used by multiple roles or before role navigation.

Screens:

* Splash Screen
* Welcome Screen
* Unified Login Screen
* Register Screen
* Forgot Password Screen
* Notification Screen
* Support / FAQ Screen
* Login Required Modal

Layout rule:

```text
Top: Logo / title
Middle: Main content or form
Bottom: Primary action + secondary action
```

### 10.2 Customer Screens

Customer screens should feel warm, appetizing, premium, and easy to order from.

Screens:

* Customer Home Screen
* Order Type Screen
* Pizza List Screen
* Pizza Detail Screen
* Build Your Pizza Screen
* Cart Screen
* Checkout Screen
* Order Success Screen
* Order Tracking Screen
* Customer Order Detail Screen
* Order History Screen
* Favorites Screen
* Promo Codes Screen
* Member QR Screen
* Customer Account Screen

Common layout:

```text
Top app area
Search / hero / status section
Main content cards
Sticky CTA when needed
Bottom navigation
```

Suggested bottom navigation:

```text
Home
Menu
Orders
Favorites
Account
```

### 10.3 Staff Screens

Staff screens should be operational, clear, and action-focused.

Screens:

* Staff Dashboard Screen
* Staff Order Detail Screen

Common layout:

```text
Top App Bar
Status Tabs
Order List
Order Detail
Next Action Button
```

Staff priorities:

* View new orders
* Confirm order
* Send order to kitchen
* Assign shipper
* Cancel order when allowed

### 10.4 Kitchen Screens

Kitchen screens should have high contrast and large touch targets.

Screens:

* Kitchen Board Screen
* Kitchen Order Detail Screen

Common layout:

```text
Top App Bar
Status Tabs: Waiting / Preparing / Baking / Ready
Large Order Cards
Large Action Buttons
```

Kitchen priorities:

* View pizza list
* View size, crust, toppings, quantity, and note
* Start Preparing
* Start Baking
* Mark as Ready

### 10.5 Shipper Screens

Shipper screens should be readable outdoors and action-focused.

Screens:

* Shipper Delivery Dashboard Screen
* Shipper Delivery Detail Screen

Common layout:

```text
Top App Bar
Delivery Status Tabs
Delivery Cards
Call / Start / Complete Actions
```

Shipper priorities:

* View assigned orders
* View customer phone
* View delivery address
* View COD amount
* Start delivery
* Call customer
* Complete order

### 10.6 Admin Screens

Admin screens should feel like a clean mobile management dashboard.

Screens:

* Admin Dashboard Screen
* Manage Orders Screen
* Manage Menu Screen
* Add/Edit Product Screen
* Manage Promo Codes Screen
* Staff Management Screen
* Reports Screen

Common layout:

```text
Top App Bar
Metric Cards
Search / Filter
Management List
Primary Admin Action
```

Admin priority is lower than the MVP flow and can be implemented after the main ordering and operation flow is stable.

---

## 11. Screen Checklist

Total UI to design: `39 UI`

### Shared

* [ ] 1. Splash Screen
* [ ] 2. Welcome Screen
* [ ] 3. Unified Login Screen
* [ ] 4. Register Screen
* [ ] 5. Forgot Password Screen
* [ ] 6. Notification Screen
* [ ] 7. Support / FAQ Screen
* [ ] 8. Login Required Modal

### Customer

* [ ] 9. Customer Home Screen
* [ ] 10. Order Type Screen
* [ ] 11. Pizza List Screen
* [ ] 12. Pizza Detail Screen
* [ ] 13. Build Your Pizza Screen
* [ ] 14. Cart Screen
* [ ] 15. Checkout Screen
* [ ] 16. Order Success Screen
* [ ] 17. Order Tracking Screen
* [ ] 18. Customer Order Detail Screen
* [ ] 19. Order History Screen
* [ ] 20. Favorites Screen
* [ ] 21. Promo Codes Screen
* [ ] 22. Member QR Screen
* [ ] 23. Customer Account Screen

### Staff

* [ ] 24. Staff Dashboard Screen
* [ ] 25. Staff Order Detail Screen

### Kitchen

* [ ] 26. Kitchen Board Screen
* [ ] 27. Kitchen Order Detail Screen

### Shipper

* [ ] 28. Shipper Delivery Dashboard Screen
* [ ] 29. Shipper Delivery Detail Screen

### Admin

* [ ] 30. Admin Dashboard Screen
* [ ] 31. Manage Orders Screen
* [ ] 32. Manage Menu Screen
* [ ] 33. Add/Edit Product Screen
* [ ] 34. Manage Promo Codes Screen
* [ ] 35. Staff Management Screen
* [ ] 36. Reports Screen

### Modal / Dialog

* [ ] 37. Cancel Order Confirmation Modal
* [ ] 38. Assign Shipper Modal
* [ ] 39. Status Update Confirmation Modal

---

## 12. MVP Design Priority

Design and implement these screens first:

* [ ] Splash Screen
* [ ] Welcome Screen
* [ ] Unified Login Screen
* [ ] Register Screen
* [ ] Customer Home Screen
* [ ] Pizza List Screen
* [ ] Pizza Detail Screen
* [ ] Cart Screen
* [ ] Checkout Screen
* [ ] Order Success Screen
* [ ] Order Tracking Screen
* [ ] Staff Dashboard Screen
* [ ] Staff Order Detail Screen
* [ ] Kitchen Board Screen
* [ ] Kitchen Order Detail Screen
* [ ] Shipper Delivery Dashboard Screen
* [ ] Shipper Delivery Detail Screen

The MVP must support this core flow:

```text
Customer logs in
→ Customer views pizza menu
→ Customer adds pizza to cart
→ Customer checks out
→ Order is created as PENDING
→ Staff confirms order
→ Kitchen prepares and marks order as ready
→ Shipper delivers order
→ Customer tracks order status
```

Delay these features until after the MVP is stable:

* Full Admin Dashboard
* Manage Menu
* Manage Promo Codes
* Staff Management
* Reports
* Real payment
* Real Google Map
* Push notification
* Realtime Firebase

---

## 13. Role Accent Rule

Use one shared brand system across the entire app.

Small secondary accents may differ by role:

```text
Customer: Tomato + Copper
Staff:    Tomato + Blue
Kitchen:  Copper + Basil Green
Shipper:  Blue + Basil Green
Admin:    Cream + Copper
```

Rules:

* Do not completely change the theme per role.
* Only adjust badges, icons, chips, and secondary actions.
* The entire app must still feel like one unified product.

---

## 14. Image Style

Images should be realistic and premium.

Rules:

* Use realistic pizza photography.
* Use warm lighting.
* Prefer dark background, stone table, or wooden table.
* Avoid cheap cartoon images.
* Avoid images that look like existing pizza brands.
* Keep product images consistent in aspect ratio.

Recommended ratios:

```text
Hero Image:       16:9 or full-width crop
Product Card:     1:1
Pizza Detail:     4:3 or 1:1
Avatar/Icon:      1:1
```

---

## 15. UI States

Important screens should support these states:

```text
Loading
Empty
Error
Success
Disabled
Logged out
No permission
```

Examples:

* Empty cart → Empty Cart State
* Guest tries to checkout → Login Required Modal
* Invalid promo code → Error State
* Product unavailable → Disabled Add to Cart
* Invalid role → Clear session and return to Login

---

## 16. Android XML Resource Mapping

Use these shared Android resources:

### Colors

```text
pt_background
pt_surface_dark
pt_surface_soft
pt_surface_card
pt_primary_tomato
pt_primary_dark
pt_copper
pt_basil_green
pt_cream
pt_text_primary_dark_bg
pt_text_secondary_dark_bg
pt_text_primary_card
pt_text_secondary_card
pt_text_disabled
```

### Dimens

```text
pt_space_4
pt_space_8
pt_space_12
pt_space_16
pt_space_20
pt_space_24
pt_space_32

pt_radius_8
pt_radius_12
pt_radius_16
pt_radius_24

pt_button_height
pt_input_height
pt_icon_size
pt_bottom_nav_height
```

### Reusable Drawables

```text
bg_app_dark.xml
bg_card_cream.xml
bg_card_dark.xml
bg_button_primary.xml
bg_button_secondary.xml
bg_input_dark.xml
```

Rules:

* Do not hardcode colors directly in XML layouts.
* Do not hardcode repeated spacing values.
* Reuse drawable backgrounds.
* Create new drawables only when needed.

---

## 17. AI Agent UI Generation Rules

When using an AI Agent to generate UI from screenshots or design references, follow these rules:

* Generate one screen at a time.
* Do not generate all 39 screens in one request.
* Do not modify Gradle unless explicitly requested.
* Do not generate backend code while working on UI.
* Do not switch to Jetpack Compose.
* Use Kotlin + XML only.
* Prefer `ConstraintLayout`.
* Use existing `colors.xml`, `dimens.xml`, and reusable drawables.
* Keep files small and readable.
* Build after each screen.

Prompt template:

```text
Read docs/design/DESIGN.md and use docs/design/screens/<screen_image>.png as visual reference.

Create only the Android XML layout for <Screen Name>.

Rules:
- Kotlin + XML only.
- Use ConstraintLayout.
- Use existing colors.xml and dimens.xml.
- Use reusable drawables when possible.
- Do not modify Gradle.
- Do not create backend code.
- Do not create unrelated screens.
- Create/update only:
  app/src/main/res/layout/<layout_file_name>.xml
```

---

## 18. Final Design Rules

* One screen should have one main action.
* CTA style must be consistent.
* Status badge colors must be consistent.
* Customer screens should be warm and appetizing.
* Staff screens should be clear and operational.
* Kitchen screens should be high contrast and fast to scan.
* Shipper screens should be action-focused and readable outdoors.
* Admin screens should be clean and managerial.
* Avoid visual clutter.
* Avoid copying real pizza brands.
* Avoid full red-yellow fast-food styling.
* Keep the design realistic for Android XML implementation.

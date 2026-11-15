# Apple System UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle the Student Spending Bill Android app to Apple × light-accent system UI: grouped gray page, stroke-less white cards, sky→pink gradient CTA, iOS-style bottom tabs, and 0.96 press scale — without following old Y2K prompt files.

**Architecture:** Change shared tokens and drawables first so every screen that already references `@drawable/bg_page_gradient`, `@drawable/bg_card`, `@drawable/bg_button_primary`, and `@drawable/bg_input` updates in place. Add a small Java press helper and a reusable bottom-tab include. Then retint layouts that still hardcode candy colors or elevation clutter. No chart-library rewrite; no IA destination changes beyond wiring existing activities into the tab bar.

**Tech Stack:** Android Java, XML resources, Room (untouched), MPAndroidChart (surface colors only).

**Spec:** `docs/superpowers/specs/2026-11-04-apple-system-ui-design.md` on branch `feat/ui-tokens-p0`.

## Global Constraints

- Page background solid `#F2F2F7` (via `bg_page` / `bg_page_gradient` solid).
- Cards white `#FFFFFF`, radius 16–20dp, **no stroke** by default.
- Primary CTA fill: horizontal gradient sky `#5BB8F5` → soft pink `#FFB4D6`, white label, pill radius.
- Selection / link / tab selected: solid sky `#5BB8F5` (not gradient text).
- Text: primary `#1C1C1E`, secondary `#8E8E93`, hint `#AEAEB2`.
- Pastel only on chart series and AI user bubble; no candy glass panels.
- Press feedback: scale 0.96, ~100ms; no layer-list highlight on primary button.
- Do not follow `Promat历史` / old UI prompt Y2K half-fusion rules.
- Pure Java; no Kotlin conversion.
- Branch: `feat/ui-tokens-p0`; commit after each task; push when a task batch is done.
- Verify with Studio rebuild on the user's machine when asked; cloud agent cannot run Android Studio UI.

---

### Task 1: Color and dimen tokens

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/dimens.xml`

**Interfaces:**
- Produces: color names `color_sky`, `color_pink_soft_cta`, `color_primary` (sky solid for selection), `bg_page` `#F2F2F7`, `bg_card` `#FFFFFF`, `bg_input_fill` `#EFEFF4`, `separator` `#C6C6C8`, updated text colors; keep old candy/chart names but stop using them on panels.

- [ ] **Step 1: Update `colors.xml` core tokens**

Replace / add these values (keep unused candy keys so old references compile; retarget panel aliases):

```xml
<!-- System surfaces -->
<color name="bg_page">#F2F2F7</color>
<color name="page_bg_start">#F2F2F7</color>
<color name="page_bg_end">#F2F2F7</color>
<color name="bg_card">#FFFFFF</color>
<color name="bg_card_stroke">#00000000</color>
<color name="bg_input_fill">#EFEFF4</color>
<color name="separator">#C6C6C8</color>
<color name="divider">#C6C6C8</color>

<!-- CTA gradient + selection solid -->
<color name="color_sky">#5BB8F5</color>
<color name="color_pink_soft_cta">#FFB4D6</color>
<color name="color_primary">#5BB8F5</color>
<color name="color_primary_dark">#3A9FE0</color>
<color name="color_primary_light">#E8F6FE</color>

<!-- Text (iOS label colors) -->
<color name="text_primary">#1C1C1E</color>
<color name="text_secondary">#8E8E93</color>
<color name="text_hint">#AEAEB2</color>

<!-- Panel aliases → white / page (no cream) -->
<color name="candy_panel_start">#FFFFFF</color>
<color name="candy_panel_center">#FFFFFF</color>
<color name="candy_panel_end">#FFFFFF</color>
<color name="candy_white">#FFFFFF</color>
```

Leave `chart_*` and `chat_user_bubble` as soft pastels. Leave income/expense semantics.

- [ ] **Step 2: Tweak dimens if needed**

In `dimens.xml` ensure:

```xml
<dimen name="card_radius">16dp</dimen>
<dimen name="radius_input">12dp</dimen>
<dimen name="card_margin_vertical">12dp</dimen>
<dimen name="tab_bar_height">56dp</dimen>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/colors.xml app/src/main/res/values/dimens.xml
git commit -m "feat(ui): Apple system color and dimen tokens"
```

---

### Task 2: Shared drawables (page, card, button, input, chips)

**Files:**
- Modify: `app/src/main/res/drawable/bg_page_gradient.xml`
- Modify: `app/src/main/res/drawable/bg_card.xml`
- Modify: `app/src/main/res/drawable/bg_button_primary.xml`
- Modify: `app/src/main/res/drawable/bg_input.xml`
- Modify: `app/src/main/res/drawable/bg_button_ghost.xml`
- Modify: `app/src/main/res/drawable/bg_soft_capsule.xml`
- Modify: `app/src/main/res/drawable/bg_chart_surface.xml`
- Modify (optional aliases): `bg_y2k_page.xml`, `bg_candy_panel.xml`, `bg_budget_ok.xml` → white card / page gray
- Create: `app/src/main/res/drawable/bg_tab_bar.xml`
- Create: `app/src/main/res/drawable/bg_list_separator.xml`

**Interfaces:**
- Consumes: Task 1 colors/dimens
- Produces: stroke-less card, gradient CTA, gray page, tab bar white+top hairline

- [ ] **Step 1: Solid page background**

`bg_page_gradient.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_page" />
</shape>
```

- [ ] **Step 2: Stroke-less white card**

`bg_card.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_card" />
    <corners android:radius="@dimen/card_radius" />
</shape>
```

- [ ] **Step 3: Sky→pink gradient primary button (no layer-list highlight)**

`bg_button_primary.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:angle="0"
        android:startColor="@color/color_sky"
        android:endColor="@color/color_pink_soft_cta"
        android:type="linear" />
    <corners android:radius="@dimen/radius_pill" />
</shape>
```

- [ ] **Step 4: Filled gray input**

`bg_input.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_input_fill" />
    <corners android:radius="@dimen/radius_input" />
</shape>
```

- [ ] **Step 5: Ghost button, soft capsule, chart surface**

- `bg_button_ghost.xml`: solid `#EFEFF4`, pill corners, no heavy stroke; text uses `@color/color_primary` in layouts.
- `bg_soft_capsule.xml`: solid `#EFEFF4`, pill; text `@color/text_secondary`.
- `bg_chart_surface.xml`: solid `#FFFFFF` or `#F9F9FB`, small radius.

- [ ] **Step 6: Tab bar + separator drawables**

`bg_tab_bar.xml` — white fill with top 1px line via layer-list:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFF" />
        </shape>
    </item>
    <item android:gravity="top" android:height="1dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/separator" />
        </shape>
    </item>
</layer-list>
```

`bg_list_separator.xml`: 1dp `@color/separator` (or 0.5dp if density allows).

- [ ] **Step 7: Retarget candy/Y2K page panels to system surfaces**

Update `bg_y2k_page.xml`, `bg_candy_panel.xml`, `bg_budget_ok.xml` (when not over-budget) to use `@color/bg_page` or `@drawable/bg_card` equivalent solid white / page gray so leftover references do not paint cream.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/res/drawable/
git commit -m "feat(ui): system page/card/CTA/input drawables"
```

---

### Task 3: Press scale helper

**Files:**
- Create: `app/src/main/java/com/campusbill/app/ui/PressScaleHelper.java`
- Modify: `app/src/main/java/com/campusbill/app/MainActivity.java` (wire `btnAddBill`, clickable panels)
- Later tasks wire other activities the same way

**Interfaces:**
- Produces: `PressScaleHelper.attach(View view)` — scale to 0.96 on ACTION_DOWN / cancel restore on UP/CANCEL, duration 100ms

- [ ] **Step 1: Create helper**

```java
package com.campusbill.app.ui;

import android.animation.ObjectAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public final class PressScaleHelper {
    private static final float PRESSED = 0.96f;
    private static final long DURATION_MS = 100L;

    private PressScaleHelper() {}

    public static void attach(View view) {
        if (view == null) {
            return;
        }
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    animate(v, PRESSED);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    animate(v, 1f);
                    break;
                default:
                    break;
            }
            return false; // allow click listeners to fire
        });
    }

    private static void animate(View v, float scale) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(v, View.SCALE_X, scale);
        ObjectAnimator sy = ObjectAnimator.ofFloat(v, View.SCALE_Y, scale);
        sx.setDuration(DURATION_MS);
        sy.setDuration(DURATION_MS);
        sx.setInterpolator(new DecelerateInterpolator());
        sy.setInterpolator(new DecelerateInterpolator());
        sx.start();
        sy.start();
    }
}
```

- [ ] **Step 2: Wire MainActivity after `setContentView`**

```java
PressScaleHelper.attach(findViewById(R.id.btnAddBill));
PressScaleHelper.attach(findViewById(R.id.panelAi));
PressScaleHelper.attach(findViewById(R.id.panelIncome));
PressScaleHelper.attach(findViewById(R.id.panelRank));
PressScaleHelper.attach(findViewById(R.id.panelCalendar));
```

Import `com.campusbill.app.ui.PressScaleHelper`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/campusbill/app/ui/PressScaleHelper.java app/src/main/java/com/campusbill/app/MainActivity.java
git commit -m "feat(ui): press scale helper on main CTA and cards"
```

---

### Task 4: iOS-style bottom tab bar

**Files:**
- Create: `app/src/main/res/layout/include_bottom_tabs.xml`
- Create: `app/src/main/res/drawable/bg_tab_item_selected.xml` (optional light sky wash)
- Create: `app/src/main/java/com/campusbill/app/ui/BottomTabHelper.java`
- Modify: `app/src/main/res/layout/activity_main.xml` — replace bare CTA footer with tab bar + keep「手动记一笔」as primary action above tabs OR promote Add as center tab
- Modify: `activity_calendar.xml`, `activity_ai_chat.xml`, `activity_history.xml` — include same tabs
- Modify: matching Activities to call `BottomTabHelper.bind(this, Tab.HOME|CALENDAR|AI|HISTORY)`
- Modify: `app/src/main/res/values/strings.xml` — short labels: 首页 / 日历 / AI / 历史

**Interfaces:**
- Produces: `BottomTabHelper.bind(Activity activity, Tab selected)` — highlights selected tab (sky color + bold), navigates with `Intent` + `FLAG_ACTIVITY_REORDER_TO_FRONT` / clear-top as appropriate; Add bill remains reachable from Home CTA (`btnAddBill`) and optional center affordance
- Destinations map: HOME→`MainActivity`, CALENDAR→`CalendarActivity`, AI→`AiChatActivity`, HISTORY→`HistoryActivity`

**Note:** App currently has no BottomNavigationView — only a home CTA. This task **introduces** a four-tab iOS bar; do not invent new destinations.

- [ ] **Step 1: `include_bottom_tabs.xml`**

Horizontal `LinearLayout` height `@dimen/tab_bar_height`, background `@drawable/bg_tab_bar`, four equal-weight vertical items (`iv` optional TextView-only if no icons — use emoji or existing vector if present; prefer simple Unicode or `@android:drawable` only if needed). Each item: icon TextView/ImageView + label `@string/tab_*`. IDs: `tabHome`, `tabCalendar`, `tabAi`, `tabHistory`.

- [ ] **Step 2: Strings**

```xml
<string name="tab_home">首页</string>
<string name="tab_calendar">日历</string>
<string name="tab_ai">AI</string>
<string name="tab_history">历史</string>
```

- [ ] **Step 3: Helper**

`BottomTabHelper` sets selected label/icon color to `@color/color_sky`, unselected `@color/text_secondary`; click starts the target Activity if not already current.

- [ ] **Step 4: Integrate Main**

In `activity_main.xml`, keep scroll content; place `btnAddBill` + `btnImport` above the include, or pin CTA above tabs inside a bottom `LinearLayout` with `include layout="@layout/include_bottom_tabs"`. Reduce card `elevation` on panels to `0dp` for flatter grouped look.

- [ ] **Step 5: Integrate Calendar / AI / History**

Same include at bottom; call helper with matching `Tab`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/layout/include_bottom_tabs.xml \
  app/src/main/java/com/campusbill/app/ui/BottomTabHelper.java \
  app/src/main/res/layout/activity_main.xml \
  app/src/main/res/layout/activity_calendar.xml \
  app/src/main/res/layout/activity_ai_chat.xml \
  app/src/main/res/layout/activity_history.xml \
  app/src/main/java/com/campusbill/app/MainActivity.java \
  app/src/main/java/com/campusbill/app/CalendarActivity.java \
  app/src/main/java/com/campusbill/app/AiChatActivity.java \
  app/src/main/java/com/campusbill/app/HistoryActivity.java \
  app/src/main/res/values/strings.xml
git commit -m "feat(ui): iOS-style bottom tabs across main screens"
```

---

### Task 5: Home + Add Bill polish

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/layout/activity_add_bill.xml`
- Modify: `app/src/main/res/layout/activity_edit_bill.xml`
- Modify: `AddBillActivity.java`, `EditBillActivity.java` — `PressScaleHelper` on save CTA

- [ ] **Step 1: Home grouped look**

- Root already uses `@drawable/bg_page_gradient` (now solid gray).
- Set panel elevations to `0dp`.
- Replace `@color/candy_ink` on history capsule with `@color/text_secondary`.
- Ensure CTA uses `@drawable/bg_button_primary` and white text.

- [ ] **Step 2: Add/Edit bill**

- Page background → page drawable; cards → `bg_card`; primary save → `bg_button_primary`; attach press scale.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/activity_main.xml \
  app/src/main/res/layout/activity_add_bill.xml \
  app/src/main/res/layout/activity_edit_bill.xml \
  app/src/main/java/com/campusbill/app/AddBillActivity.java \
  app/src/main/java/com/campusbill/app/EditBillActivity.java
git commit -m "feat(ui): polish home and add/edit bill surfaces"
```

---

### Task 6: Remaining screens + chip/dialog cleanup

**Files:**
- Modify layouts: `activity_import.xml`, `activity_income_detail.xml`, `activity_merchant_rank.xml`, dialogs, list items as needed
- Modify drawables still using candy glass for panels: `bg_candy_glass.xml`, `bg_glass_panel.xml`, `bg_dialog_candy.xml`, chips → system soft gray / sky selected
- Wire `PressScaleHelper` on remaining primary buttons

- [ ] **Step 1: Grep leftover cream/candy panel usage**

```bash
rg -n "bg_y2k_page|bg_candy_panel|candy_ink|page_bg_start|bg_card_stroke|elevation=\"[2-9]" app/src/main/res/layout
```

Retarget hits to system tokens / zero elevation.

- [ ] **Step 2: Soften chips**

Selected chip: light `@color/color_primary_light` fill + `@color/color_primary` text; unselected gray text on `#EFEFF4`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/
git commit -m "feat(ui): systemize remaining screens and chips"
```

---

### Task 7: Push and acceptance checklist

- [ ] **Step 1: Push**

```bash
git push -u origin feat/ui-tokens-p0
```

- [ ] **Step 2: Report SHA + checklist**

Verify against spec acceptance:

1. Pages show `#F2F2F7` + white cards, no cream wash, no card stroke.
2. 「手动记一笔」sky→pink gradient pill; press scales ~0.96; no inset highlight bug.
3. Bottom tabs: white + top hairline; selected sky; 首页/日历/AI/历史.
4. Pastels only on charts / AI bubble.
5. Debug build compiles (user Studio Rebuild).

---

## Self-review (plan author)

1. Spec coverage: tokens, components, press, tabs, full-app retarget, non-goals — mapped to Tasks 1–7.
2. No TBD placeholders in steps.
3. Helper names consistent: `PressScaleHelper.attach`, `BottomTabHelper.bind`.

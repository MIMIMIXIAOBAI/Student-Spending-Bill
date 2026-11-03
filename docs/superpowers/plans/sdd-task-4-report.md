# Task 4 report: iOS-style bottom tab bar

- **Status:** DONE_WITH_CONCERNS
- **Branch:** `feat/ui-tokens-p0`
- **Commit:** `859f81e73063fe58d02f80f491538f582f681b5f` — `feat(ui): iOS-style bottom tabs across main screens`

## Files changed

- Created `app/src/main/res/layout/include_bottom_tabs.xml`
- Created `app/src/main/res/drawable/bg_tab_item_selected.xml`
- Created `app/src/main/java/com/campusbill/app/ui/BottomTabHelper.java`
- Modified `activity_main.xml`, `activity_calendar.xml`, `activity_ai_chat.xml`, `activity_history.xml`
- Modified `MainActivity`, `CalendarActivity`, `AiChatActivity`, `HistoryActivity`
- Modified `app/src/main/res/values/strings.xml` (`tab_home` / `tab_calendar` / `tab_ai` / `tab_history`)

## What landed

Four equal tabs — 首页, 日历, AI, 历史 — sit on `@drawable/bg_tab_bar`. The row height is `@dimen/tab_bar_height` (56dp). Selected tab uses `@color/color_sky` and bold text plus a light `@color/color_primary_light` wash. Unselected tabs use `@color/text_secondary`.

`BottomTabHelper.bind(Activity, Tab)` maps:

- `HOME` → `MainActivity`
- `CALENDAR` → `CalendarActivity`
- `AI` → `AiChatActivity`
- `HISTORY` → `HistoryActivity`

A tap on the current tab does nothing. Other taps use `FLAG_ACTIVITY_REORDER_TO_FRONT | FLAG_ACTIVITY_SINGLE_TOP`. `btnAddBill` stays on Home above the bar. Home panel elevations (`panelAi`, `panelBudget`, `panelChart`, `panelIncome`, `panelRank`, `panelCalendar`) are `0dp`. The add-bill button elevation is still `4dp`.

## Verification

No device or instrumentation run. Layout XML parses. The five touched Java files parse. Tab ids `tabHome`, `tabCalendar`, `tabAi`, `tabHistory` exist on the include. Each activity calls `bind` with the matching tab. Destination classes are the real activities already declared in the manifest. `btnAddBill` is still in `activity_main.xml` above the include.

## Concerns

- None of the four activities set `android:launchMode` (they stay `standard`). `REORDER_TO_FRONT` reuses an instance already in the task, but Back can still return through earlier tabs because `CLEAR_TOP` was not added (that flag would finish the other tab screens). Home panels and the history capsule still call `startActivity` without those flags, so they can push a second copy of Calendar, AI, or History.
- Opening Calendar or AI from the Home tab does not copy `MainActivity`'s selected month. `extra_month` is forwarded only when the current activity's intent already has it.
- `targetSdk` is 36, so the bar wrapper is `wrap_content` and pads for the navigation-bar inset. The tab row itself stays 56dp. The bar hides while the IME is visible so the AI composer is not squeezed.
- Tab glyphs are Unicode text, not icons. Labels still read correctly if a glyph is missing.
- The tab bar was not exercised on a device.

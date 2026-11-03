# Tasks 5–6 report, plus tab back-stack fix

- **Status:** DONE_WITH_CONCERNS
- **Branch:** `feat/ui-tokens-p0`

## Commits

- `e8fb3940277073376a5a36f5f45a3323687ae2f9` — `fix(ui): singleTop tab activities and CLEAR_TOP`
- `8eeb93dd165eb879abccb33a2cc4093eaa5c5b31` — `feat(ui): polish home and add/edit bill surfaces`
- `880b2c8f5cb795765f16fa1efa99600bcc5b2475` — `feat(ui): systemize remaining screens and chips`

## A — Tab back stack

`MainActivity`, `CalendarActivity`, `AiChatActivity`, and `HistoryActivity` are `android:launchMode="singleTop"`. `BottomTabHelper.open` still sets `FLAG_ACTIVITY_SINGLE_TOP` and now also `FLAG_ACTIVITY_CLEAR_TOP` (with the existing `FLAG_ACTIVITY_REORDER_TO_FRONT`). A tab switch finishes activities above the target instead of stacking another copy.

## B — Home, add/edit, remaining screens

Home panels were already `0dp`. The history capsule now uses `@color/text_secondary`. The home CTA already used `@drawable/bg_button_primary` and white text; its elevation is now `0dp`.

Add and edit already used the page drawable, `bg_card`, and `bg_button_primary`. `PressScaleHelper` is attached to both save buttons. Selected type segments use `@color/color_primary_light` with `@color/color_primary` text.

Remaining layout elevations above `0dp` were set to `0dp` (calendar, import, income detail, merchant rank, AI composer, add/edit). Dialog title and amount no longer use `candy_ink`. Glass and candy panel drawables (`bg_candy_glass`, `bg_glass_panel`, `bg_dialog_candy`, `bg_glass_ai`) are solid white with no stroke. Unselected chips (`bg_quick_chip`) are `@color/bg_input_fill` with no stroke; selected chips stay `@color/color_primary_light` and `@color/color_primary`. AI quick chips use secondary text. Press scale is also on the AI send button and the three import actions.

## Verification

No device or instrumentation run. All `res` XML files parse (69 files). The touched Java files parse. Layout grep for `candy_ink`, `bg_y2k_page`, `bg_candy_panel`, and `elevation="[1-9]"` is empty. The manifest has exactly four `singleTop` activities, and the helper contains both `SINGLE_TOP` and `CLEAR_TOP`.

## Concerns

- Jumping back to an earlier tab finishes the activities above it, so that tab’s screen is created again on the next visit.
- Home panels and the history capsule still call `startActivity` without `CLEAR_TOP`. `singleTop` only reuses the instance when it is already on top.
- Pie slices and calendar day dots still use `candy_*` pastels. Those are chart markers, not panel fills. `chat_user_bubble` was left pastel.
- Unused Y2K drawables can still declare a transparent `bg_card_stroke`. Nothing in layouts references them.
- The UI was not exercised on a device.

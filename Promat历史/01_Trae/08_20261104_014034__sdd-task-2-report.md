# Task 2 report: Shared drawables

- **Status:** DONE_WITH_CONCERNS
- **Branch:** `feat/ui-tokens-p0`
- **Commit:** `2c6e7c79ada54549c04429b98dc36cc98da9d2eb` — `feat(ui): system page/card/CTA/input drawables`

## Files changed

Modified:

- `app/src/main/res/drawable/bg_page_gradient.xml` — solid `@color/bg_page`
- `app/src/main/res/drawable/bg_card.xml` — white, `@dimen/card_radius`, no stroke
- `app/src/main/res/drawable/bg_button_primary.xml` — horizontal `@color/color_sky` → `@color/color_pink_soft_cta`, pill, no highlight layer-list
- `app/src/main/res/drawable/bg_input.xml` — solid `@color/bg_input_fill`, `@dimen/radius_input`, no stroke
- `app/src/main/res/drawable/bg_button_ghost.xml` — solid `@color/bg_input_fill` (`#EFEFF4`), pill, no stroke
- `app/src/main/res/drawable/bg_soft_capsule.xml` — same gray pill, no stroke
- `app/src/main/res/drawable/bg_chart_surface.xml` — solid `@color/bg_card`, `@dimen/radius_small`
- `app/src/main/res/drawable/bg_y2k_page.xml` — solid `@color/bg_page`
- `app/src/main/res/drawable/bg_candy_panel.xml` — stroke-less white card
- `app/src/main/res/drawable/bg_budget_ok.xml` — stroke-less white card

Created:

- `app/src/main/res/drawable/bg_tab_bar.xml`
- `app/src/main/res/drawable/bg_list_separator.xml` — 1dp `@color/separator`

No Java, layouts, or `colors.xml` edits.

## Verification

Android drawables have no unit tests. All 12 files parse as well-formed XML. Every `@color` and `@dimen` reference resolves in `colors.xml` / `dimens.xml`. Card, input, ghost, candy panel, budget-ok, and primary button contain no `<stroke>`. Primary button is a single `<shape>` gradient, not a layer-list. `minSdk` is 29, so the tab-bar `item` `android:height` / `android:gravity` attributes are supported.

## Concerns

- Chip drawables (`bg_chip_selected`, `bg_quick_chip`, and the other candy chips) were not changed. The task title mentions chips, but the steps leave selected/unselected chip styling to Task 6.
- `bg_tab_bar` and `bg_list_separator` are not referenced by layouts yet.
- `bg_budget_ok` no longer has a sky stroke, so the ok state matches a plain white card. `bg_budget_over` was left alone.
- Other candy/glass panel drawables (`bg_candy_glass`, `bg_glass_panel`, `bg_dialog_candy`, and similar) are still the old treatments. Task 6 covers those.

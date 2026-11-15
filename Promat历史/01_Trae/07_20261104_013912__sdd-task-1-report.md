# Task 1 report: Color and dimen tokens

- **Status:** DONE
- **Branch:** `feat/ui-tokens-p0`
- **Commit:** `78f34a9b41360aed49eef79cc38d0651bf6a7e28` — `feat(ui): Apple system color and dimen tokens`

## Files changed

- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/dimens.xml`

No drawables, layouts, or Java were edited.

## What landed

- Page surfaces: `bg_page`, `page_bg_start`, `page_bg_end` are `#F2F2F7`.
- Cards: `bg_card` `#FFFFFF`, `bg_card_stroke` `#00000000`.
- Inputs and separators: `bg_input_fill` `#EFEFF4`, `separator` and `divider` `#C6C6C8`.
- CTA and selection: `color_sky` `#5BB8F5`, `color_pink_soft_cta` `#FFB4D6`, `color_primary` `#5BB8F5`, `color_primary_dark` `#3A9FE0`, `color_primary_light` `#E8F6FE`.
- Text: `text_primary` `#1C1C1E`, `text_secondary` `#8E8E93`, `text_hint` `#AEAEB2`.
- Panel aliases `candy_panel_start` / `candy_panel_center` / `candy_panel_end` and `candy_white` are `#FFFFFF`.
- Other candy keys, `chart_*`, `chat_user_bubble`, and income/expense colors were left as they were.
- Dimens: `card_radius` 16dp, `radius_input` 12dp, `card_margin_vertical` 12dp, new `tab_bar_height` 56dp.

## Verification

Android resource XML has no unit tests for these tokens. Verification was a well-formedness parse of both files plus an exact name/value check of the required colors and dimens (73 colors, 18 dimens, no duplicate names). That check passed.

Drawables still paint the old shapes, so the new tokens do not change on-screen chrome until later tasks.

## Concerns

- `page_blue` (`#FFF6F2`) and `page_bubble` (`#33FFD8E8`) were not in the Task 1 replace list and were left unchanged. They are not referenced by layouts.
- Shared drawables such as `bg_page_gradient` and `bg_button_primary` still use the previous structure. They now read the new color values where they reference `@color/bg_page` or `@color/color_primary`, but gradient CTA and stroke-less cards are Task 2.

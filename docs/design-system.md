# Design system

Status: Phase 1 tokens and base components are written. They have not been compiled or run yet (see `handoffs/phase-1.md`).

Module: `core:designsystem`. It depends on Compose runtime, foundation and ui only. Material components are not used, so the look is defined here and not inherited from a stock theme.

## Direction

Dark first. Near-black navy base, thin translucent-looking borders, restrained accent, no neon, no terminal styling. Rounded corners are modest (8, 12 and 16 dp) and used by role, not everywhere. Structure comes from spacing, hairline borders and type weight; cards are used only where a group needs to read as one object.

A light theme exists for the light and system theme settings.

## Colour tokens (`VmColors`)

| Token | Dark | Light | Role |
| --- | --- | --- | --- |
| background | `#07090F` | `#F4F6FA` | Screen background |
| surface | `#0D1220` | `#FFFFFF` | Bars, grouped content |
| surfaceRaised | `#141B2D` | `#EBEFF6` | Inputs, secondary buttons, chips |
| border | white 12% | navy 12% | Hairlines |
| borderStrong | white 24% | navy 24% | Emphasis borders |
| textPrimary | `#E8ECF5` | `#0D1220` | Main text |
| textSecondary | `#9AA5BD` | `#45506B` | Supporting text |
| textTertiary | `#7683A0` | `#66718B` | Hints |
| accent | `#7C97FF` | `#3453D1` | Actions, selection, focus |
| onAccent | `#07090F` | `#FFFFFF` | Text on accent |
| accentSubtle | accent 15% | accent 12% | Selected backgrounds |
| danger / warning / success | `#EC6F78` / `#E3AE55` / `#55BC92` | `#B3261E` / `#8A5A00` / `#1B7A55` | Status |

Contrast for text and accent pairs is checked when the tokens are next changed; the values were chosen to stay above 4.5:1 for normal text but have not been measured with a tool yet.

## Typography (`VmTypography`)

One family, seven roles: display 30/38, title 22/28, subtitle 17/24, body 15/22, bodyStrong 15/22, label 13/18, caption 12/16 (size/line height in sp). The family is the platform default. A licensed typeface is an open decision. Text scales with the system font size.

## Spacing and size (`VmSpacing`, `VmDimens`)

4 dp grid: 2, 4, 8, 12, 16, 24, 32, 48. Minimum touch target 48 dp. Hairline 1 dp. Top bar 56 dp, bottom bar 64 dp, rail 88 dp.

## Components

| Component | Notes |
| --- | --- |
| `VmSurface` | Container with optional hairline border |
| `VmButton` | Primary, Secondary, Text. 48 dp minimum height |
| `VmChip` | Selectable filter or tag chip, exposes selected state |
| `VmTextField` | Single input. The label is the accessible name and the hint while empty |
| `VmTopBar` | Title with leading slot and trailing actions. Save actions go in the trailing slot |
| `VmSectionHeader` | Heading semantics |
| `VmEmptyState` | Title, message, optional action |
| `VmNavigationBar`, `VmNavigationRail` | Bottom bar for compact widths, rail from 840 dp |
| `VmIcon` | Line icons drawn in code: home, people, personal, settings |

## Accessibility rules built in

- Every tappable element is at least 48 dp.
- Tappable elements expose a role (button, checkbox, tab) and selected state.
- Keyboard focus shows a 2 dp accent ring.
- Disabled elements are dimmed and not focusable for clicks.
- Headings are marked as headings.
- Icons are decorative; the containing element provides the name.

Not done yet: reduced-motion handling (no animations exist yet), screen reader testing, contrast measurement, large-font layout testing.

## Layout

The app shell switches between bottom bar and rail at 840 dp. The shell applies the safe drawing insets, which include the on-screen keyboard.

## Right-to-left

Layouts use start and end semantics from Compose, and strings live in resources. Whether the app ships a right-to-left language is an open decision; nothing here prevents it.

## Not in Phase 1

Motion, dialogs, bottom sheets, lists, and the remaining icon set. They are added with the first feature that needs them.

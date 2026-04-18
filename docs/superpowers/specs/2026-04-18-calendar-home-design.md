# Calendar Home Design

**Goal:** Turn the home screen into a real calendar-first entry page with a switchable month view, date selection, red-dot schedule markers, and a day-scoped schedule list.

**Approved Scope**
- Replace the current hero card with a month calendar card.
- Build the month grid with `RecyclerView + GridLayoutManager(7)`.
- Support switching to the previous and next month.
- Show filler days from adjacent months in gray, and make them non-clickable.
- Highlight today and the selected date with distinct visual states.
- Show a red dot on dates that contain schedules.
- Filter the lower schedule list by the selected date.
- Keep a reset-to-today action that resets both the visible month and selected date.
- Preserve the existing day list interactions: drag sorting, long-press menu, edit, delete, and time sorting.

**User Experience**
- Home opens to the current month and selects today.
- The month header shows a Chinese month title, for example "April 2026" in Chinese formatting.
- The calendar card includes left and right month controls.
- Week labels use a Chinese Monday-first layout.
- Tapping a valid date selects it and refreshes the schedule list below.
- The day list title changes with the selection and shows the chosen month and day.
- Tapping the reset-to-today action returns the calendar to today's month, reselects today, and refreshes the list.
- If the user switches to another month and the previous selected date is outside that month, the screen selects day 1 of the new month.

**Screen Structure**
- Top summary chip and page title remain.
- The main card becomes a calendar card with:
  - month header and navigation
  - week label row
  - 7-column date grid
- The lower card remains the schedule section, but it becomes selection-driven instead of global.
- The schedule section header shows the selected date.
- The right-side action changes from a static sort-only layout to a selected-day action area that includes reset-to-today and time sorting.

**Data Design**
- `HomeViewModel` stores:
  - visible month
  - selected date
  - selected-day schedules
  - current-month marker days
- `ScheduleRepository` adds:
  - query schedules for one calendar day
  - query schedule-bearing days for one calendar month
- The repository returns day numbers or local-date keys for marker rendering so the month grid can paint red dots in one pass.
- Manual sort order remains scoped to the selected-day list that is currently shown.

**Implementation Outline**
- Add a calendar grid adapter for date cells.
- Add a small UI model for calendar cells so the fragment does not calculate styles inline.
- Update `HomeFragment` to bind month navigation, date taps, and the reset-to-today action.
- Update `HomeViewModel` to derive:
  - month title
  - week grid data
  - selected-day list title
  - selected-day schedule list
- Extend the repository and DAO path to support day and month lookups.
- Keep the current schedule list adapter and reuse it for the selected day.

**Behavior Rules**
- Gray filler dates are visible but not interactive.
- Today is always visually recognizable, even when not selected.
- Selected state takes precedence over plain today styling.
- Red dots appear on both today and selected cells when that day has schedules.
- Month switching refreshes both the grid and the lower list.
- Drag sorting only affects the currently selected day.
- Time sorting only affects the currently selected day.

**Out of Scope**
- Week view and day view switching.
- Lunar calendar, holidays, and festival labels.
- Long-press on a date to create a schedule.
- Cross-day schedule visualization across multiple cells.
- Heavy page transitions or complex month animations.

**Verification Targets**
- Unit coverage for month-grid generation and selected-day filtering logic.
- Debug build must succeed.
- Home screen must show:
  - correct month switching
  - correct selected-day list refresh
  - correct red-dot rendering for seeded schedules

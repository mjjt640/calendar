# Recurring Schedule Design

**Goal:** Add full recurring schedule support with rule-based series, exception handling, a dedicated recurrence configuration page, and smooth transitions from create/edit flows.

**Approved Scope**
- Add recurrence rules to schedules: `不重复 / 每日 / 每周 / 每月 / 自定义间隔`.
- Add recurrence duration controls in a dedicated page reached from create/edit flows.
- On the create page, show a `重复与持续时间` card only after both start time and end time are chosen.
- On the edit page, add an entry button that jumps to the same recurrence configuration page.
- When editing a recurring schedule, ask the user to choose:
  - `仅这一次`
  - `这一次及之后`
- When deleting a recurring schedule, ask the user to choose:
  - `仅删除这一次`
  - `删除这次及之后`
- Show a small icon-only recurrence marker at the right side of the schedule title in list items.
- Use smooth page transitions and card reveal animations that match the current Chinese-app visual language.

**User Experience**
- A normal one-time schedule still works exactly like now.
- After the user picks both start and end time on the create page, the recurrence card fades in below the time card with a slight upward motion.
- The recurrence card shows a concise summary, for example:
  - `不重复`
  - `每日，持续到 2026-05-31`
  - `每 2 周，持续 12 次`
- Tapping the recurrence card opens a dedicated configuration page.
- Saving configuration returns to the create/edit page and refreshes the summary immediately.
- Editing a recurring schedule does not silently affect future occurrences. The user always chooses the scope first.
- Deleting a recurring schedule also asks for scope before applying the removal.

**Screen Structure**
- `AddScheduleActivity`
  - basic info card
  - time card
  - conditionally visible `重复与持续时间` card
  - bottom save area remains in place
- New recurrence configuration screen
  - page title and back navigation
  - recurrence mode section
  - custom interval section, shown only for custom mode
  - duration section
  - live summary section
  - primary save button
- Edit flow
  - keeps the existing edit page
  - adds a jump entry to the recurrence configuration screen
  - shows scope-selection dialog before entering recurrence edits for recurring schedules

**UI Direction**
- Reuse the current warm cream and orange palette.
- The recurrence card should look like a compact premium setting card, not a plain list row.
- The recurrence marker in schedule rows should be the selected small double-arrow style, icon-only, and visually lighter than the priority indicator.
- Transition style:
  - create/edit page to recurrence page: right-in, left-out
  - back navigation: reverse transition
  - recurrence card reveal: fade + translate up
  - summary update after save: brief highlight pulse on the summary area

**Data Design**
- Keep `Schedule` as the occurrence-facing domain model used by current screens.
- Add a recurrence-series model and persistence path that stores:
  - series id
  - frequency type
  - interval value
  - anchor start time
  - anchor end time
  - duration mode: none / until date / occurrence count
  - duration value
- Add recurrence-exception records for:
  - deleted single occurrence
  - overridden single occurrence
- A one-time schedule has no series id and no recurrence rule.
- A recurring schedule instance is resolved from:
  - its base series rule
  - any later split series
  - any exception records that remove or override one occurrence

**Rule Model**
- `不重复`
  - no recurrence series is created
- `每日`
  - generate occurrences every N days, default N = 1
- `每周`
  - generate occurrences every N weeks, default N = 1
- `每月`
  - generate occurrences every N months, default N = 1
- `自定义间隔`
  - supports unit selection: day / week / month
  - supports interval value greater than 1

**Duration Model**
- `永久`
  - no explicit end condition
- `截止到某天`
  - stops generating occurrences after the chosen date
- `持续 N 次`
  - stops after the chosen occurrence count

**Exception Behavior**
- `仅这一次`
  - editing creates an override exception for that occurrence only
  - deleting creates a deletion exception for that occurrence only
- `这一次及之后`
  - editing splits the original series at the chosen occurrence
  - the original series ends before that occurrence
  - a new series starts from that occurrence with the updated rule or content
  - deleting truncates the original series so all later occurrences disappear

**Repository and Generation Strategy**
- Do not pre-generate large numbers of future rows.
- Use rule-driven occurrence calculation for the visible range that the UI requests.
- Home and calendar screens request occurrences within a day or month window.
- The repository resolves:
  - one-time schedules in that range
  - recurring occurrences derived from series rules in that range
  - exception removals and overrides inside that range
- Manual reordering remains scoped to the rendered list, but recurring rule resolution happens before order is applied.

**Create Flow Behavior**
- User creates title, location, note, priority, and time as today.
- Once start and end are both set, the recurrence card appears.
- If the user never enters recurrence settings, save as a one-time schedule.
- If the user configures recurrence, save both:
  - the base schedule content
  - the recurrence series metadata

**Edit Flow Behavior**
- Editing a non-recurring schedule works as today, with the new recurrence entry available to convert it into a recurring schedule.
- Editing a recurring schedule first asks for scope.
- `仅这一次`
  - opens recurrence edit in single-occurrence mode
  - saving writes an override exception
- `这一次及之后`
  - opens recurrence edit in series-split mode
  - saving updates the current occurrence onward by creating a new series segment

**Delete Flow Behavior**
- Deleting a non-recurring schedule works as today.
- Deleting a recurring schedule shows a scope choice sheet.
- `仅删除这一次`
  - writes a deletion exception
- `删除这次及之后`
  - truncates the current series from the selected occurrence onward

**List and Calendar Rendering**
- Home schedule cards show the recurrence icon on the title row when the item belongs to a recurring series.
- The icon should not replace the priority dot.
- Calendar red-dot markers continue to represent dates that contain at least one visible occurrence, including recurring ones.
- A deleted single occurrence must disappear from both the day list and month markers.
- An overridden single occurrence keeps its original date marker but shows the updated content in the day list.

**Out of Scope**
- Syncing recurring schedules with remote accounts or cloud calendars.
- Complex business rules like `每月最后一个工作日`.
- Multiple weekday selection inside one weekly rule in the first version.
- Holiday skipping.
- Cross-device merge conflict handling.

**Implementation Outline**
- Add recurrence data models, entities, and repository methods.
- Add window-based occurrence generation and exception application in the schedule repository path.
- Add the new recurrence configuration screen and result contract back to create/edit flows.
- Update create/edit view models to store recurrence summary and recurrence payload.
- Update delete/edit actions on recurring schedules to request scope before applying changes.
- Add the small recurrence icon to schedule list rows.
- Keep the existing non-recurring path stable and untouched where possible.

**Behavior Rules**
- The recurrence card appears only after both start and end times are valid.
- The recurrence page always reflects the latest chosen time range from the create/edit page.
- Changing the start/end time after configuring recurrence should refresh the recurrence summary if the rule is still valid.
- Invalid recurrence configurations must be blocked before save.
- Scope selection must appear every time the user edits or deletes an already recurring schedule.
- Non-recurring schedules must never show the recurrence icon.

**Verification Targets**
- Unit coverage for:
  - recurrence rule calculation across day/week/month windows
  - duration cutoff by date and by count
  - single-occurrence deletion exception
  - single-occurrence override exception
  - series split for `这一次及之后`
- UI verification for:
  - recurrence card reveal after time selection
  - create/edit navigation to recurrence page
  - summary refresh after saving recurrence settings
  - scope-choice dialog for edit and delete
  - recurrence icon rendering in schedule rows
- Debug build must succeed.

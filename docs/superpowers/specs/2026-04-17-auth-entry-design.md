# Auth Entry Design

**Goal:** Add a polished Chinese login flow with an animated welcome screen, account-password login, optional profile completion, and a launcher-based routing layer.

**Approved Scope**
- Use a new `LauncherActivity` as the app entry.
- Login uses `账号 + 密码`, not phone-first.
- Reserve fields and interfaces for future phone binding.
- After login, first-time users go to a profile setup page.
- Profile setup includes a `跳过` action.
- If skipped, generate a random nickname like `用户4827`.
- Default signature is `暂未留下签名`.
- Login screen should be visually richer than the current app, with tasteful motion and warm Chinese-app styling.

**Flow**
- App launches into `LauncherActivity`.
- If there is no local session, route to `LoginActivity`.
- If logged in and onboarding is incomplete, route to `ProfileSetupActivity`.
- If logged in and onboarding is complete or skipped, route to `MainActivity`.

**UI Direction**
- Keep the existing warm cream and orange palette.
- Add animated floating background shapes and staged content reveal on the login page.
- Use card-based forms and concise Chinese copy.
- Keep profile setup visually aligned with login, but calmer and lighter on motion.

**Data Design**
- `AuthSession`: access token, refresh token, login flag.
- `UserProfile`: account, optional phone, nickname, gender, birthday, city, signature, onboarding status, phone binding status.
- Local persistence uses `SharedPreferences` for this phase.
- Repository interfaces reserve remote login and profile submission hooks.

**Behavior Rules**
- Account/password fields validate locally.
- Login currently uses a mock success path behind a repository boundary.
- Profile completion is optional.
- Skip writes generated nickname and default signature.
- Phone is optional now and remains available for future binding.

**Verification Targets**
- Unit tests for launcher routing, login validation, and skip-default profile creation.
- Debug build must succeed.

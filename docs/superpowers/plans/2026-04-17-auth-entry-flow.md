# Auth Entry Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a launcher-routed authentication flow with a polished login page and an optional profile setup page.

**Architecture:** Introduce a lightweight local auth/profile layer backed by `SharedPreferences`, then route startup through a new launcher activity. Build dedicated login and profile setup activities that feed into the existing `MainActivity`, while keeping remote auth/profile calls behind repository interfaces.

**Tech Stack:** Android Java, AppCompat, Material 3, SharedPreferences, ViewBinding, JUnit4

---

### Task 1: Add auth models and local storage

**Files:**
- Create: `app/src/main/java/com/example/calendar/domain/model/AuthSession.java`
- Create: `app/src/main/java/com/example/calendar/domain/model/UserProfile.java`
- Create: `app/src/main/java/com/example/calendar/data/repository/AuthRepository.java`
- Create: `app/src/main/java/com/example/calendar/data/repository/ProfileRepository.java`
- Create: `app/src/main/java/com/example/calendar/data/repository/LocalAuthRepository.java`
- Create: `app/src/main/java/com/example/calendar/data/repository/LocalProfileRepository.java`
- Modify: `app/src/main/java/com/example/calendar/common/constants/AppConstants.java`

- [ ] Add session/profile models and repository contracts.
- [ ] Implement `SharedPreferences` storage for session and profile.
- [ ] Add constants for auth preferences and default profile values.

### Task 2: Add launcher routing

**Files:**
- Create: `app/src/main/java/com/example/calendar/ui/launcher/LauncherActivity.java`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] Make `LauncherActivity` the launcher entry.
- [ ] Route to login, profile setup, or home based on local session/profile state.

### Task 3: Add login screen

**Files:**
- Create: `app/src/main/java/com/example/calendar/ui/auth/LoginActivity.java`
- Create: `app/src/main/java/com/example/calendar/ui/auth/LoginViewModel.java`
- Create: `app/src/main/java/com/example/calendar/ui/auth/LoginViewModelFactory.java`
- Create: `app/src/main/res/layout/activity_login.xml`
- Create: `app/src/main/res/drawable/bg_login_hero.xml`
- Create: `app/src/main/res/drawable/bg_login_card.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/colors.xml`

- [ ] Build an animated welcome/login layout.
- [ ] Validate account/password input.
- [ ] Save mock session through repository boundary.
- [ ] Route successful login to profile setup or home.

### Task 4: Add profile setup screen

**Files:**
- Create: `app/src/main/java/com/example/calendar/ui/profile/ProfileSetupActivity.java`
- Create: `app/src/main/java/com/example/calendar/ui/profile/ProfileSetupViewModel.java`
- Create: `app/src/main/java/com/example/calendar/ui/profile/ProfileSetupViewModelFactory.java`
- Create: `app/src/main/res/layout/activity_profile_setup.xml`
- Create: `app/src/main/res/drawable/bg_profile_header.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] Build the profile completion UI with `完成` and `跳过`.
- [ ] Save entered profile locally.
- [ ] On skip, generate a nickname and default signature.
- [ ] Mark onboarding handled and route to home.

### Task 5: Add tests and verify

**Files:**
- Create: `app/src/test/java/com/example/calendar/ui/launcher/LauncherRouteResolverTest.java`
- Create: `app/src/test/java/com/example/calendar/ui/auth/LoginViewModelTest.java`
- Create: `app/src/test/java/com/example/calendar/ui/profile/ProfileSetupViewModelTest.java`

- [ ] Add tests for launcher routing.
- [ ] Add tests for login validation and success flow.
- [ ] Add tests for skip profile defaults.
- [ ] Run `.\gradlew.bat testDebugUnitTest`.
- [ ] Run `.\gradlew.bat assembleDebug`.

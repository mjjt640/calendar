# Android Java Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a minimal Android Java project skeleton for the calendar app using a single `app` module, MVVM-oriented package structure, and a home screen entry point.

**Architecture:** The project will use a standard Android application module with Java source sets, Material-based theme resources, and a thin MVVM starter structure. The first test-driven slice will cover a tiny `HomeViewModel` default state so the scaffold includes a working test target instead of only empty directories.

**Tech Stack:** Android Gradle Plugin, Gradle Wrapper-compatible structure, Java, AndroidX AppCompat, Material Components, Lifecycle, RecyclerView, Room, WorkManager, Gson, JUnit

---

### Task 1: Create project build skeleton

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `app/build.gradle`
- Create: `app/proguard-rules.pro`

- [ ] Add project-level Gradle settings for a single `app` module.
- [ ] Add Android and Java dependency declarations needed for the initial scaffold.
- [ ] Keep compile targets modern and practical for local development.

### Task 2: Add test-first starter slice

**Files:**
- Create: `app/src/test/java/com/example/calendar/ui/home/HomeViewModelTest.java`
- Create: `app/src/main/java/com/example/calendar/ui/home/HomeViewModel.java`

- [ ] Write a failing test asserting the home screen exposes a default title.
- [ ] Implement the smallest `HomeViewModel` needed to satisfy that test.

### Task 3: Add app entry point and core package layout

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/example/calendar/App.java`
- Create: `app/src/main/java/com/example/calendar/ui/MainActivity.java`
- Create: `app/src/main/java/com/example/calendar/ui/home/HomeFragment.java`
- Create: `app/src/main/java/com/example/calendar/common/constants/AppConstants.java`
- Create: `app/src/main/java/com/example/calendar/threading/AppExecutors.java`

- [ ] Add the application class and manifest registration.
- [ ] Add a single activity host and home fragment.
- [ ] Add minimal common and threading starter files so the package structure matches the approved design.

### Task 4: Add starter resources

**Files:**
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/layout/fragment_home.xml`
- Create: `app/src/main/res/layout/item_today_schedule.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values-night/themes.xml`
- Create: `app/src/main/res/values/dimens.xml`
- Create: `app/src/main/res/drawable/bg_schedule_card.xml`
- Create: `app/src/main/res/xml/backup_rules.xml`
- Create: `app/src/main/res/xml/data_extraction_rules.xml`

- [ ] Add a minimal but coherent visual shell for the home page.
- [ ] Include day list placeholders so later schedule data can plug in without layout rewrites.

### Task 5: Verify scaffold completeness

**Files:**
- Verify: `README.md`

- [ ] Check the generated file tree for missing essentials.
- [ ] Summarize how to open and extend the project next.

# Discipline — Native Android Habit & Performance Tracker

> **Tagline:** *See your discipline, not just your habits.*

Discipline is a professional, production-oriented native Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3**, powered by **Supabase (PostgreSQL + RLS)** and **Room Database**.

It measures your **actual consistency and behavior** over time using an exact 8-stage color gradient calendar, testable scoring engine, honest streak calculations, and native reminders.

---

## Architecture & Technology Stack

- **UI Framework:** 100% Native Jetpack Compose + Material 3 Design System
- **Language:** Kotlin 2.x
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern + Coroutines & Flow
- **Local Persistence:** Android Room Database with offline-first synchronization queue
- **Cloud Backend:** **Supabase** (PostgreSQL) with strict **Row Level Security (RLS)**
- **Authentication:** **Supabase Auth** (Email/Password, recovery, profile, account deletion)
- **Local Preferences:** Jetpack DataStore Preferences (Light, Dark, and System Theme switching)
- **Reminders & Notifications:** Native `AlarmManager` + `BroadcastReceiver` + Notification Channels with auto-reschedule on device boot (`RECEIVE_BOOT_COMPLETED`)
- **CI/CD Cloud Build:** **CodeMagic** (`codemagic.yaml`) for building automated Debug and Release APKs without requiring Android Studio
- **Code Editing:** Fully compatible with **Replit** and Git/GitHub

---

## Repository Structure

```
DisciplineApp/
├── .replit                                   # Replit workspace configuration
├── replit.nix                                # Replit dependencies (JDK 17, Gradle, Git)
├── codemagic.yaml                            # CodeMagic CI/CD automated build pipeline
├── supabase_schema.sql                       # Complete PostgreSQL schema & strict RLS policies
├── SUPABASE_SETUP.md                         # Supabase step-by-step guide
├── README.md                                 # This file
├── gradle/
│   ├── libs.versions.toml                    # Version catalog (Compose BOM, Room, OkHttp, Coroutines)
│   └── wrapper/                              # Gradle Wrapper binaries and properties
├── app/
│   ├── build.gradle.kts                      # Android module build file
│   ├── proguard-rules.pro                    # R8 / ProGuard optimization rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml           # Manifest permissions and components
│       │   ├── res/values/                   # Colors, strings, themes
│       │   └── java/com/discipline/app/      # Kotlin source code (MVVM architecture)
│       └── test/java/com/discipline/app/     # Automated unit test suite
```

---

## Discipline Scoring Engine & Performance Colors

Daily Score Formula:
$$\text{Daily Score} = \left(\frac{\text{Completed Scheduled Habits}}{\text{Active Scheduled Habits}}\right) \times 100$$

If no habits are scheduled for a given day, the score is `null` (not applicable), displayed in **neutral gray (`#6B7280`)** rather than an artificial 0% or 100%.

### Invariant Performance Colors:

| Daily Score | Color Name | Hex Code | Purpose |
| :--- | :--- | :--- | :--- |
| **100%** | Dark Green | `#064E3B` | All scheduled habits completed |
| **80–99%** | Green | `#10B981` | High consistency (exceeds default streak threshold) |
| **60–79%** | Lime Green | `#84CC16` | Moderate consistency |
| **40–59%** | Amber | `#F59E0B` | Partial completion |
| **20–39%** | Orange | `#F97316` | Low completion |
| **1–19%** | Red | `#EF4444` | Minimal completion |
| **0%** | Dark Red | `#7F1D1D` | Scheduled habits present, none completed |
| **No scheduled habits** | Neutral Gray | `#6B7280` | Rest day / no active scheduled habits |

---

## Cloud Build & Deployment Workflow (Without Android Studio)

### Step 1: Store in GitHub
1. Create a new GitHub repository (e.g. `discipline-android`).
2. Push this project code into the repository.

### Step 2: Edit & Manage in Replit
1. Open Replit ([https://replit.com](https://replit.com)).
2. Click **Create Repl > Import from GitHub**.
3. Select your repository. Replit will automatically read `.replit` and `replit.nix`.
4. You can edit files, run unit tests, and manage Git commits directly in your browser.

### Step 3: Build APK on CodeMagic
1. Open CodeMagic ([https://codemagic.io](https://codemagic.io)) and sign in with GitHub.
2. Click **Add application** and select your GitHub repository.
3. CodeMagic will automatically detect the [`codemagic.yaml`](file:///C:/Users/SMZ/.gemini/antigravity/scratch/DisciplineApp/codemagic.yaml) file.
4. (Optional) Set environment variables under **App settings > Environment variables**:
   - `SUPABASE_URL` = `https://your-project.supabase.co`
   - `SUPABASE_ANON_KEY` = `your-anon-key`
5. Click **Start new build** and select `discipline-debug-apk`.
6. Once the build completes (typically 3–5 minutes), download the ready-to-install `app-debug.apk` directly from the build artifacts!

---

## Supabase PostgreSQL & Row Level Security (RLS)

1. Open your Supabase project dashboard at [https://supabase.com](https://supabase.com).
2. Go to the **SQL Editor** and execute the script in [`supabase_schema.sql`](file:///C:/Users/SMZ/.gemini/antigravity/scratch/DisciplineApp/supabase_schema.sql).
3. The schema sets up:
   - `public.habits`
   - `public.daily_logs`
   - `public.reflections`
   - Row Level Security (RLS) policies guaranteeing that authenticated users can only query, insert, update, or delete their own data (`auth.uid() = user_id`).
   - Cascade deletion if an account is deleted.
4. See [`SUPABASE_SETUP.md`](file:///C:/Users/SMZ/.gemini/antigravity/scratch/DisciplineApp/SUPABASE_SETUP.md) for detailed configuration steps.

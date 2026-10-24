# Supabase Setup Guide for Discipline

Discipline uses **Supabase** for user authentication and cloud synchronization powered by PostgreSQL with Row Level Security (RLS).

---

## 1. Create a Free Supabase Project
1. Go to [https://supabase.com](https://supabase.com) and sign in or create an account.
2. Click **New Project**.
3. Choose an organization, enter a project name (e.g., `Discipline`), set a database password, and choose your preferred cloud region.
4. Click **Create new project**.

---

## 2. Run the Database Schema & RLS Script
1. In your Supabase dashboard, open the **SQL Editor** from the left navigation menu:
   `https://supabase.com/dashboard/project/_/sql`
2. Open the file [`supabase_schema.sql`](file:///C:/Users/SMZ/.gemini/antigravity/scratch/DisciplineApp/supabase_schema.sql) included in the project root.
3. Copy its entire contents and paste it into the Supabase SQL Editor.
4. Click **Run** (green button).
5. Verify that:
   - Three tables are created: `public.habits`, `public.daily_logs`, and `public.reflections`.
   - **Row Level Security (RLS) is enabled** on each table so each user can only read and write their own records.
   - The secure function `public.delete_user_account()` is created for safe server-side account deletion without needing service-role keys.

---

## 3. Retrieve Your Project URL & Anon Key
1. Go to **Project Settings** (gear icon in the bottom left).
2. Select **API** under Configuration:
   - **Project URL:** e.g., `https://abcdefghijklm.supabase.co`
   - **Project API Keys > `anon` `public`:** e.g., `eyJhbGciOiJIUzI1NiIsInR5cCI...`
3. ⚠️ **SECURITY GUARANTEE:**
   - **DO NOT** use the `service_role` secret key.
   - Only use the `anon` public key in the mobile application.
   - Row Level Security (RLS) ensures that even with the public `anon` key, users can **only** read and modify their own records when authenticated.
   - The app never embeds passwords or administrative tokens.

---

## 4. Enabling Email/Password Authentication
1. In the Supabase dashboard, go to **Authentication > Providers**.
2. Ensure **Email** is turned **ON**.
3. (Optional for quick development) Under **Authentication > URL Configuration**, you can disable email confirmation if you want users to log in immediately upon registration without email verification.

---

## 5. Configuring Supabase in CodeMagic or the App
You have three simple ways to supply your Supabase URL and Anon Key:

### Option A: In CodeMagic Environment Variables (Recommended for CI/CD)
In CodeMagic, go to your app settings > **Environment variables**:
- `SUPABASE_URL` = `https://your-project.supabase.co`
- `SUPABASE_ANON_KEY` = `your-anon-key`

The build script will inject these into `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY`.

### Option B: Directly in the App via Settings
When you launch the app, open **Settings > Cloud Sync (Supabase)**. You can enter or update your Supabase URL and Anon Key directly in the UI.

### Option C: In `app/build.gradle.kts`
You can set default values in `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "SUPABASE_URL", "\"https://your-project.supabase.co\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"your-anon-key\"")
```

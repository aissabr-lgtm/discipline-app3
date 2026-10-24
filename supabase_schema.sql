-- ==============================================================================
-- DISCIPLINE APP — SUPABASE POSTGRESQL SCHEMA WITH ROW LEVEL SECURITY (RLS)
-- ==============================================================================
-- This script creates the required database tables, indexes, and strict Row Level
-- Security policies for the Discipline habit tracker.
--
-- How to apply:
-- 1. Open your Supabase Dashboard: https://supabase.com/dashboard/project/_/sql
-- 2. Paste this entire file into the SQL Editor.
-- 3. Click "Run".
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. HABITS TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.habits (
    id TEXT PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    category TEXT DEFAULT 'PERSONAL',
    custom_category TEXT,
    type TEXT DEFAULT 'YES_NO', -- 'YES_NO' or 'MEASURABLE'
    target_value DOUBLE PRECISION DEFAULT 1.0,
    unit TEXT DEFAULT '',
    scheduled_days_csv TEXT DEFAULT '1,2,3,4,5,6,7', -- 1=Mon .. 7=Sun
    reminder_hour INTEGER,
    reminder_minute INTEGER,
    is_paused BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

-- Indexes for habits
CREATE INDEX IF NOT EXISTS idx_habits_user_id ON public.habits(user_id);
CREATE INDEX IF NOT EXISTS idx_habits_updated_at ON public.habits(updated_at);

-- ------------------------------------------------------------------------------
-- 2. DAILY LOGS TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.daily_logs (
    id TEXT PRIMARY KEY, -- Composite key: habit_id || '_' || date
    habit_id TEXT REFERENCES public.habits(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    date TEXT NOT NULL, -- Format: YYYY-MM-DD
    progress_value DOUBLE PRECISION DEFAULT 0.0,
    is_completed BOOLEAN DEFAULT FALSE,
    updated_at BIGINT NOT NULL
);

-- Indexes for daily logs
CREATE INDEX IF NOT EXISTS idx_daily_logs_user_date ON public.daily_logs(user_id, date);
CREATE INDEX IF NOT EXISTS idx_daily_logs_habit_id ON public.daily_logs(habit_id);

-- ------------------------------------------------------------------------------
-- 3. DAILY REFLECTIONS TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.reflections (
    id TEXT PRIMARY KEY, -- Composite key: user_id || '_' || date
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    date TEXT NOT NULL, -- Format: YYYY-MM-DD
    reflection_text TEXT DEFAULT '',
    mood_rating INTEGER DEFAULT 0, -- 0=unrated, 1..5
    updated_at BIGINT NOT NULL
);

-- Indexes for reflections
CREATE INDEX IF NOT EXISTS idx_reflections_user_date ON public.reflections(user_id, date);

-- ------------------------------------------------------------------------------
-- 4. ROW LEVEL SECURITY (RLS) POLICIES
-- Strict Isolation: Each authenticated user can ONLY access and modify their own records.
-- Public / unauthenticated access is strictly blocked.
-- ------------------------------------------------------------------------------

-- Enable RLS on all tables
ALTER TABLE public.habits ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reflections ENABLE ROW LEVEL SECURITY;

-- HABITS POLICIES
DROP POLICY IF EXISTS "Users can view their own habits" ON public.habits;
CREATE POLICY "Users can view their own habits"
    ON public.habits FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can create their own habits" ON public.habits;
CREATE POLICY "Users can create their own habits"
    ON public.habits FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update their own habits" ON public.habits;
CREATE POLICY "Users can update their own habits"
    ON public.habits FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete their own habits" ON public.habits;
CREATE POLICY "Users can delete their own habits"
    ON public.habits FOR DELETE
    USING (auth.uid() = user_id);

-- DAILY LOGS POLICIES
DROP POLICY IF EXISTS "Users can view their own daily logs" ON public.daily_logs;
CREATE POLICY "Users can view their own daily logs"
    ON public.daily_logs FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert their own daily logs" ON public.daily_logs;
CREATE POLICY "Users can insert their own daily logs"
    ON public.daily_logs FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update their own daily logs" ON public.daily_logs;
CREATE POLICY "Users can update their own daily logs"
    ON public.daily_logs FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete their own daily logs" ON public.daily_logs;
CREATE POLICY "Users can delete their own daily logs"
    ON public.daily_logs FOR DELETE
    USING (auth.uid() = user_id);

-- REFLECTIONS POLICIES
DROP POLICY IF EXISTS "Users can view their own reflections" ON public.reflections;
CREATE POLICY "Users can view their own reflections"
    ON public.reflections FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert their own reflections" ON public.reflections;
CREATE POLICY "Users can insert their own reflections"
    ON public.reflections FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update their own reflections" ON public.reflections;
CREATE POLICY "Users can update their own reflections"
    ON public.reflections FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete their own reflections" ON public.reflections;
CREATE POLICY "Users can delete their own reflections"
    ON public.reflections FOR DELETE
    USING (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 5. SECURE SERVER-SIDE ACCOUNT DELETION FUNCTION (RPC)
-- Allows an authenticated user to delete their own Supabase Auth account.
-- Runs with SECURITY DEFINER so it can remove the user from auth.users,
-- but strictly verified to match auth.uid().
-- Foreign key ON DELETE CASCADE automatically cleans up habits, logs, and reflections.
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.delete_user_account()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    DELETE FROM auth.users WHERE id = auth.uid();
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Authenticated user account was not found';
    END IF;
END;
$$;

-- Functions are executable by PUBLIC by default in PostgreSQL, so revoke that
-- default explicitly and grant only to authenticated users.
REVOKE ALL ON FUNCTION public.delete_user_account() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.delete_user_account() FROM anon;
GRANT EXECUTE ON FUNCTION public.delete_user_account() TO authenticated;

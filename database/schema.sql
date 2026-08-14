-- ============================================
-- RELEAF APP - SUPABASE DATABASE SCHEMA
-- Run this in Supabase SQL Editor (https://app.supabase.com > SQL Editor)
-- ============================================

-- 1. PROFILES (extends Supabase auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT DEFAULT '',
    email TEXT DEFAULT '',
    phone TEXT DEFAULT '',
    title TEXT DEFAULT 'Gardener',
    avatar_url TEXT DEFAULT '',
    total_points INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Auto-create profile on user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, name)
    VALUES (NEW.id, NEW.email, COALESCE(NEW.raw_user_meta_data->>'name', ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================
-- 2. POIS (Points of Interest - toilets / trash cans)
-- ============================================
CREATE TABLE IF NOT EXISTS public.pois (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'TOILET' CHECK (category IN ('TOILET', 'TRASH_CAN')),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    rating DOUBLE PRECISION DEFAULT 0.0,
    cleanliness TEXT DEFAULT 'AVERAGE' CHECK (cleanliness IN ('CLEAN', 'AVERAGE', 'DIRTY')),
    is_paid BOOLEAN DEFAULT false,
    is_verified BOOLEAN DEFAULT false,
    verification_count INT DEFAULT 0,
    report_count INT DEFAULT 0,
    created_by UUID REFERENCES auth.users(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- 3. POI PHOTOS
-- ============================================
CREATE TABLE IF NOT EXISTS public.poi_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poi_id UUID NOT NULL REFERENCES public.pois(id) ON DELETE CASCADE,
    photo_url TEXT NOT NULL,
    uploaded_by UUID REFERENCES auth.users(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- 4. POI VERIFICATIONS (track verify / report actions)
-- ============================================
CREATE TABLE IF NOT EXISTS public.poi_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poi_id UUID NOT NULL REFERENCES public.pois(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id),
    action TEXT NOT NULL CHECK (action IN ('VERIFY', 'REPORT')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(poi_id, user_id, action)
);

-- ============================================
-- 5. REVIEWS (comments on POIs)
-- ============================================
CREATE TABLE IF NOT EXISTS public.reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poi_id UUID NOT NULL REFERENCES public.pois(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id),
    star_rating INT NOT NULL CHECK (star_rating >= 1 AND star_rating <= 5),
    text TEXT DEFAULT '',
    like_count INT DEFAULT 0,
    dislike_count INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- 6. GARDENS (per-user garden progress)
-- ============================================
CREATE TABLE IF NOT EXISTS public.gardens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    current_exp INT DEFAULT 0,
    exp_target INT DEFAULT 1000,
    grow_uses_left INT DEFAULT 1,
    grow_uses_max INT DEFAULT 1,
    fertilize_uses_left INT DEFAULT 1,
    fertilize_uses_max INT DEFAULT 1,
    current_points INT DEFAULT 0,
    current_gems INT DEFAULT 0,
    points_target INT DEFAULT 100,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- 7. PLANT SLOTS (6 slots per user garden)
-- ============================================
CREATE TABLE IF NOT EXISTS public.plant_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    slot_index INT NOT NULL CHECK (slot_index >= 1 AND slot_index <= 6),
    state TEXT DEFAULT 'EMPTY_POT' CHECK (state IN ('LOCKED', 'EMPTY_POT', 'GROWING', 'FULLY_GROWN')),
    plant_type TEXT,
    planted_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, slot_index)
);

-- ============================================
-- 8. QUESTS (quest definitions)
-- ============================================
CREATE TABLE IF NOT EXISTS public.quests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    reward_label TEXT DEFAULT 'Points',
    reward_count INT DEFAULT 10,
    progress_target INT DEFAULT 10,
    difficulty TEXT DEFAULT 'EASY',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- 9. USER QUESTS (per-user quest progress)
-- ============================================
CREATE TABLE IF NOT EXISTS public.user_quests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    quest_id UUID NOT NULL REFERENCES public.quests(id) ON DELETE CASCADE,
    progress_current INT DEFAULT 0,
    status TEXT DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'CLAIMABLE', 'CLAIMED')),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, quest_id)
);

-- ============================================
-- 10. REWARD TIERS
-- ============================================
CREATE TABLE IF NOT EXISTS public.reward_tiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_points INT NOT NULL,
    reward_description TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- 11. USER REWARDS (unlocked tiers)
-- ============================================
CREATE TABLE IF NOT EXISTS public.user_rewards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    tier_id UUID NOT NULL REFERENCES public.reward_tiers(id) ON DELETE CASCADE,
    unlocked_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, tier_id)
);

-- ============================================
-- 12. ACHIEVEMENTS
-- ============================================
CREATE TABLE IF NOT EXISTS public.achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label TEXT NOT NULL,
    description TEXT DEFAULT '',
    icon_url TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- 13. USER ACHIEVEMENTS
-- ============================================
CREATE TABLE IF NOT EXISTS public.user_achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    achievement_id UUID NOT NULL REFERENCES public.achievements(id) ON DELETE CASCADE,
    earned_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, achievement_id)
);

-- ============================================
-- SEED DATA: Sample quests
-- ============================================
INSERT INTO public.quests (title, description, reward_label, reward_count, progress_target, difficulty)
VALUES
    -- EASY QUESTS (50-100 Points, 1-2 Gems)
    ('Morning Check-in', 'Open the Releaf app today', 'Points', 50, 1, 'EASY'),
    ('First Review', 'Write your first toilet review', 'Points', 80, 1, 'EASY'),
    ('Toilet Scout', 'Verify a toilet location', 'Points', 60, 1, 'EASY'),
    ('Cleanliness Watch', 'Rate the cleanliness of 1 toilet', 'Gems', 1, 1, 'EASY'),

    -- MEDIUM QUESTS (150-250 Points, 3-5 Gems)
    ('Active Contributor', 'Write 3 reviews for different toilets', 'Points', 200, 3, 'MEDIUM'),
    ('Local Guide', 'Verify 3 toilet locations', 'Points', 180, 3, 'MEDIUM'),
    ('Photographer', 'Upload 2 photos of facilities', 'Gems', 3, 2, 'MEDIUM'),
    ('Garden Helper', 'Fertilize 2 plants in your garden', 'Points', 220, 2, 'MEDIUM'),

    -- HARD QUESTS (400-600 Points, 10-15 Gems)
    ('Releaf Master', 'Visit and review 5 different locations', 'Points', 500, 5, 'HARD'),
    ('Expert Scout', 'Verify 10 facilities in a day', 'Gems', 12, 10, 'HARD'),
    ('Master Gardener', 'Harvest 5 fully grown plants', 'Points', 600, 5, 'HARD'),
    ('Community Pillar', 'Get 10 likes on your reviews', 'Gems', 15, 10, 'HARD')
ON CONFLICT DO NOTHING;

-- ============================================
-- SEED DATA: Reward tiers
-- ============================================
INSERT INTO public.reward_tiers (target_points, reward_description)
VALUES
    (500, 'Bronze Gardener Badge'),
    (2000, 'Silver Gardener Badge'),
    (10000, 'Gold Gardener Badge')
ON CONFLICT DO NOTHING;

-- ============================================
-- SEED DATA: Achievements
-- ============================================
INSERT INTO public.achievements (label, description)
VALUES
    ('Expert Reviewer', 'Write 10 or more reviews'),
    ('Expert Gardener', 'Grow 50 plants to full bloom'),
    ('Expert Navigator', 'Visit 20 different locations'),
    ('Early Adopter', 'Joined during the first month'),
    ('Toilet Scout', 'Verified 5 new toilets'),
    ('Clean Crusader', 'Reported 10 dirty toilets'),
    ('Photo Fanatic', 'Uploaded 20 photos'),
    ('Social Butterfly', 'Liked 50 reviews')
ON CONFLICT DO NOTHING;

-- ============================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- Allow authenticated users to access their own data
-- ============================================

-- Profiles: users can read any profile, update only their own
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read any profile" ON public.profiles FOR SELECT TO authenticated USING (true);
CREATE POLICY "Users can update own profile" ON public.profiles FOR UPDATE TO authenticated USING (auth.uid() = id);

-- POIs: authenticated users can read, create
ALTER TABLE public.pois ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read POIs" ON public.pois FOR SELECT TO authenticated USING (true);
CREATE POLICY "Anyone can create POIs" ON public.pois FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Anyone can update POIs" ON public.pois FOR UPDATE TO authenticated USING (true);
CREATE POLICY "Anyone can delete POIs" ON public.pois FOR DELETE TO authenticated USING (true);

-- POI Photos: anyone can read, create
ALTER TABLE public.poi_photos ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read photos" ON public.poi_photos FOR SELECT TO authenticated USING (true);
CREATE POLICY "Anyone can create photos" ON public.poi_photos FOR INSERT TO authenticated WITH CHECK (true);

-- POI Verifications: anyone can read, create
ALTER TABLE public.poi_verifications ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read verifications" ON public.poi_verifications FOR SELECT TO authenticated USING (true);
CREATE POLICY "Anyone can create verifications" ON public.poi_verifications FOR INSERT TO authenticated WITH CHECK (true);

-- Reviews: anyone can read, create, update (for likes)
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read reviews" ON public.reviews FOR SELECT TO authenticated USING (true);
CREATE POLICY "Anyone can create reviews" ON public.reviews FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Anyone can update reviews" ON public.reviews FOR UPDATE TO authenticated USING (true);

-- Gardens: users can read/update only their own garden
ALTER TABLE public.gardens ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read own garden" ON public.gardens FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users can update own garden" ON public.gardens FOR UPDATE TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users can create own garden" ON public.gardens FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

-- Plant Slots: users can read/update only their own
ALTER TABLE public.plant_slots ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read own slots" ON public.plant_slots FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users can update own slots" ON public.plant_slots FOR UPDATE TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users can create own slots" ON public.plant_slots FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

-- Quests: anyone can read
ALTER TABLE public.quests ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read quests" ON public.quests FOR SELECT TO authenticated USING (true);

-- User Quests: users can read/update only their own
ALTER TABLE public.user_quests ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read own quests" ON public.user_quests FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users can update own quests" ON public.user_quests FOR UPDATE TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users can create own quests" ON public.user_quests FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

-- Reward Tiers: anyone can read
ALTER TABLE public.reward_tiers ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read tiers" ON public.reward_tiers FOR SELECT TO authenticated USING (true);

-- User Rewards: users can read/create only their own
ALTER TABLE public.user_rewards ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read own rewards" ON public.user_rewards FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users can create own rewards" ON public.user_rewards FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

-- Achievements: anyone can read
ALTER TABLE public.achievements ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read achievements" ON public.achievements FOR SELECT TO authenticated USING (true);

-- User Achievements: users can read only their own
ALTER TABLE public.user_achievements ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read own achievements" ON public.user_achievements FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users can create own achievements" ON public.user_achievements FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

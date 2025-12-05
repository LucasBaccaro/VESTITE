-- VESTITE Database Schema
-- Execute this in Supabase SQL Editor

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Perfiles (Foto Base del Usuario)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_body_image_url TEXT, -- Nullable: el usuario la sube después
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable Row Level Security
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- RLS Policies for profiles
CREATE POLICY "Users can view their own profile"
    ON profiles FOR SELECT
    USING (auth.uid() = id);

CREATE POLICY "Users can insert their own profile"
    ON profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
    ON profiles FOR UPDATE
    USING (auth.uid() = id);

-- Function: Crear perfil automáticamente cuando se registra un usuario
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, full_body_image_url)
    VALUES (NEW.id, NULL);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger: Ejecutar función después de INSERT en auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();

-- 2. Categorías de Prendas (Estáticas)
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slug TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS (read-only for all authenticated users)
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Categories are viewable by authenticated users"
    ON categories FOR SELECT
    TO authenticated
    USING (true);

-- Insert default categories
INSERT INTO categories (slug, display_name) VALUES
    ('upper', 'Prendas Superiores'),
    ('lower', 'Prendas Inferiores'),
    ('footwear', 'Calzado')
ON CONFLICT (slug) DO NOTHING;

-- 3. Prendas (Con metadatos generados por IA)
CREATE TABLE IF NOT EXISTS garments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,

    -- Storage
    image_url TEXT NOT NULL,

    -- AI-generated metadata (by Gemini Flash)
    ai_description TEXT,
    ai_fit TEXT DEFAULT 'regular',

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS
ALTER TABLE garments ENABLE ROW LEVEL SECURITY;

-- RLS Policies for garments
CREATE POLICY "Users can view their own garments"
    ON garments FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own garments"
    ON garments FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own garments"
    ON garments FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own garments"
    ON garments FOR DELETE
    USING (auth.uid() = user_id);

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_garments_user_id ON garments(user_id);
CREATE INDEX IF NOT EXISTS idx_garments_category_id ON garments(category_id);

-- 4. Outfits Generados (Resultados del Try-On)
CREATE TABLE IF NOT EXISTS outfits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Generated image
    generated_image_url TEXT NOT NULL,

    -- Optional metadata
    occasion TEXT,

    -- Garments used (optional foreign keys)
    upper_garment_id UUID REFERENCES garments(id) ON DELETE SET NULL,
    lower_garment_id UUID REFERENCES garments(id) ON DELETE SET NULL,
    footwear_garment_id UUID REFERENCES garments(id) ON DELETE SET NULL,

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS
ALTER TABLE outfits ENABLE ROW LEVEL SECURITY;

-- RLS Policies for outfits
CREATE POLICY "Users can view their own outfits"
    ON outfits FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own outfits"
    ON outfits FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own outfits"
    ON outfits FOR DELETE
    USING (auth.uid() = user_id);

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_outfits_user_id ON outfits(user_id);

-- 5. Storage Buckets Setup
-- Run these commands in Supabase Storage section:
-- Bucket: 'avatars' (public)
-- Bucket: 'garments' (public or private based on preference)
-- Bucket: 'outfits' (private)

-- 6. Updated timestamp trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_garments_updated_at
    BEFORE UPDATE ON garments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- El onboarding se puede saltar: la app deja de mostrarlo al entrar, pero el perfil sigue
-- incompleto (onboarding_completed = false) y Perfil invita a completarlo.
ALTER TABLE user_profiles
    ADD COLUMN onboarding_skipped_at timestamptz;

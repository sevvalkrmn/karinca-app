-- Karınca — seed default (system) categories.
--
-- user_id = null rows are visible to every authenticated user via the
-- categories_select_own_or_default RLS policy, but no insert/update policy
-- permits user_id = null — these rows can only be written by the service
-- role (i.e. via this migration), never by a client. Clients pick them up
-- through the normal pullCategories() sync cycle.
--
-- Fixed UUIDs + `on conflict (id) do nothing` make this migration safe to
-- re-run. updated_at is pinned to the same instant as created_at so every
-- default row shares one sync-cursor timestamp.

insert into public.categories (id, user_id, kind, name, color, icon, created_at, updated_at)
values
    ('a92eeb80-472f-48df-ad67-6f69c398034b', null, 'expense', 'Market',      '#FB923C', 'shopping_cart',    now(), now()),
    ('51ac87e1-ef0c-4b4f-99e7-91d76a1c4df0', null, 'expense', 'Yemek',       '#B45309', 'coffee',           now(), now()),
    ('e23f4fa8-d660-4252-8b52-4825c0c05cd2', null, 'expense', 'Ulaşım',      '#3B82F6', 'directions_bus',   now(), now()),
    ('647ea376-91fd-4831-94fe-e2bdd185ade3', null, 'expense', 'Faturalar',   '#6366F1', 'receipt_long',     now(), now()),
    ('da8d9c3c-8a3d-4cb9-be93-c2b217048ca0', null, 'expense', 'Kira',        '#0EA5E9', 'home',             now(), now()),
    ('3611c8b7-c72f-4b8e-a781-ba3c717bff56', null, 'expense', 'Eğlence',     '#A855F7', 'sports_esports',   now(), now()),
    ('16d90c79-135d-4014-87c4-c715dda42e4e', null, 'expense', 'Alışveriş',   '#EC4899', 'shopping_bag',     now(), now()),
    ('80f0f9ae-cfef-4ba9-a180-24b93f13df61', null, 'expense', 'Sağlık',      '#14B8A6', 'medical_services', now(), now()),
    ('63a2e5f3-4c60-47c7-b288-eb6172964a48', null, 'expense', 'Abonelik',    '#8B5CF6', 'autorenew',        now(), now()),
    ('21191fa1-212e-48c6-8b85-5cc869afb12f', null, 'expense', 'Eğitim',      '#06B6D4', 'school',           now(), now()),
    ('9e401573-b7e3-452d-9924-49b8b5b87bfe', null, 'expense', 'Diğer',       '#78716C', 'more_horiz',       now(), now()),
    ('f7a0e280-296e-4b37-876a-89611afea4be', null, 'income',  'Maaş/Burs',   '#10B981', 'payments',         now(), now()),
    ('7ef98393-96aa-4e78-9a74-1908460e027e', null, 'income',  'Ek Gelir',    '#10B981', 'trending_up',      now(), now()),
    ('d46090e7-1189-4ce8-a390-e6ad0d35a5bb', null, 'income',  'Diğer Gelir', '#10B981', 'more_horiz',       now(), now())
on conflict (id) do nothing;

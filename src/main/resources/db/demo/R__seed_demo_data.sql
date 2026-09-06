-- Local/test fixture only. This location is deliberately excluded from the prod profile.
-- All five users use the documented demo-only password: OpsPulseDemo!2026
INSERT INTO users (
    id, email, password_hash, full_name, active, created_at, created_by, updated_at, updated_by
) VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'admin@opspulse.demo',
        '$2a$12$h0Q5g1hIWmvuL.BmCmPz7OVQdlM/FIash75n29.Xk7xMXgNJyAJ.2',
        'Demo Administrator',
        TRUE,
        now(),
        '10000000-0000-0000-0000-000000000001',
        now(),
        '10000000-0000-0000-0000-000000000001'
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'manager@opspulse.demo',
        '$2a$12$h0Q5g1hIWmvuL.BmCmPz7OVQdlM/FIash75n29.Xk7xMXgNJyAJ.2',
        'Demo Manager',
        TRUE,
        now(),
        '10000000-0000-0000-0000-000000000002',
        now(),
        '10000000-0000-0000-0000-000000000002'
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'operator1@opspulse.demo',
        '$2a$12$h0Q5g1hIWmvuL.BmCmPz7OVQdlM/FIash75n29.Xk7xMXgNJyAJ.2',
        'Demo Operator One',
        TRUE,
        now(),
        '10000000-0000-0000-0000-000000000003',
        now(),
        '10000000-0000-0000-0000-000000000003'
    ),
    (
        '10000000-0000-0000-0000-000000000004',
        'operator2@opspulse.demo',
        '$2a$12$h0Q5g1hIWmvuL.BmCmPz7OVQdlM/FIash75n29.Xk7xMXgNJyAJ.2',
        'Demo Operator Two',
        TRUE,
        now(),
        '10000000-0000-0000-0000-000000000004',
        now(),
        '10000000-0000-0000-0000-000000000004'
    ),
    (
        '10000000-0000-0000-0000-000000000005',
        'viewer@opspulse.demo',
        '$2a$12$h0Q5g1hIWmvuL.BmCmPz7OVQdlM/FIash75n29.Xk7xMXgNJyAJ.2',
        'Demo Viewer',
        TRUE,
        now(),
        '10000000-0000-0000-0000-000000000005',
        now(),
        '10000000-0000-0000-0000-000000000005'
    )
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id) VALUES
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'),
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003'),
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000003'),
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000004')
ON CONFLICT (user_id, role_id) DO NOTHING;

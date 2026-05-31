-- Users
INSERT INTO app_user (id, first_name, last_name, email, password, role, department, status) VALUES
(1, 'John',  'Doe',   'user@example.com',  'password123', 'User',    'Operations', 'Active'),
(2, 'Jane',  'Admin', 'admin@example.com', 'admin123',    'Admin',   'IT',         'Active'),
(3, 'Bob',   'Smith', 'bob@example.com',   'bob123',      'Manager', 'HR',         'Inactive'),
(4, 'Alice', 'Lee',   'alice@example.com', 'alice123',    'User',    'Finance',    'Active');

-- Activities
INSERT INTO activity (id, name, user_name, activity_date, status, type) VALUES
(1, 'Login',           'John Doe',  '2026-05-30 09:42:00', 'Success', 'Auth'),
(2, 'Policy Updated',  'John Doe',  '2026-05-29 14:10:00', 'Info',    'Policy'),
(3, 'Export Report',   'John Doe',  '2026-05-28 11:03:00', 'Success', 'Report'),
(4, 'Password Change', 'John Doe',  '2026-05-27 16:55:00', 'Success', 'Auth'),
(5, 'Failed Login',    'Unknown',   '2026-05-26 02:14:00', 'Failed',  'Auth'),
(6, 'Account Update',  'John Doe',  '2026-05-24 10:30:00', 'Success', 'Account'),
(7, 'Policy Review',   'Jane Admin','2026-05-22 09:00:00', 'Pending', 'Policy');

-- Policies
INSERT INTO policy (id, policy_code, name, category, effective_date, expiry_date, status) VALUES
(1, 'POL-001', 'Data Retention',   'Compliance', '2026-01-01', '2026-12-31', 'Active'),
(2, 'POL-002', 'Access Control',   'Security',   '2026-01-01', '2026-12-31', 'Active'),
(3, 'POL-003', 'Remote Work',      'HR',         '2026-03-01', '2027-02-28', 'Active'),
(4, 'POL-004', 'Incident Response','Security',   '2025-07-01', '2026-06-30', 'Expiring Soon'),
(5, 'POL-005', 'Password Policy',  'Security',   '2024-01-01', '2025-12-31', 'Expired');

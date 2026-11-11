CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(120)  NOT NULL,
    email           VARCHAR(254)  NOT NULL,
    employee_id     VARCHAR(30),
    password_hash   VARCHAR(100)  NOT NULL,
    role            VARCHAR(20)   NOT NULL DEFAULT 'EMPLOYEE',
    requested_role  VARCHAR(20),
    account_status  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email       UNIQUE (email),
    CONSTRAINT uq_users_employee_id UNIQUE (employee_id),   -- NULLs are allowed multiple times
    CONSTRAINT chk_users_email_lc   CHECK (email = lower(email)),
    CONSTRAINT chk_users_role       CHECK (role IN ('EMPLOYEE', 'HR', 'MANAGER', 'ADMIN')),
    CONSTRAINT chk_users_req_role   CHECK (requested_role IS NULL OR requested_role IN ('EMPLOYEE', 'HR', 'MANAGER', 'ADMIN')),
    CONSTRAINT chk_users_status     CHECK (account_status IN ('ACTIVE', 'PENDING', 'SUSPENDED'))
);

CREATE INDEX idx_users_role ON users (role);

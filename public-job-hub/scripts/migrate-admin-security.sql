-- 관리자 판정을 설정 문자열이 아닌 DB 권한으로 전환하고,
-- AES-GCM으로 암호화된 TOTP 비밀키를 저장할 충분한 길이를 확보한다.
-- TiDB/MySQL 8에서 여러 번 실행해도 안전하다.
ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE app_users
    MODIFY COLUMN two_factor_secret VARCHAR(255) NULL;

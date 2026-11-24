-- Login con Google: la cuenta guarda el "sub" de Google (id estable de la cuenta de Google).
-- Las cuentas creadas con Google no tienen contrasena; las que ya existian pueden vincularse por email.
ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL,
    ADD COLUMN google_subject varchar(255);

ALTER TABLE users
    ADD CONSTRAINT users_google_subject_key UNIQUE (google_subject);

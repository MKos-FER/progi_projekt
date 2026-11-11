-- Obavezni podaci profila
ALTER TABLE users
    ALTER COLUMN gender SET NOT NULL,
    ALTER COLUMN date_of_birth SET NOT NULL,
    ALTER COLUMN city_id SET NOT NULL,

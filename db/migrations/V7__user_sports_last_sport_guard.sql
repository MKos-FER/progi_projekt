-- Korisnik ne smije ostati bez sportova (F-004).
-- Okidač je odgođen do COMMIT-a, pa zamjena cijelog popisa (DELETE pa INSERT u istoj transakciji) prolazi.
-- Brisanje korisnika prolazi: kaskadno obrisani sportovi pripadaju korisniku kojeg više nema.
-- Unos korisnika se ne provjerava jer registracija i ogledni podaci prvo upisuju korisnika, a zatim sportove.

CREATE FUNCTION user_sports_last_sport_guard() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    -- Zaključavanje korisnika: dvije istodobne transakcije koje brišu njegove zadnje sportove provjeravaju se redom
    PERFORM 1 FROM users WHERE id = OLD.user_id FOR NO KEY UPDATE;
    IF EXISTS (SELECT 1 FROM users WHERE id = OLD.user_id)
       AND NOT EXISTS (SELECT 1 FROM user_sports WHERE user_id = OLD.user_id) THEN
        RAISE EXCEPTION 'Korisnik % mora imati barem jedan sport.', OLD.user_id
            USING ERRCODE = 'check_violation',
                  CONSTRAINT = 'user_sports_last_sport_guard';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER user_sports_last_sport_guard
    AFTER DELETE OR UPDATE OF user_id ON user_sports
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION user_sports_last_sport_guard();

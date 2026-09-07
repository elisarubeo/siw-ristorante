-- Dati di prova.
-- Hibernate esegue questo file da solo dopo aver creato lo schema, ma solo
-- finche' ddl-auto resta "create": e' lui a ricrearlo a ogni avvio.
-- ATTENZIONE: il lettore di Hibernate tratta UNA RIGA = UNA ISTRUZIONE.
-- Spezzare un INSERT su piu' righe lo fa fallire.
-- Password in chiaro, per provare il login: admin/admin, elisa/elisa, marco/marco

-- ---------- ristoranti ----------
INSERT INTO ristorante (id, nome, indirizzo) VALUES (1, 'Osteria del Borgo', 'Via dei Coronari 12, Roma');
INSERT INTO ristorante (id, nome, indirizzo) VALUES (2, 'Trattoria da Nino', 'Piazza Trilussa 4, Roma');
INSERT INTO ristorante (id, nome, indirizzo) VALUES (3, 'Locanda Verde', 'Via Ostiense 108, Roma');

-- ---------- piatti ----------
INSERT INTO piatto (id, nome, ingredienti, prezzo, disponibile, ristorante_id) VALUES (1, 'Carbonara', 'uova, guanciale, pecorino, pepe nero', 13.50, true, 1);
INSERT INTO piatto (id, nome, ingredienti, prezzo, disponibile, ristorante_id) VALUES (2, 'Cacio e pepe', 'pecorino romano, pepe nero, tonnarelli', 12.00, true, 1);
INSERT INTO piatto (id, nome, ingredienti, prezzo, disponibile, ristorante_id) VALUES (3, 'Amatriciana', 'guanciale, pomodoro, pecorino', 12.50, true, 1);
INSERT INTO piatto (id, nome, ingredienti, prezzo, disponibile, ristorante_id) VALUES (4, 'Tiramisu', 'savoiardi, mascarpone, caffe, cacao', 6.00, true, 1);
INSERT INTO piatto (id, nome, ingredienti, prezzo, disponibile, ristorante_id) VALUES (5, 'Fritto misto', 'calamari, gamberi, zucchine', 14.00, true, 2);
INSERT INTO piatto (id, nome, ingredienti, prezzo, disponibile, ristorante_id) VALUES (6, 'Spaghetti alle vongole', 'vongole veraci, aglio, prezzemolo', 16.00, true, 2);
INSERT INTO piatto (id, nome, ingredienti, prezzo, disponibile, ristorante_id) VALUES (7, 'Tagliata di manzo', 'controfiletto, rucola, grana', 18.50, true, 2);
INSERT INTO piatto (id, nome, ingredienti, prezzo, disponibile, ristorante_id) VALUES (8, 'Vellutata di zucca', 'zucca, patate, rosmarino', 9.00, true, 3);
INSERT INTO piatto (id, nome, ingredienti, prezzo, disponibile, ristorante_id) VALUES (9, 'Risotto ai funghi', 'riso carnaroli, porcini, burro', 14.00, true, 3);
INSERT INTO piatto (id, nome, ingredienti, prezzo, disponibile, ristorante_id) VALUES (10, 'Tortino al cioccolato', 'cioccolato fondente, uova, burro', 6.50, true, 3);

-- ---------- tavoli ----------
INSERT INTO tavolo (id, numero_tavolo, numero_posti, status, ristorante_id) VALUES (1, 1, 2, 'LIBERO', 1);
INSERT INTO tavolo (id, numero_tavolo, numero_posti, status, ristorante_id) VALUES (2, 2, 4, 'LIBERO', 1);
INSERT INTO tavolo (id, numero_tavolo, numero_posti, status, ristorante_id) VALUES (3, 3, 6, 'LIBERO', 1);
INSERT INTO tavolo (id, numero_tavolo, numero_posti, status, ristorante_id) VALUES (4, 1, 4, 'LIBERO', 2);
INSERT INTO tavolo (id, numero_tavolo, numero_posti, status, ristorante_id) VALUES (5, 2, 4, 'LIBERO', 2);
INSERT INTO tavolo (id, numero_tavolo, numero_posti, status, ristorante_id) VALUES (6, 1, 2, 'LIBERO', 3);
INSERT INTO tavolo (id, numero_tavolo, numero_posti, status, ristorante_id) VALUES (7, 2, 8, 'LIBERO', 3);

-- ---------- utenti e credenziali ----------
INSERT INTO users (id) VALUES (1);
INSERT INTO users (id) VALUES (2);
INSERT INTO users (id) VALUES (3);
INSERT INTO credentials (id, username, password, role, user_id) VALUES (1, 'admin', '$2a$10$j6KMFXrv0bUrvwUwuzgunu.gGb9AhjH3DsDKHPkKnviIFT10mDlam', 'ADMIN', 1);
INSERT INTO credentials (id, username, password, role, user_id) VALUES (2, 'elisa', '$2a$10$B6o2Po0WQCuNIkoYSXP7JeLGPxkkoGU26fdNjk6BVdZyAYgujOM7W', 'DEFAULT', 2);
INSERT INTO credentials (id, username, password, role, user_id) VALUES (3, 'marco', '$2a$10$UEZfF1aPhXw/qJ16qW0OGeM9sB.Ya3tPM/46InXVlKQDeDyTX714.', 'DEFAULT', 3);

-- ---------- recensioni ----------
INSERT INTO recensione (id, titolo, voto, testo, data, ristorante_id, user_id) VALUES (1, 'Carbonara memorabile', 5, 'Guanciale croccante e pasta al punto giusto. Ci torno.', CURRENT_DATE - 7, 1, 2);
INSERT INTO recensione (id, titolo, voto, testo, data, ristorante_id, user_id) VALUES (2, 'Buono ma affollato', 4, 'Si mangia bene, peccato per l attesa al tavolo.', CURRENT_DATE - 3, 1, 3);
INSERT INTO recensione (id, titolo, voto, testo, data, ristorante_id, user_id) VALUES (3, 'Pesce fresco', 4, 'Vongole ottime, servizio cordiale.', CURRENT_DATE - 1, 2, 2);

-- ---------- prenotazioni ----------
-- turno normale di domani sera
INSERT INTO prenotazione (id, numero_persone, data_prenotazione, orario_prenotazione, durata_minuti, status, ristorante_id, tavolo_id, user_id) VALUES (1, 4, CURRENT_DATE + 1, TIME '20:00', 120, 'SCHEDULED', 1, 2, 2);
INSERT INTO prenotazione (id, numero_persone, data_prenotazione, orario_prenotazione, durata_minuti, status, ristorante_id, tavolo_id, user_id) VALUES (2, 2, CURRENT_DATE + 1, TIME '21:00', 120, 'SCHEDULED', 1, 1, 3);
-- annullata all ultimo momento: il tavolo deve risultare di nuovo LIBERO
INSERT INTO prenotazione (id, numero_persone, data_prenotazione, orario_prenotazione, durata_minuti, status, ristorante_id, tavolo_id, user_id) VALUES (3, 2, CURRENT_DATE, TIME '13:00', 120, 'CANCELLED', 3, 6, 2);
-- turno che scavalca la mezzanotte, per provare Prenotazione.copre()
INSERT INTO prenotazione (id, numero_persone, data_prenotazione, orario_prenotazione, durata_minuti, status, ristorante_id, tavolo_id, user_id) VALUES (4, 6, CURRENT_DATE, TIME '23:00', 180, 'SCHEDULED', 1, 3, 3);

-- ---------- ordinazioni ----------
-- Qui stanno solo i conti APERTI: alla chiusura l'ordinazione diventa uno
-- scontrino e viene cancellata. Un tavolo non puo' avere piu' di una riga
-- (vincolo unico su tavolo_id, dichiarato in Ordinazione).
-- il tavolo 4 (ristorante 2) e' occupato: 1 x 14.00 + 1 x 16.00 = 30.00
INSERT INTO ordinazione (id, totale, apertura, tavolo_id) VALUES (1, 30.00, now() - interval '30 minutes', 4);
-- il tavolo 2 (ristorante 1) e' occupato: 2 x 13.50 + 1 x 6.00 = 33.00
INSERT INTO ordinazione (id, totale, apertura, tavolo_id) VALUES (2, 33.00, now() - interval '20 minutes', 2);
INSERT INTO riga_ordinazione (id, quantita, prezzo_unitario, ordinazione_id, piatto_id) VALUES (1, 1, 14.00, 1, 5);
INSERT INTO riga_ordinazione (id, quantita, prezzo_unitario, ordinazione_id, piatto_id) VALUES (2, 1, 16.00, 1, 6);
INSERT INTO riga_ordinazione (id, quantita, prezzo_unitario, ordinazione_id, piatto_id) VALUES (3, 2, 13.50, 2, 1);
INSERT INTO riga_ordinazione (id, quantita, prezzo_unitario, ordinazione_id, piatto_id) VALUES (4, 1, 6.00, 2, 4);

-- ---------- scontrini ----------
-- Il conto del tavolo 2 del ristorante 2, pagato all'una e mezza: era
-- un'ordinazione, alla chiusura e' diventato questo e il tavolo e' tornato
-- libero (e di nuovo eliminabile). Numero del tavolo, nome e prezzo dei piatti
-- sono copie: restano veri anche se il tavolo sparisce o il listino cambia.
INSERT INTO scontrino (id, data_ora, apertura, numero_tavolo, totale, ristorante_id) VALUES (1, CURRENT_DATE + TIME '13:30', CURRENT_DATE + TIME '12:00', 2, 46.00, 2);
INSERT INTO riga_scontrino (id, quantita, nome_piatto, prezzo_unitario, piatto_id, scontrino_id) VALUES (1, 2, 'Spaghetti alle vongole', 16.00, 6, 1);
INSERT INTO riga_scontrino (id, quantita, nome_piatto, prezzo_unitario, piatto_id, scontrino_id) VALUES (2, 1, 'Fritto misto', 14.00, 5, 1);

-- ---------- sequenze ----------
-- Gli id qui sopra sono scritti a mano, ma le sequenze partirebbero comunque
-- da 1: senza questo blocco il primo inserimento dall'applicazione andrebbe in
-- collisione con i dati di prova. Si spostano oltre.
ALTER SEQUENCE ristorante_seq RESTART WITH 1000;
ALTER SEQUENCE piatto_seq RESTART WITH 1000;
ALTER SEQUENCE tavolo_seq RESTART WITH 1000;
ALTER SEQUENCE users_seq RESTART WITH 1000;
ALTER SEQUENCE credentials_seq RESTART WITH 1000;
ALTER SEQUENCE recensione_seq RESTART WITH 1000;
ALTER SEQUENCE prenotazione_seq RESTART WITH 1000;
ALTER SEQUENCE ordinazione_seq RESTART WITH 1000;
ALTER SEQUENCE riga_ordinazione_seq RESTART WITH 1000;
ALTER SEQUENCE scontrino_seq RESTART WITH 1000;
ALTER SEQUENCE riga_scontrino_seq RESTART WITH 1000;

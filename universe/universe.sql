CREATE TABLE items (name TEXT, description TEXT);
CREATE TABLE possessions (owner TEXT, item TEXT, qty INTEGER, timestamp INTEGER);
CREATE TABLE resources (x INTEGER, y INTEGER, item TEXT, yield INTEGER);
CREATE TABLE ship_types (name TEXT, attack INTEGER, defense INTEGER, shields INTEGER, description TEXT);
CREATE TABLE ships (x INTEGER, y INTEGER, owner TEXT, ship_type TEXT, shields REAL);

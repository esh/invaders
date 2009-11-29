BEGIN TRANSACTION;
CREATE TABLE possessions (owner TEXT, item TEXT, qty INTEGER, timestamp INTEGER);
CREATE TABLE items (name TEXT, description TEXT);
INSERT INTO "items" VALUES('titanium','');
INSERT INTO "items" VALUES('tungsten','');
INSERT INTO "items" VALUES('uranium','');
INSERT INTO "items" VALUES('deuterium','');
CREATE TABLE ship_types (name TEXT, attack INTEGER, defense INTEGER, shields INTEGER, description TEXT);
INSERT INTO "ship_types" VALUES('ark ship',1,1,1,'');
INSERT INTO "ship_types" VALUES('frigate',1,1,1,'');
INSERT INTO "ship_types" VALUES('cruiser',1,1,1,'');
CREATE TABLE resources (x INTEGER, y INTEGER, item TEXT, yield INTEGER);
INSERT INTO "resources" VALUES(2,1,'titanium',1);
INSERT INTO "resources" VALUES(2,1,'deuterium',1);
INSERT INTO "resources" VALUES(1,0,'tungsten',1);
INSERT INTO "resources" VALUES(0,0,'uranium',1);
INSERT INTO "resources" VALUES(0,0,'titanium',1);
INSERT INTO "resources" VALUES(3,3,'titanium',3);
INSERT INTO "resources" VALUES(3,3,'uranium',2);
INSERT INTO "resources" VALUES(3,0,'tungsten',2);
INSERT INTO "resources" VALUES(3,4,'deuterium',1);
INSERT INTO "resources" VALUES(4,1,'titanium',3);
INSERT INTO "resources" VALUES(4,1,'uranium',1);
CREATE TABLE ships(id INTEGER PRIMARY KEY, x INTEGER, y INTEGER, owner TEXT, ship_type TEXT, shields REAL);
INSERT INTO "ships" VALUES(1,1,0,'esh','ark ship',1.0);
INSERT INTO "ships" VALUES(2,2,1,'esh','frigate',1.0);
INSERT INTO "ships" VALUES(3,0,0,'yuki','ark ship',1.0);
CREATE TABLE ship_costs (ship_type TEXT, item TEXT, qty INTEGER);
INSERT INTO "ship_costs" VALUES('frigate','titanium',1000);
INSERT INTO "ship_costs" VALUES('frigate','tungsten',300);
INSERT INTO "ship_costs" VALUES('frigate','uranium',300);
INSERT INTO "ship_costs" VALUES('cruiser','titanium',10000);
INSERT INTO "ship_costs" VALUES('cruiser','tungsten',5000);
INSERT INTO "ship_costs" VALUES('cruiser','uranium',4000);
COMMIT;

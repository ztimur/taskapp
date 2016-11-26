CREATE KEYSPACE IF NOT EXISTS taskdemo WITH replication = {'class':'SimpleStrategy', 'replication_factor':3};
CREATE TABLE IF NOT EXISTS taskdemo.tasks( id varchar, task varchar, status int, createdOn timestamp,  PRIMARY KEY (id));
USE taskdemo;
INSERT INTO tasks (id, task, status, createdOn) VALUES ('1', 'My first task', 0, 1390815970);
INSERT INTO tasks (id, task, status, createdOn) VALUES ('2', 'Perform unit testing', 2, 1390815993);
INSERT INTO tasks (id, task, status, createdOn) VALUES ('3', 'Find bugs', 2, 1390817659);
INSERT INTO tasks (id, task, status, createdOn) VALUES ('4', 'Test in small devices', 2, 1390818389);

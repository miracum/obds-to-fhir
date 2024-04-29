ALTER SESSION SET CONTAINER = FREEPDB1;
CREATE USER DWH_ROUTINE IDENTIFIED BY devPassword;
GRANT CREATE SESSION, CREATE TABLE TO DWH_ROUTINE;
GRANT UNLIMITED TABLESPACE TO DWH_ROUTINE;

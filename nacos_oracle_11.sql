CREATE TABLE "CONFIG_INFO" (
    "ID" NUMBER ( 20, 0 ) not NULL ,
    "DATA_ID" NVARCHAR2 ( 255 ) NOT NULL,
    "GROUP_ID" NVARCHAR2 ( 255 ) DEFAULT NULL,
    "CONTENT" CLOB NOT NULL,
    "MD5" NVARCHAR2 ( 32 ),
    "GMT_CREATE" DATE DEFAULT SYSDATE NOT NULL,
    "GMT_MODIFIED" DATE DEFAULT SYSDATE NOT NULL,
    "SRC_USER" CLOB,
    "SRC_IP" NVARCHAR2 ( 50 ) DEFAULT NULL,
    "APP_NAME" NVARCHAR2 ( 128 ) DEFAULT NULL,
    "TENANT_ID" NVARCHAR2 ( 128 ) DEFAULT '',
    "C_DESC" NVARCHAR2 ( 256 ) DEFAULT NULL,
    "C_USE" NVARCHAR2 ( 64 ) DEFAULT NULL,
    "EFFECT" NVARCHAR2 ( 64 ) DEFAULT NULL,
    "TYPE" NVARCHAR2 ( 64 ) DEFAULT NULL,
    "C_SCHEMA" CLOB,
    CONSTRAINT "CI_PK_ID" PRIMARY KEY ( "ID" ),
    CONSTRAINT "UK_CONFIGINFO_DATAGROUPTENANT" UNIQUE ( "DATA_ID", "GROUP_ID", "TENANT_ID" )
);
CREATE TABLE "CONFIG_INFO_AGGR" (
    "ID" NUMBER ( 20, 0 ) not NULL,
    "DATA_ID" NVARCHAR2 ( 255 ) NOT NULL,
    "GROUP_ID" NVARCHAR2 ( 255 ) NOT NULL,
    "DATUM_ID" NVARCHAR2 ( 255 ) NOT NULL,
    "CONTENT" CLOB NOT NULL,
    "GMT_MODIFIED" DATE DEFAULT SYSDATE NOT NULL,
    "APP_NAME" NVARCHAR2 ( 128 ) DEFAULT NULL,
    "TENANT_ID" NVARCHAR2 ( 128 ) DEFAULT '',
    CONSTRAINT "CIA_PK_ID" PRIMARY KEY ( "ID" ),
    CONSTRAINT "UK_CIA_DATAGROUPTENANTDATUM" UNIQUE ( "DATA_ID", "GROUP_ID", "TENANT_ID", "DATUM_ID" )
);
CREATE TABLE "CONFIG_INFO_BETA" (
    "ID" NUMBER ( 20, 0 ) not NULL,
    "DATA_ID" NVARCHAR2 ( 255 ) NOT NULL,
    "GROUP_ID" NVARCHAR2 ( 128 ) NOT NULL,
    "APP_NAME" NVARCHAR2 ( 128 ) DEFAULT NULL,
    "CONTENT" CLOB NOT NULL,
    "BETA_IPS" NVARCHAR2 ( 1024 ) DEFAULT NULL,
    "MD5" NVARCHAR2 ( 32 ) DEFAULT NULL,
    "GMT_CREATE" DATE DEFAULT SYSDATE NOT NULL,
    "GMT_MODIFIED" DATE DEFAULT SYSDATE NOT NULL,
    "SRC_USER" CLOB,
    "SRC_IP" NVARCHAR2 ( 50 ),
    "TENANT_ID" NVARCHAR2 ( 128 ) DEFAULT '',
    CONSTRAINT "CIB_PK_ID" PRIMARY KEY ( "ID" ),
    CONSTRAINT "UK_CIB_DATAGROUPTENANT" UNIQUE ( "DATA_ID", "GROUP_ID", "TENANT_ID" )
);
CREATE TABLE "CONFIG_INFO_TAG" (
    "ID" NUMBER ( 20, 0 ) not NULL ,
    "DATA_ID" NVARCHAR2 ( 255 ) NOT NULL,
    "GROUP_ID" NVARCHAR2 ( 128 ) NOT NULL,
    "TENANT_ID" NVARCHAR2 ( 128 ) DEFAULT '',
    "TAG_ID" NVARCHAR2 ( 128 ) NOT NULL,
    "APP_NAME" NVARCHAR2 ( 128 ) DEFAULT NULL,
    "CONTENT" CLOB NOT NULL,
    "MD5" NVARCHAR2 ( 32 ) DEFAULT NULL,
    "GMT_CREATE" DATE DEFAULT SYSDATE NOT NULL,
    "GMT_MODIFIED" DATE DEFAULT SYSDATE NOT NULL,
    "SRC_USER" CLOB,
    "SRC_IP" NVARCHAR2 ( 50 ),
    CONSTRAINT "CIT_PK_ID" PRIMARY KEY ( "ID" ),
    CONSTRAINT "UK_CIT_DATAGROUPTENANTTAG" UNIQUE ( "DATA_ID", "GROUP_ID", "TENANT_ID", "TAG_ID" )
);
CREATE TABLE "CONFIG_TAGS_RELATION" (
    "ID" NUMBER ( 20, 0 ) not NULL,
    "TAG_NAME" NVARCHAR2 ( 128 ) NOT NULL,
    "TAG_TYPE" NVARCHAR2 ( 64 ) DEFAULT NULL,
    "DATA_ID" NVARCHAR2 ( 255 ) NOT NULL,
    "GROUP_ID" NVARCHAR2 ( 128 ) NOT NULL,
    "TENANT_ID" NVARCHAR2 ( 128 ) DEFAULT '',
    "NID" NUMBER ( 20, 0 ) NOT NULL,
    CONSTRAINT "CTR_PK_ID" PRIMARY KEY ( "NID" ),
    CONSTRAINT "UK_CTR_CONFIGIDTAG" UNIQUE ( "ID", "TAG_NAME", "TAG_TYPE" )
);
CREATE INDEX "IDX_CTR_TENANT_ID" ON CONFIG_TAGS_RELATION ( "TENANT_ID" );
CREATE TABLE "GROUP_CAPACITY" (
    "ID" NUMBER ( 20, 0 ) not NULL ,
    "GROUP_ID" NVARCHAR2 ( 128 ) DEFAULT '' NOT NULL,
    "QUOTA" NUMBER ( 10, 0 ) DEFAULT 0 NOT NULL,
    "USAGE" NUMBER ( 10, 0 ) DEFAULT 0 NOT NULL,
    "MAX_SIZE" NUMBER ( 10, 0 ) DEFAULT 0 NOT NULL,
    "MAX_AGGR_COUNT" NUMBER ( 10, 0 ) DEFAULT 0 NOT NULL,
    "MAX_AGGR_SIZE" NUMBER ( 10, 0 ) DEFAULT 0 NOT NULL,
    "MAX_HISTORY_COUNT" NUMBER ( 10, 0 ) DEFAULT 0 NOT NULL,
    "GMT_CREATE" DATE DEFAULT SYSDATE NOT NULL,
    "GMT_MODIFIED" DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT "GC_PK_ID" PRIMARY KEY ( "ID" ),
    CONSTRAINT "UK_GC_GROUP_ID" UNIQUE ( "GROUP_ID" )
);
CREATE TABLE "HIS_CONFIG_INFO" (
    "ID" NUMBER ( 20, 0 ) NOT NULL,
    "NID" NUMBER ( 20, 0 ) not NULL,
    "DATA_ID" NVARCHAR2 ( 255 ) NOT NULL,
    "GROUP_ID" NVARCHAR2 ( 128 ) NOT NULL,
    "APP_NAME" NVARCHAR2 ( 128 ) DEFAULT NULL,
    "CONTENT" CLOB NOT NULL,
    "MD5" NVARCHAR2 ( 32 ) DEFAULT NULL,
    "GMT_CREATE" DATE DEFAULT SYSDATE NOT NULL,
    "GMT_MODIFIED" DATE DEFAULT SYSDATE NOT NULL,
    "SRC_USER" CLOB,
    "SRC_IP" NVARCHAR2 ( 50 ) DEFAULT NULL,
    "OP_TYPE" NCHAR ( 10 ) DEFAULT NULL,
    "TENANT_ID" NVARCHAR2 ( 128 ) DEFAULT '',
    CONSTRAINT "HCI_PK_ID" PRIMARY KEY ( "NID" )
);
CREATE INDEX "IDX_HCI_GMT_CREATE" ON HIS_CONFIG_INFO ( "GMT_CREATE" );
CREATE INDEX "IDX_HCI_GMT_MODIFIED" ON HIS_CONFIG_INFO ( "GMT_MODIFIED" );
CREATE INDEX "IDX_HCI_DID" ON HIS_CONFIG_INFO ( "DATA_ID" );
CREATE TABLE "TENANT_CAPACITY" (
    "ID" NUMBER ( 20, 0 ) not NULL,
    "TENANT_ID" NVARCHAR2 ( 128 ) DEFAULT '' NOT NULL,
    "QUOTA" NUMBER ( 10, 0 ) DEFAULT 0 NOT NULL,
    "USAGE" NUMBER ( 10, 0 ) DEFAULT 0 NOT NULL,
    "MAX_SIZE" NUMBER ( 10, 0 ) DEFAULT 0 NOT NULL,
    "MAX_AGGR_COUNT" NUMBER ( 10, 0 ) DEFAULT 0 NOT NULL,
    "MAX_AGGR_SIZE" NUMBER ( 10, 0 ) DEFAULT 0 NOT NULL,
    "MAX_HISTORY_COUNT" NUMBER ( 10, 0 ) DEFAULT 0 NOT NULL,
    "GMT_CREATE" DATE DEFAULT SYSDATE NOT NULL,
    "GMT_MODIFIED" DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT "TC_PK_ID" PRIMARY KEY ( "ID" ),
    CONSTRAINT "UK_TC_TENANT_ID" UNIQUE ( "TENANT_ID" )
);
CREATE TABLE "TENANT_INFO" (
    "ID" NUMBER ( 20, 0 ) not NULL,
    "KP" NVARCHAR2 ( 128 ) NOT NULL,
    "TENANT_ID" NVARCHAR2 ( 128 ) DEFAULT '',
    "TENANT_NAME" NVARCHAR2 ( 128 ) DEFAULT '',
    "TENANT_DESC" NVARCHAR2 ( 256 ) DEFAULT NULL,
    "CREATE_SOURCE" NVARCHAR2 ( 32 ) DEFAULT NULL,
    "GMT_CREATE" NUMBER ( 20, 0 ) NOT NULL,
    "GMT_MODIFIED" NUMBER ( 20, 0 ) NOT NULL,
    CONSTRAINT "TI_PK_ID" PRIMARY KEY ( "ID" ),
    CONSTRAINT "UK_TENANT_INFO_KPTENANTID" UNIQUE ( "KP", "TENANT_ID" )
);
CREATE INDEX IDX_TI_TENANT_ID ON TENANT_INFO ( "TENANT_ID" );
CREATE TABLE "USERS" (
    "USERNAME" NVARCHAR2 ( 50 ) NOT NULL,
    "PASSWORD" NVARCHAR2 ( 500 ) NOT NULL,
    "ENABLED" NUMBER ( 1, 0 ) DEFAULT 1 NOT NULL,
    CONSTRAINT "USERS_PK_NAME" PRIMARY KEY ( "USERNAME" )
);
CREATE TABLE "ROLES" (
    "USERNAME" NVARCHAR2 ( 50 ) NOT NULL,
    "ROLE" NVARCHAR2 ( 50 ) NOT NULL,
    CONSTRAINT "UK_USERNAME_ROLE" UNIQUE ( "USERNAME", "ROLE" )
);
CREATE TABLE "PERMISSIONS" (
    "ROLE" NVARCHAR2 ( 50 ) NOT NULL,
    "RESOURCE" NVARCHAR2 ( 512 ) NOT NULL,
    "ACTION" NVARCHAR2 ( 8 ) NOT NULL,
    CONSTRAINT UK_ROLE_PERMISSION UNIQUE ( "ROLE", "RESOURCE", "ACTION" )
);
INSERT INTO "USERS" ( "USERNAME", "PASSWORD", "ENABLED" ) VALUES( 'nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', 1 );
INSERT INTO "ROLES" ( "USERNAME", "ROLE" ) VALUES   ( 'nacos', 'ROLE_ADMIN' );

create sequence seq_config_info;
create or replace trigger tg_config_info
--before:执行DML等操作之前触发
before insert on config_info  
--行级触发器
for each row  
begin 
    select seq_config_info.nextval into :new.id from dual;
end;
/
create sequence seq_config_tags_relation;
create or replace trigger tg_config_tags_relation
--before:执行DML等操作之前触发
before insert on config_tags_relation  
--行级触发器
for each row  
begin 
    select seq_config_tags_relation.nextval into :new.nid from dual;
end;
/
create sequence seq_config_info_aggr;
create or replace trigger tg_config_info_aggr
--before:
before insert on config_info_aggr  
--行级触发器
for each row  
begin 
    select seq_config_info_aggr.nextval into :new.id from dual;
end;
/
create sequence seq_config_info_beta;
create or replace trigger tg_config_info_beta
--before:
before insert on config_info_beta  
--行级触发器
for each row  
begin 
    select seq_config_info_beta.nextval into :new.id from dual;
end;
/
create sequence seq_config_info_tag;
create or replace trigger tg_config_info_tag
--before:
before insert on config_info_tag  
--行级触发器
for each row  
begin 
    select seq_config_info_tag.nextval into :new.id from dual;
end;
/
create sequence seq_config_info_tag;
create or replace trigger tg_config_info_tag
--before:
before insert on config_info_tag  
--行级触发器
for each row  
begin 
    select seq_config_info_tag.nextval into :new.id from dual;
end;
/
create sequence seq_group_capacity;
create or replace trigger tg_group_capacity
--before:
before insert on group_capacity  
--行级触发器
for each row  
begin 
    select seq_group_capacity.nextval into :new.id from dual;
end;
/
create sequence seq_his_config_info;
create or replace trigger tg_his_config_info
--before:
before insert on his_config_info  
--行级触发器
for each row  
begin 
    select seq_his_config_info.nextval into :new.nid from dual;
end;
/
create sequence seq_tenant_capacity;
create or replace trigger tg_tenant_capacity
--before:
before insert on tenant_capacity  
--行级触发器
for each row  
begin 
    select seq_tenant_capacity.nextval into :new.id from dual;
end;
/
create sequence seq_tenant_info;
create or replace trigger tg_tenant_info
--before:
before insert on tenant_info  
--行级触发器
for each row  
begin 
    select seq_tenant_info.nextval into :new.id from dual;
end;
/

/*
 Navicat Premium Data Transfer

 Source Server         : small_localhost
 Source Server Type    : PostgreSQL
 Source Server Version : 170006 (170006)
 Source Host           : localhost:5432
 Source Catalog        : cms
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 170006 (170006)
 File Encoding         : 65001

 Date: 03/10/2025 20:25:01
*/


-- ----------------------------
-- Table structure for vector_store
-- ----------------------------
DROP TABLE IF EXISTS "public"."vector_store";
CREATE TABLE "public"."vector_store" (
                                         "id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                         "content" text COLLATE "pg_catalog"."default",
                                         "metadata" jsonb,
                                         "embedding" "public"."vector"
)
;
ALTER TABLE "public"."vector_store" OWNER TO "postgres";

-- ----------------------------
-- Indexes structure for table vector_store
-- ----------------------------
CREATE INDEX "vector_store_embedding_idx" ON "public"."vector_store" (
                                                                      "embedding" "public"."vector_cosine_ops" ASC NULLS LAST
    );
CREATE INDEX "vector_store_metadata_idx" ON "public"."vector_store" USING gin (
    "metadata" "pg_catalog"."jsonb_ops"
    );

-- ----------------------------
-- Primary Key structure for table vector_store
-- ----------------------------
ALTER TABLE "public"."vector_store" ADD CONSTRAINT "vector_store_pkey" PRIMARY KEY ("id");



/*
 Navicat Premium Data Transfer

 Source Server         : small_localhost
 Source Server Type    : PostgreSQL
 Source Server Version : 170006 (170006)
 Source Host           : localhost:5432
 Source Catalog        : cms
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 170006 (170006)
 File Encoding         : 65001

 Date: 03/10/2025 20:25:42
*/


-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user_role";
CREATE TABLE "public"."sys_user_role" (
                                          "user_id" int8 NOT NULL,
                                          "role_id" int8 NOT NULL
)
;
ALTER TABLE "public"."sys_user_role" OWNER TO "postgres";

-- ----------------------------
-- Primary Key structure for table sys_user_role
-- ----------------------------
ALTER TABLE "public"."sys_user_role" ADD CONSTRAINT "sys_user_role_pkey" PRIMARY KEY ("user_id", "role_id");

-- ----------------------------
-- Foreign Keys structure for table sys_user_role
-- ----------------------------
ALTER TABLE "public"."sys_user_role" ADD CONSTRAINT "sys_user_role_role_id_fkey" FOREIGN KEY ("role_id") REFERENCES "public"."sys_role" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."sys_user_role" ADD CONSTRAINT "sys_user_role_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;




/*
 Navicat Premium Data Transfer

 Source Server         : small_localhost
 Source Server Type    : PostgreSQL
 Source Server Version : 170006 (170006)
 Source Host           : localhost:5432
 Source Catalog        : cms
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 170006 (170006)
 File Encoding         : 65001

 Date: 03/10/2025 20:26:06
*/


-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role";
CREATE TABLE "public"."sys_role" (
                                     "id" int8 NOT NULL DEFAULT nextval('sys_role_id_seq'::regclass),
                                     "role_code" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                     "role_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                     "description" varchar(255) COLLATE "pg_catalog"."default",
                                     "sort" int4,
                                     "status" int4,
                                     "created_at" timestamp(6),
                                     "updated_at" timestamp(6)
)
;
ALTER TABLE "public"."sys_role" OWNER TO "postgres";

-- ----------------------------
-- Uniques structure for table sys_role
-- ----------------------------
ALTER TABLE "public"."sys_role" ADD CONSTRAINT "sys_role_role_code_key" UNIQUE ("role_code");

-- ----------------------------
-- Primary Key structure for table sys_role
-- ----------------------------
ALTER TABLE "public"."sys_role" ADD CONSTRAINT "sys_role_pkey" PRIMARY KEY ("id");



/*
 Navicat Premium Data Transfer

 Source Server         : small_localhost
 Source Server Type    : PostgreSQL
 Source Server Version : 170006 (170006)
 Source Host           : localhost:5432
 Source Catalog        : cms
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 170006 (170006)
 File Encoding         : 65001

 Date: 03/10/2025 20:25:50
*/


-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role_permission";
CREATE TABLE "public"."sys_role_permission" (
                                                "role_id" int8 NOT NULL,
                                                "permission_id" int8 NOT NULL
)
;
ALTER TABLE "public"."sys_role_permission" OWNER TO "postgres";

-- ----------------------------
-- Primary Key structure for table sys_role_permission
-- ----------------------------
ALTER TABLE "public"."sys_role_permission" ADD CONSTRAINT "sys_role_permission_pkey" PRIMARY KEY ("role_id", "permission_id");

-- ----------------------------
-- Foreign Keys structure for table sys_role_permission
-- ----------------------------
ALTER TABLE "public"."sys_role_permission" ADD CONSTRAINT "sys_role_permission_permission_id_fkey" FOREIGN KEY ("permission_id") REFERENCES "public"."sys_permission" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."sys_role_permission" ADD CONSTRAINT "sys_role_permission_role_id_fkey" FOREIGN KEY ("role_id") REFERENCES "public"."sys_role" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;




/*
 Navicat Premium Data Transfer

 Source Server         : small_localhost
 Source Server Type    : PostgreSQL
 Source Server Version : 170006 (170006)
 Source Host           : localhost:5432
 Source Catalog        : cms
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 170006 (170006)
 File Encoding         : 65001

 Date: 03/10/2025 20:26:15
*/


-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_permission";
CREATE TABLE "public"."sys_permission" (
                                           "id" int8 NOT NULL DEFAULT nextval('sys_permission_id_seq'::regclass),
                                           "permission_code" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                           "permission_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                           "description" varchar(255) COLLATE "pg_catalog"."default",
                                           "resource" varchar(255) COLLATE "pg_catalog"."default",
                                           "method" varchar(255) COLLATE "pg_catalog"."default",
                                           "sort" int4,
                                           "status" int4,
                                           "created_at" timestamp(6),
                                           "updated_at" timestamp(6),
                                           "parent_id" int8
)
;
ALTER TABLE "public"."sys_permission" OWNER TO "postgres";

-- ----------------------------
-- Uniques structure for table sys_permission
-- ----------------------------
ALTER TABLE "public"."sys_permission" ADD CONSTRAINT "sys_permission_permission_code_key" UNIQUE ("permission_code");

-- ----------------------------
-- Primary Key structure for table sys_permission
-- ----------------------------
ALTER TABLE "public"."sys_permission" ADD CONSTRAINT "sys_permission_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table sys_permission
-- ----------------------------
ALTER TABLE "public"."sys_permission" ADD CONSTRAINT "sys_permission_parent_id_fkey" FOREIGN KEY ("parent_id") REFERENCES "public"."sys_permission" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;



/*
 Navicat Premium Data Transfer

 Source Server         : small_localhost
 Source Server Type    : PostgreSQL
 Source Server Version : 170006 (170006)
 Source Host           : localhost:5432
 Source Catalog        : cms
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 170006 (170006)
 File Encoding         : 65001

 Date: 03/10/2025 20:26:24
*/


-- ----------------------------
-- Table structure for chat_sessions
-- ----------------------------
DROP TABLE IF EXISTS "public"."chat_sessions";
CREATE TABLE "public"."chat_sessions" (
                                          "id" int8 NOT NULL DEFAULT nextval('chat_sessions_id_seq'::regclass),
                                          "session_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                          "user_id" int8 NOT NULL,
                                          "title" varchar(500) COLLATE "pg_catalog"."default",
                                          "first_query" text COLLATE "pg_catalog"."default",
                                          "message_count" int4 DEFAULT 1,
                                          "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
                                          "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
ALTER TABLE "public"."chat_sessions" OWNER TO "postgres";

-- ----------------------------
-- Indexes structure for table chat_sessions
-- ----------------------------
CREATE INDEX "idx_chat_sessions_created_at" ON "public"."chat_sessions" USING btree (
    "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
    );
CREATE INDEX "idx_chat_sessions_user_id" ON "public"."chat_sessions" USING btree (
    "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Uniques structure for table chat_sessions
-- ----------------------------
ALTER TABLE "public"."chat_sessions" ADD CONSTRAINT "chat_sessions_session_id_key" UNIQUE ("session_id");

-- ----------------------------
-- Primary Key structure for table chat_sessions
-- ----------------------------
ALTER TABLE "public"."chat_sessions" ADD CONSTRAINT "chat_sessions_pkey" PRIMARY KEY ("id");

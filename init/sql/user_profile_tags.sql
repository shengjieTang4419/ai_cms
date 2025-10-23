-- =============================================
-- 用户画像和标签系统表结构
-- PostgreSQL 数据库
-- 更新日期：2025/01/16
-- =============================================

-- ----------------------------
-- Table structure for user_profiles
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_profiles";
CREATE TABLE "public"."user_profiles" (
    "id" SERIAL PRIMARY KEY,
    "user_id" int8 NOT NULL,
    "gender" varchar(10) COLLATE "pg_catalog"."default",
    "age" int4,
    "location" int4,
    "occupation" int4,
    "hobbies" JSON,
    "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------
-- Table structure for chat_tags
-- ----------------------------
DROP TABLE IF EXISTS "public"."chat_tags";
CREATE TABLE "public"."chat_tags" (
    "id" SERIAL PRIMARY KEY,
    "user_id" int8 NOT NULL,
    "session_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
    "tag_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
    "frequency" int4 NOT NULL DEFAULT 1,
    "source_type" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'CHAT'::character varying,
    "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------
-- Table structure for user_tags
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_tags";
CREATE TABLE "public"."user_tags" (
    "id" SERIAL PRIMARY KEY,
    "user_id" int8 NOT NULL,
    "tag_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
    "base_weight" DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    "chat_weight" DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    "fusion_weight" DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    "total_weight" DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    "source_type" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'CHAT'::character varying,
    "last_updated" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
    "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------
-- Indexes structure for tables
-- ----------------------------

-- user_profiles indexes
CREATE UNIQUE INDEX "idx_user_profiles_user_id" ON "public"."user_profiles" USING btree (
    "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- chat_tags indexes
CREATE INDEX "idx_chat_tags_user_id" ON "public"."chat_tags" USING btree (
    "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_chat_tags_session_id" ON "public"."chat_tags" USING btree (
    "session_id" "pg_catalog"."varchar_ops" ASC NULLS LAST
);
CREATE INDEX "idx_chat_tags_user_session" ON "public"."chat_tags" USING btree (
    "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
    "session_id" "pg_catalog"."varchar_ops" ASC NULLS LAST
);

-- user_tags indexes
CREATE INDEX "idx_user_tags_user_id" ON "public"."user_tags" USING btree (
    "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_tags_total_weight" ON "public"."user_tags" USING btree (
    "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
    "total_weight" "pg_catalog"."numeric_ops" DESC NULLS LAST
);
CREATE INDEX "idx_user_tags_tag_name" ON "public"."user_tags" USING btree (
    "tag_name" "pg_catalog"."varchar_ops" ASC NULLS LAST
);

-- ----------------------------
-- Constraints structure for tables
-- ----------------------------

-- user_profiles constraints
ALTER TABLE "public"."user_profiles" ADD CONSTRAINT "user_profiles_user_id_key" UNIQUE ("user_id");

-- user_tags constraints
ALTER TABLE "public"."user_tags" ADD CONSTRAINT "user_tags_user_tag_unique" UNIQUE ("user_id", "tag_name");

-- ----------------------------
-- Comments
-- ----------------------------
COMMENT ON TABLE "public"."user_profiles" IS '用户基础画像表';
COMMENT ON COLUMN "public"."user_profiles"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."user_profiles"."gender" IS '性别：男、女、其他';
COMMENT ON COLUMN "public"."user_profiles"."age" IS '年龄';
COMMENT ON COLUMN "public"."user_profiles"."location" IS '居住地（城市代码）';
COMMENT ON COLUMN "public"."user_profiles"."occupation" IS '职业（职业代码）';
COMMENT ON COLUMN "public"."user_profiles"."hobbies" IS '爱好（JSON格式存储）';

COMMENT ON TABLE "public"."chat_tags" IS '聊天标签表 - 记录每次聊天会话中提取的标签';
COMMENT ON COLUMN "public"."chat_tags"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."chat_tags"."session_id" IS '会话ID';
COMMENT ON COLUMN "public"."chat_tags"."tag_name" IS '标签名称';
COMMENT ON COLUMN "public"."chat_tags"."frequency" IS '标签频率（在该会话中出现的次数）';
COMMENT ON COLUMN "public"."chat_tags"."source_type" IS '标签来源（PROFILE-来自用户画像, CHAT-来自聊天内容, FUSION-融合标签）';

COMMENT ON TABLE "public"."user_tags" IS '用户标签表 - 用户最终的综合标签，融合了基础画像和聊天行为';
COMMENT ON COLUMN "public"."user_tags"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."user_tags"."tag_name" IS '标签名称';
COMMENT ON COLUMN "public"."user_tags"."base_weight" IS '基础权重（来自用户画像，DECIMAL类型）';
COMMENT ON COLUMN "public"."user_tags"."chat_weight" IS '聊天权重（来自聊天行为，DECIMAL类型）';
COMMENT ON COLUMN "public"."user_tags"."fusion_weight" IS '融合权重（基础画像与聊天标签重叠时的额外权重，DECIMAL类型）';
COMMENT ON COLUMN "public"."user_tags"."total_weight" IS '总权重（base_weight + chat_weight + fusion_weight，DECIMAL类型）';
COMMENT ON COLUMN "public"."user_tags"."source_type" IS '标签来源类型（PROFILE-来自用户画像, CHAT-来自聊天内容, FUSION-融合标签）';



ALTER TABLE "public"."user_profiles"
    ALTER COLUMN "location" TYPE varchar(64) USING "location"::varchar(64);

COMMENT ON COLUMN "public"."user_profiles"."location" IS '居住地';

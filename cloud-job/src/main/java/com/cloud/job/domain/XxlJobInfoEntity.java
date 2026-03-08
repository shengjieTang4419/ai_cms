package com.cloud.job.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * XXL-JOB任务信息表实体类
 */
@Data
@Entity
@Table(name = "xxl_job_info")
public class XxlJobInfoEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 执行器主键ID
     */
    @Column(name = "job_group")
    private Integer jobGroup;

    /**
     * 任务描述
     */
    @Column(name = "job_desc", nullable = false, length = 255)
    private String jobDesc;

    /**
     * CRON表达式
     */
    @Column(name = "job_cron", nullable = false, length = 128)
    private String jobCron;

    /**
     * GLUE模式的Java类名
     */
    @Column(name = "job_class", length = 255)
    private String jobClass;

    /**
     * 执行器参数
     */
    @Column(name = "job_param", length = 512)
    private String jobParam;

    /**
     * 任务状态：0-停止，1-运行
     */
    @Column(name = "job_status")
    private Integer jobStatus = 0;

    /**
     * 负责人
     */
    @Column(name = "author", length = 64)
    private String author;

    /**
     * 报警邮件
     */
    @Column(name = "alarm_email", length = 255)
    private String alarmEmail;

    /**
     * 执行器路由策略
     */
    @Column(name = "executor_route_strategy", length = 50)
    private String executorRouteStrategy = "FIRST";

    /**
     * 执行器任务handler
     */
    @Column(name = "executor_handler", length = 255)
    private String executorHandler;

    /**
     * 执行器任务参数
     */
    @Column(name = "executor_param", length = 512)
    private String executorParam;

    /**
     * 阻塞处理策略
     */
    @Column(name = "executor_block_strategy", length = 50)
    private String executorBlockStrategy = "SERIAL_EXECUTION";

    /**
     * 任务执行超时时间，单位秒
     */
    @Column(name = "executor_timeout")
    private Integer executorTimeout = 0;

    /**
     * 失败重试次数
     */
    @Column(name = "executor_fail_retry_count")
    private Integer executorFailRetryCount = 0;

    /**
     * GLUE类型
     */
    @Column(name = "glue_type", nullable = false, length = 50)
    private String glueType = "BEAN";

    /**
     * GLUE源代码
     */
    @Column(name = "glue_source", columnDefinition = "TEXT")
    private String glueSource;

    /**
     * GLUE备注
     */
    @Column(name = "glue_remark", length = 128)
    private String glueRemark;

    /**
     * GLUE更新时间
     */
    @Column(name = "glue_updatetime")
    private LocalDateTime glueUpdatetime;

    /**
     * 子任务ID，多个逗号分隔
     */
    @Column(name = "child_jobid", length = 255)
    private String childJobid;

    /**
     * 调度状态：0-停止，1-运行
     */
    @Column(name = "trigger_status")
    private Integer triggerStatus = 0;

    /**
     * 上次调度时间
     */
    @Column(name = "trigger_last_time")
    private Long triggerLastTime = 0L;

    /**
     * 下次调度时间
     */
    @Column(name = "trigger_next_time")
    private Long triggerNextTime = 0L;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 是否删除：0-正常，1-删除
     */
    @Column(name = "delete_flag")
    private Integer deleteFlag = 0;
}

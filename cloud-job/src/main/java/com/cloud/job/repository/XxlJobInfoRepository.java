package com.cloud.job.repository;

import com.cloud.job.domain.XxlJobInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * XXL-JOB任务信息Repository
 */
@Repository
public interface XxlJobInfoRepository extends JpaRepository<XxlJobInfoEntity, Long> {

    /**
     * 根据任务描述和执行器参数查询任务
     */
    @Query("SELECT j FROM XxlJobInfoEntity j WHERE j.jobDesc = :jobDesc AND j.executorParam = :executorParam AND j.deleteFlag = 0")
    Optional<XxlJobInfoEntity> findByJobDescAndExecutorParam(@Param("jobDesc") String jobDesc, @Param("executorParam") String executorParam);

    /**
     * 查询所有未删除的任务
     */
    @Query("SELECT j FROM XxlJobInfoEntity j WHERE j.deleteFlag = 0 ORDER BY j.createTime DESC")
    List<XxlJobInfoEntity> findAllActive();

    /**
     * 根据服务名查询任务
     */
    @Query("SELECT j FROM XxlJobInfoEntity j WHERE j.executorParam LIKE :serviceName% AND j.deleteFlag = 0")
    List<XxlJobInfoEntity> findByServiceName(@Param("serviceName") String serviceName);

    /**
     * 根据任务状态查询任务
     */
    @Query("SELECT j FROM XxlJobInfoEntity j WHERE j.triggerStatus = :triggerStatus AND j.deleteFlag = 0")
    List<XxlJobInfoEntity> findByTriggerStatus(@Param("triggerStatus") Integer triggerStatus);

    /**
     * 统计任务数量
     */
    @Query("SELECT COUNT(j) FROM XxlJobInfoEntity j WHERE j.deleteFlag = 0")
    Long countActive();
}

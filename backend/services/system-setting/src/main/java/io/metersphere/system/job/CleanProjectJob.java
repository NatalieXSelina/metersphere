package io.metersphere.system.job;


import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.metersphere.project.domain.Project;
import io.metersphere.project.domain.ProjectExample;
import io.metersphere.project.mapper.ProjectMapper;
import io.metersphere.sdk.util.LogUtils;
import io.metersphere.system.service.SystemProjectService;
import jakarta.annotation.Resource;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class CleanProjectJob {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private SystemProjectService systemProjectService;

    /**
     * 清理状态为删除的项目  每天凌晨三点执行
     */
    @QuartzScheduled(cron = "0 0 3 * * ?")
    public void cleanupProject() {
        LogUtils.info("clean up project start.");
        try {
            LocalDate date = LocalDate.now().minusMonths(1);
            long timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            this.doCleanupProject(timestamp);
        } catch (Exception e) {
            LogUtils.error("clean up project error.", e);
        }
        LogUtils.info("clean up project end.");
    }

    private void doCleanupProject(long timestamp) {
        ProjectExample example = new ProjectExample();
        example.createCriteria().andDeletedEqualTo(true).andDeleteTimeLessThanOrEqualTo(timestamp);
        List<Project> projects = projectMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(projects)) {
            systemProjectService.deleteProject(projects);
        }
    }
}

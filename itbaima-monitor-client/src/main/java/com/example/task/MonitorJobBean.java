package com.example.task;

import com.example.entity.RuntimeDetail;
import com.example.utils.MonitorUtils;
import com.example.utils.NetUtils;
import jakarta.annotation.Resource;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

// 禁止并发执行：采集任务每 10 秒触发一次，若上一轮因依赖不可达而阻塞，
// 没有该注解会不断新建线程，最终占满 Quartz 线程池（见测试记录 F6）。
@DisallowConcurrentExecution
public class MonitorJobBean extends QuartzJobBean {

    @Resource
    MonitorUtils  monitorUtils;

    @Resource
    NetUtils netUtils;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        RuntimeDetail runtimeDetail = monitorUtils.monitorRuntimeDetail();
        netUtils.updateRuntimeDetail(runtimeDetail);
    }
}


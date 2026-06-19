package com.marketlens.backend.service;

import com.marketlens.backend.entity.JobRunLog;
import com.marketlens.backend.mapper.JobRunLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class JobRunLogService {
    private final JobRunLogMapper jobRunLogMapper;

    public JobRunLogService(JobRunLogMapper jobRunLogMapper) {
        this.jobRunLogMapper = jobRunLogMapper;
    }

    public void record(
            String jobName,
            String market,
            LocalDate tradeDate,
            String status,
            String provider,
            int records,
            String message,
            LocalDateTime startedAt
    ) {
        JobRunLog log = new JobRunLog();
        log.setJobName(jobName);
        log.setMarket(market);
        log.setTradeDate(tradeDate);
        log.setStatus(status);
        log.setProvider(provider);
        log.setRecordsCount(records);
        log.setMessage(message);
        log.setStartedAt(startedAt);
        log.setFinishedAt(LocalDateTime.now());
        jobRunLogMapper.insert(log);
    }
}

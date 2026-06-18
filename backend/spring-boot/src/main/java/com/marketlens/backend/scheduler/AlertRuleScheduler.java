package com.marketlens.backend.scheduler;

import com.marketlens.backend.service.AlertEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlertRuleScheduler {
    private static final Logger log = LoggerFactory.getLogger(AlertRuleScheduler.class);

    private final AlertEvaluationService alertEvaluationService;
    private final boolean enabled;

    public AlertRuleScheduler(
            AlertEvaluationService alertEvaluationService,
            @Value("${market-lens.alerts.scheduler-enabled:true}") boolean enabled
    ) {
        this.alertEvaluationService = alertEvaluationService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${market-lens.alerts.fixed-delay-ms:60000}")
    public void evaluateAlertRules() {
        if (!enabled) {
            return;
        }

        int triggered = alertEvaluationService.evaluateEnabledRules();
        if (triggered > 0) {
            log.info("Triggered {} alert rule(s)", triggered);
        }
    }
}

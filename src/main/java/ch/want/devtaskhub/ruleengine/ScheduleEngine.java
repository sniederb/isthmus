package ch.want.devtaskhub.ruleengine;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.scheduling.support.CronTrigger;

import ch.want.devtaskhub.common.RestClient;
import ch.want.devtaskhub.state.ScheduledRule;

public class ScheduleEngine extends AbstractRuleEngine implements Observer {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduleEngine.class);
    @Autowired
    private TaskScheduler executor;
    private final Collection<ScheduledFuture<?>> scheduledFutures = new HashSet<>();

    @PostConstruct
    public void init() {
        scheduleRules();
    }

    @Override
    public void update(final Observable userPropertiesManager, final Object source) {
        applicationState.addAction("Re-scheduling due to (re)loaded UserProperties");
        cancelCurrentRunners();
        scheduleRules();
    }

    public void forceRun(final int ruleIndex) {
        if (userProperties.getScheduledRules().size() > ruleIndex) {
            new ScheduledRuleRunner(userProperties.getScheduledRules().get(ruleIndex)).run();
        }
    }

    private void cancelCurrentRunners() {
        scheduledFutures.stream()
                .forEach(f -> f.cancel(false));
        scheduledFutures.clear();
    }

    private void scheduleRules() {
        scheduledFutures.addAll(//
                userProperties.getScheduledRules().stream()//
                        .filter(ScheduledRule::isEnabled)
                        .map(ScheduledRuleRunner::new)
                        .map(r -> {
                            try {
                                applicationState.addAction("Registering new scheduled task for " + r.rule.getScheduledQueryEndpoint());
                                r.logNextExecutionTime();
                                return executor.schedule(r, r.getScheduleTrigger());
                            } catch (final Exception e) {
                                LOG.error(e.getMessage(), e);
                                applicationState.addAction("FAILED to schedule rule: " + e.getMessage());
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    private class ScheduledRuleRunner implements Runnable {

        private final ScheduledRule rule;

        ScheduledRuleRunner(final ScheduledRule rule) {
            this.rule = rule;
        }

        Trigger getScheduleTrigger() {
            return new CronTrigger(rule.getCronExpression());
        }

        void logNextExecutionTime() {
            final CronSequenceGenerator cronTrigger = new CronSequenceGenerator(rule.getCronExpression());
            final Date next = cronTrigger.next(new Date());
            LOG.info("Next execution expected at {}", next);
            applicationState.addAction("Next execution expected at " + next);
        }

        @Override
        public void run() {
            applicationState.addAction("Running scheduled event against " + rule.getScheduledQueryEndpoint());
            try {
                final String queryResult = query();
                extractPayloadAndSend(rule, queryResult);
            } catch (final Exception e) {
                applicationState.addAction("Scheduled event failed with " + e.getMessage());
                LOG.error(e.getMessage(), e);
            }
        }

        private String query() throws IOException {
            final RestClient queryClient = restClientFactory.getClient(rule.getScheduledQueryEndpoint());
            // always treat scheduled query as GET
            return queryClient.get(String.class).orElseThrow(IllegalStateException::new);
        }
    }
}
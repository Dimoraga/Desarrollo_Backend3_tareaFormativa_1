package cl.duoc.backendiii.semana1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * Listener didáctico para observar el cierre de un Job.
 *
 * Es genérico y lo reutilizan los tres Jobs del proyecto (transacciones,
 * intereses, cuentas anuales): solo informa en consola el nombre del Job y
 * el estado final, sin asumir ningún archivo de salida en particular.
 */
public class JobCompletionNotificationListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("JOB {} FINALIZADO OK", jobName);
        } else {
            log.warn("JOB {} terminó con estado {}", jobName, jobExecution.getStatus());
        }
    }
}

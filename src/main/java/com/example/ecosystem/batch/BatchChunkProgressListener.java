package com.example.ecosystem.batch;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "batch.progress.logging", havingValue = "true")
public class BatchChunkProgressListener implements ChunkListener {

    private static final int LOG_EVERY_N_CHUNKS = 10;

    @Value("${batch.chunk.size:2000}")
    private int chunkSize;

    @Override
    public void beforeChunk(ChunkContext context) {
        StepExecution step = stepExecution(context);
        if (step.getCommitCount() == 0) {
            String reportDate = step.getJobParameters().getString("reportDate", "unknown");
            BatchConsole.line("");
            BatchConsole.tag("BATCH", "Starting dailySalesStep for reportDate="
                    + BatchConsole.highlight(reportDate)
                    + " (chunk size=" + BatchConsole.highlight(String.valueOf(chunkSize)) + ")");
        }
    }

    @Override
    public void afterChunk(ChunkContext context) {
        StepExecution step = stepExecution(context);
        long commits = step.getCommitCount();
        if (commits % LOG_EVERY_N_CHUNKS != 0) {
            return;
        }
        BatchConsole.tagf(
                "BATCH",
                "chunk %s committed | read %s | written %s | skipped %s",
                BatchConsole.highlight(String.format("%,d", commits)),
                BatchConsole.highlight(String.format("%,d", step.getReadCount())),
                BatchConsole.highlight(String.format("%,d", step.getWriteCount())),
                BatchConsole.highlight(String.format("%,d", step.getSkipCount()))
        );
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        StepExecution step = stepExecution(context);
        BatchConsole.errorf(
                "BATCH",
                "chunk error after %,d commits | read %,d | skipped %,d",
                step.getCommitCount(),
                step.getReadCount(),
                step.getSkipCount()
        );
    }

    private static StepExecution stepExecution(ChunkContext context) {
        StepContext stepContext = context.getStepContext();
        return stepContext.getStepExecution();
    }
}

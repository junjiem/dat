package ai.dat.core.agent;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.contentstore.ContentStore;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Human-in-the-loop Askdata Agent
 *
 * @Author JunjieM
 * @Date 2025/8/12
 */
public abstract class AbstractHitlAskdataAgent extends AbstractAskdataAgent {

    private CompletableFuture<String> future = new CompletableFuture<>();

    public AbstractHitlAskdataAgent(@NonNull ContentStore contentStore,
                                    @NonNull DatabaseAdapter databaseAdapter) {
        super(contentStore, databaseAdapter);
    }

    /**
     * user response
     *
     * @param response
     */
    @Override
    public void userResponse(String response) {
        future.complete(response);
    }

    /**
     * 等待 user response
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public String waitForUserResponse()
            throws InterruptedException, ExecutionException {
        try {
            return future.get();
        } finally {
            future = new CompletableFuture<>();
        }
    }

    /**
     * 等待 user response，支持超时
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return response，如果超时则抛出 TimeoutException
     */
    public String waitForUserResponse(long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException, ExecutionException {
        try {
            return future.get(timeout, unit);
        } finally {
            future = new CompletableFuture<>();
        }
    }
}

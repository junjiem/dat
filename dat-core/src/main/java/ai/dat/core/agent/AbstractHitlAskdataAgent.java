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

    private CompletableFuture<String> userResponseFuture = new CompletableFuture<>();
    private CompletableFuture<Boolean> userApprovalFuture = new CompletableFuture<>();

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
        userResponseFuture.complete(response);
    }

    /**
     * user approval
     *
     * @param approval
     */
    @Override
    public void userApproval(Boolean approval) {
        userApprovalFuture.complete(approval);
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
            return userResponseFuture.get();
        } finally {
            userResponseFuture = new CompletableFuture<>();
        }
    }

    /**
     * 等待 user approval
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public Boolean waitForUserApproval()
            throws InterruptedException, ExecutionException {
        try {
            return userApprovalFuture.get();
        } finally {
            userApprovalFuture = new CompletableFuture<>();
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
            return userResponseFuture.get(timeout, unit);
        } finally {
            userResponseFuture = new CompletableFuture<>();
        }
    }

    /**
     * 等待 user approval，支持超时
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return response，如果超时则抛出 TimeoutException
     */
    public Boolean waitForUserApproval(long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException, ExecutionException {
        try {
            return userApprovalFuture.get(timeout, unit);
        } finally {
            userApprovalFuture = new CompletableFuture<>();
        }
    }
}
